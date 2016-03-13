package cc.lee.registry.support.zookeeper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.lee.registry.NotifyListener;
import cc.lee.registry.common.Constants;
import cc.lee.registry.common.URL;
import cc.lee.registry.remoting.zookeeper.ZooKeeperClient;
import cc.lee.registry.remoting.zookeeper.listener.ChildrenListener;
import cc.lee.registry.remoting.zookeeper.listener.StateListener;
import cc.lee.registry.remoting.zookeeper.support.ZooKeeperTransporter;
import cc.lee.registry.support.FailbackRegistry;
import cc.lee.registry.util.ConcurrentHashSet;
import cc.lee.registry.util.UrlUtils;

public class ZooKeeperRegistry extends FailbackRegistry {
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final static int DEFAULT_ZOOKEEPER_PORT = 2181;
	private final static String DEFAULT_ROOT = "registry";
	private final String root;
	private final Set<String> anyServices = new ConcurrentHashSet<String>();
	private final ConcurrentMap<URL, ConcurrentMap<NotifyListener, ChildrenListener>> zkListeners = new ConcurrentHashMap<URL, ConcurrentMap<NotifyListener, ChildrenListener>>();
	private final ZooKeeperClient zkClient;

	public ZooKeeperRegistry(URL url, ZooKeeperTransporter zooKeeperTransporter) {
		super(url);
		if (url.isAnyHost()) {
			throw new IllegalStateException("registry address == null");
		}
		String group = url.getParameter(Constants.GROUP_KEY, DEFAULT_ROOT);
		if (!group.startsWith(Constants.PATH_SEPARATOR)) {
			group = Constants.PATH_SEPARATOR + group;
		}
		this.root = group;
		zkClient = zooKeeperTransporter.connect(url);
		zkClient.addStateListener(new StateListener() {
			public void changed(int state) {
				if (state == RECONNECTED) {
					try {
						recover();
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}
			}
		});
	}

	@Override
	public boolean isAvailable() {
        return zkClient.isConnected();
	}
	
	@Override
	public void destroy() {
        super.destroy();
        try {
            zkClient.close();
        } catch (Exception e) {
            log.warn("Failed to close zookeeper client " + getURL() + ", cause: " + e.getMessage(), e);
        }
	}

	@Override
	protected void doRegister(URL url) {
        try {
        	zkClient.create(toUrlPath(url), url.getParameter(Constants.DYNAMIC_KEY, true));
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to register " + url + " to zookeeper " + getURL() + ", cause: " + e.getMessage(), e);
        }
	}

	@Override
	protected void doUnregister(URL url) {
        try {
            zkClient.delete(toUrlPath(url));
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to unregister " + url + " to zookeeper " + getURL() + ", cause: " + e.getMessage(), e);
        }
	}

	@Override
	protected void doSubscribe(final URL url, final NotifyListener listener) {
        try {
            if (Constants.ANY_VALUE.equals(url.getServiceInterface())) {
                String root = toRootPath();
                ConcurrentMap<NotifyListener, ChildrenListener> listeners = zkListeners.get(url);
                if (listeners == null) {
                    zkListeners.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, ChildrenListener>());
                    listeners = zkListeners.get(url);
                }
                ChildrenListener zkListener = listeners.get(listener);
                if (zkListener == null) {
                    listeners.putIfAbsent(listener, new ChildrenListener() {
                        public void changed(String parentPath, List<String> currentChilds) {
                            for (String child : currentChilds) {
                                if (! anyServices.contains(child)) {
                                    anyServices.add(child);
                                    subscribe(url.setPath(child).addParameters(Constants.INTERFACE_KEY, child, 
                                            Constants.CHECK_KEY, String.valueOf(false)), listener);
                                }
                            }
                        }
                    });
                    zkListener = listeners.get(listener);
                }
                zkClient.create(root, false);
                List<String> services = zkClient.addChildrenListener(root, zkListener);
                if (services != null && services.size() > 0) {
                    anyServices.addAll(services);
                    for (String service : services) {
                        subscribe(url.setPath(service).addParameters(Constants.INTERFACE_KEY, service, 
                                Constants.CHECK_KEY, String.valueOf(false)), listener);
                    }
                }
            } else {
                List<URL> urls = new ArrayList<URL>();
                for (String path : toCategoriesPath(url)) {
                    ConcurrentMap<NotifyListener, ChildrenListener> listeners = zkListeners.get(url);
                    if (listeners == null) {
                        zkListeners.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, ChildrenListener>());
                        listeners = zkListeners.get(url);
                    }
                    ChildrenListener zkListener = listeners.get(listener);
                    if (zkListener == null) {
                        listeners.putIfAbsent(listener, new ChildrenListener() {
                            public void changed(String parentPath, List<String> currentChilds) {
                            	ZooKeeperRegistry.this.notify(url, listener, toUrlsWithEmpty(url, parentPath, currentChilds));
                            }
                        });
                        zkListener = listeners.get(listener);
                    }
                    zkClient.create(path, false);
                    List<String> children = zkClient.addChildrenListener(path, zkListener);
                    if (children != null) {
                    	urls.addAll(toUrlsWithEmpty(url, path, children));
                    }
                }
                notify(url, listener, urls);
            }
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to subscribe " + url + " to zookeeper " + getURL() + ", cause: " + e.getMessage(), e);
        }
	}

	@Override
	protected void doUnsubscribe(URL url, NotifyListener listener) {
        ConcurrentMap<NotifyListener, ChildrenListener> listeners = zkListeners.get(url);
        if (listeners != null) {
            ChildrenListener zkListener = listeners.get(listener);
            if (zkListener != null) {
                zkClient.removeChildrenListener(toUrlPath(url), zkListener);
            }
        }
    }

    public List<URL> lookup(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("lookup url == null");
        }
        try {
            List<String> providers = new ArrayList<String>();
            for (String path : toCategoriesPath(url)) {
                try {
                    List<String> children = zkClient.getChildren(path);
                    if (children != null) {
                        providers.addAll(children);
                    }
                } catch (ZkNoNodeException e) {
                    // ignore
                }
            }
            return toUrlsWithoutEmpty(url, providers);
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to lookup " + url + " from zookeeper " + getURL() + ", cause: " + e.getMessage(), e);
        }
	}

    
    private String toRootDir() {
        if (root.equals(Constants.PATH_SEPARATOR)) {
            return root;
        }
        return root + Constants.PATH_SEPARATOR;
    }
    
    private String toRootPath() {
        return root;
    }
    
    private String toServicePath(URL url) {
        String name = url.getServiceInterface();
        if (Constants.ANY_VALUE.equals(name)) {
            return toRootPath();
        }
        return toRootDir() + URL.encode(name);
    }

    private String[] toCategoriesPath(URL url) {
        String[] categroies;
        if (Constants.ANY_VALUE.equals(url.getParameter(Constants.CATEGORY_KEY))) {
            categroies = new String[] {Constants.PROVIDERS_CATEGORY, Constants.CONSUMERS_CATEGORY, 
                    Constants.ROUTERS_CATEGORY, Constants.CONFIGURATORS_CATEGORY};
        } else {
            categroies = url.getParameter(Constants.CATEGORY_KEY, new String[] {Constants.DEFAULT_CATEGORY});
        }
        String[] paths = new String[categroies.length];
        for (int i = 0; i < categroies.length; i ++) {
            paths[i] = toServicePath(url) + Constants.PATH_SEPARATOR + categroies[i];
        }
        return paths;
    }

    private String toCategoryPath(URL url) {
        return toServicePath(url) + Constants.PATH_SEPARATOR + url.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
    }

    private String toUrlPath(URL url) {
        return toCategoryPath(url) + Constants.PATH_SEPARATOR + URL.encode(url.toFullString());
    }
    
    private List<URL> toUrlsWithoutEmpty(URL consumer, List<String> providers) {
    	List<URL> urls = new ArrayList<URL>();
        if (providers != null && providers.size() > 0) {
            for (String provider : providers) {
                provider = URL.decode(provider);
                if (provider.contains("://")) {
                    URL url = URL.valueOf(provider);
                    if (UrlUtils.isMatch(consumer, url)) {
                        urls.add(url);
                    }
                }
            }
        }
        return urls;
    }

    private List<URL> toUrlsWithEmpty(URL consumer, String path, List<String> providers) {
        List<URL> urls = toUrlsWithoutEmpty(consumer, providers);
        if (urls == null || urls.isEmpty()) {
        	int i = path.lastIndexOf('/');
        	String category = i < 0 ? path : path.substring(i + 1);
        	URL empty = consumer.setProtocol(Constants.EMPTY_PROTOCOL).addParameter(Constants.CATEGORY_KEY, category);
            urls.add(empty);
        }
        return urls;
    }

    static String appendDefaultPort(String address) {
        if (address != null && address.length() > 0) {
            int i = address.indexOf(':');
            if (i < 0) {
                return address + ":" + DEFAULT_ZOOKEEPER_PORT;
            } else if (Integer.parseInt(address.substring(i + 1)) == 0) {
                return address.substring(0, i + 1) + DEFAULT_ZOOKEEPER_PORT;
            }
        }
        return address;
    }
}
