package cc.lee.registry.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.lee.registry.NotifyListener;
import cc.lee.registry.Registry;
import cc.lee.registry.common.Constants;
import cc.lee.registry.common.URL;
import cc.lee.registry.util.ConcurrentHashSet;
import cc.lee.registry.util.ConfigUtils;
import cc.lee.registry.util.NamedThreadFactory;
import cc.lee.registry.util.UrlUtils;
/**
 * @author lizhitao
 */
public abstract class AbstractRegistry implements Registry {
	// SLF4j日志
	protected final Logger log = LoggerFactory.getLogger(getClass());

	// URL 地址分隔符，在文件缓存中分割 URL
	private static final char URL_SEPARATOR = ' ';

	// URL 地址分割正则表达式，用于解析文件缓存中 URL 列表
	private static final String URL_SPLIT_PATTERN = "\\s+";

	private URL registryURL;

	// 本地磁盘缓存文件
	private File file;

	// 本地磁盘缓存
	private final Properties properties = new Properties();

	// 文件缓存定时写入线程
	private final ExecutorService registryCacheExecutor = Executors.newFixedThreadPool(1, new NamedThreadFactory("RegistryCenterCache", true));

	// 是否同步保存文件
	private final boolean syncSaveFile;

	private AtomicLong lastCacheChanged = new AtomicLong();

	private final Set<URL> registered = new ConcurrentHashSet<URL>();

	private final ConcurrentMap<URL, Set<NotifyListener>> subscribed = new ConcurrentHashMap<URL, Set<NotifyListener>>();

	private final ConcurrentMap<URL, Map<String, List<URL>>> notified = new ConcurrentHashMap<URL, Map<String, List<URL>>>();

	public AbstractRegistry(URL url) {
		setUrl(url);
		// 是否同步保存文件，默认为false
		syncSaveFile = url.getParameter(Constants.REGISTRY_FILESAVE_SYNC_KEY, false);;
		// 缓存文件保存路径
		String fileName =url.getParameter(Constants.FILE_KEY, System.getProperty("user.home") + "/.registry-center/registry-" + url.getHost() + ".cache");
		File file = null;
		
		// 缓存文件不存在，创建缓存文件
		if (ConfigUtils.isNotEmpty(fileName)) {
			file = new File(fileName);
			if (!file.exists() && file.getParentFile() != null && !file.getParentFile().exists()) {
				if (!file.getParentFile().mkdirs()) {
					throw new IllegalArgumentException("Invalid registry store file " + file + ",cause: Failed to create directory " + file.getParentFile() + "!");
				}
			}
		}
		this.file = file;
		loadProperties();// 加载缓存文件
        notify(url.getBackupUrls());
	}

	protected void setUrl(URL url) {
		if (url == null) {
			throw new IllegalArgumentException("registry url == null");
		}
		this.registryURL = url;
	}

	/**
	 * 加载缓存文件
	 */
	private void loadProperties() {
		if (null != file && file.exists()) {
			InputStream in = null;
			try {
				in = new FileInputStream(file);
				properties.load(in);
				if (log.isInfoEnabled()) {
					log.info("Load registry store file " + file + ", data: " + properties);
				}
			} catch (Exception e) {
				log.warn("Failed to load registry store file " + file, e);
			} finally {
				if (null != in) {
					try {
						in.close();
					} catch (IOException e) {
						log.warn(e.getMessage(), e);
					}
				}
			}
		}
	}

	public URL getURL() {
		return this.registryURL;
	}
	
	public Set<URL> getRegistered() {
        return registered;
    }
	
	public Map<URL, Set<NotifyListener>> getSubscribed() {
        return subscribed;
    }
	
	public Map<URL, Map<String, List<URL>>> getNotified() {
        return notified;
    }

    public File getCacheFile() {
        return file;
    }

    public Properties getCacheProperties() {
        return properties;
    }

    public AtomicLong getLastCacheChanged(){
        return lastCacheChanged;
    }

    private class SaveProperties implements Runnable{
        private long version;
        private SaveProperties(long version){
            this.version = version;
        }
        public void run() {
            doSaveProperties(version);
        }
    }
    
    public void doSaveProperties(long version) {
        if(version < lastCacheChanged.get()){
            return;
        }
        if (file == null) {
            return;
        }
        Properties newProperties = new Properties();
        // 保存之前先读取一遍，防止多个注册中心之间冲突
        InputStream in = null;
        try {
            if (file.exists()) {
                in = new FileInputStream(file);
                newProperties.load(in);
            }
        } catch (Throwable e) {
            log.warn("Failed to load registry store file, cause: " + e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.warn(e.getMessage(), e);
                }
            }
        }     
     // 保存
        try {
			newProperties.putAll(properties);
            File lockfile = new File(file.getAbsolutePath() + ".lock");
            if (!lockfile.exists()) {
            	lockfile.createNewFile();
            }
            RandomAccessFile raf = new RandomAccessFile(lockfile, "rw");
            try {
                FileChannel channel = raf.getChannel();
                try {
                    FileLock lock = channel.tryLock();
                	if (lock == null) {
                        throw new IOException("Can not lock the registry cache file " + file.getAbsolutePath() + ", ignore and retry later, maybe multi java process use the file");
                    }
                	// 保存
                    try {
                    	if (! file.exists()) {
                            file.createNewFile();
                        }
                        FileOutputStream outputFile = new FileOutputStream(file);  
                        try {
                            newProperties.store(outputFile, "Registry Center Cache");
                        } finally {
                        	outputFile.close();
                        }
                    } finally {
                    	lock.release();
                    }
                } finally {
                    channel.close();
                }
            } finally {
                raf.close();
            }
        } catch (Throwable e) {
            if (version < lastCacheChanged.get()) {
                return;
            } else {
                registryCacheExecutor.execute(new SaveProperties(lastCacheChanged.incrementAndGet()));
            }
            log.warn("Failed to save registry store file, cause: " + e.getMessage(), e);
        }
    }
    
	public List<URL> getCacheURLs(URL url) {
		for (Map.Entry<Object, Object> entry : properties.entrySet()) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			if (key != null && key.length() > 0 && key.equals(url.getServiceKey())
                    && (Character.isLetter(key.charAt(0)) || key.charAt(0) == '_')
                    && value != null && value.length() > 0) {
                String[] arr = value.trim().split(URL_SPLIT_PATTERN);
                List<URL> urls = new ArrayList<URL>();
                for (String u : arr) {
                    urls.add(URL.valueOf(u));
                }
                return urls;
            }
		}
        return null;
	}
	
	public List<URL> lookup(URL url){
		List<URL> result = new ArrayList<URL>();
		Map<String, List<URL>> notifiedUrls = getNotified().get(url);

        if (notifiedUrls != null && notifiedUrls.size() > 0) {
            for (List<URL> urls : notifiedUrls.values()) {
                for (URL u : urls) {
                    if (! Constants.EMPTY_PROTOCOL.equals(u.getProtocol())) {
                        result.add(u);
                    }
                }
            }
        } else {
            final AtomicReference<List<URL>> reference = new AtomicReference<List<URL>>();
            NotifyListener listener = new NotifyListener() {
                public void notify(List<URL> urls) {
                    reference.set(urls);
                }
            };
            subscribe(url, listener); // 订阅逻辑保证第一次notify后再返回
            List<URL> urls = reference.get();
            if (urls != null && urls.size() > 0) {
                for (URL u : urls) {
                    if (! Constants.EMPTY_PROTOCOL.equals(u.getProtocol())) {
                        result.add(u);
                    }
                }
            }
        }
        return result;
	}
	
	public void register(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("register url == null");
        }
        if (log.isInfoEnabled()){
            log.info("Register: " + url);
        }
        registered.add(url);
	}

	public void unregister(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("unregister url == null");
        }
        if (log.isInfoEnabled()){
        	log.info("Unregister: " + url);
        }
        registered.remove(url);
	}

	public void subscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("subscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("subscribe listener == null");
        }
        if (log.isInfoEnabled()){
        	log.info("Subscribe: " + url);
        }
        Set<NotifyListener> listeners = subscribed.get(url);
        if (listeners == null) {
            subscribed.putIfAbsent(url, new ConcurrentHashSet<NotifyListener>());
            listeners = subscribed.get(url);
        }
        listeners.add(listener);
	}

	public void unsubscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("unsubscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("unsubscribe listener == null");
        }
        if (log.isInfoEnabled()){
        	log.info("Unsubscribe: " + url);
        }
        Set<NotifyListener> listeners = subscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
	}
	
	protected void recover() throws Exception {
        // register
        Set<URL> recoverRegistered = new HashSet<URL>(getRegistered());
        if (! recoverRegistered.isEmpty()) {
            if (log.isInfoEnabled()) {
            	log.info("Recover register url " + recoverRegistered);
            }
            for (URL url : recoverRegistered) {
                register(url);
            }
        }
        // subscribe
        Map<URL, Set<NotifyListener>> recoverSubscribed = new HashMap<URL, Set<NotifyListener>>(getSubscribed());
        if (! recoverSubscribed.isEmpty()) {
            if (log.isInfoEnabled()) {
            	log.info("Recover subscribe url " + recoverSubscribed.keySet());
            }
            for (Map.Entry<URL, Set<NotifyListener>> entry : recoverSubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    subscribe(url, listener);
                }
            }
        }
    }
	
    protected static List<URL> filterEmpty(URL url, List<URL> urls) {
        if (urls == null || urls.size() == 0) {
            List<URL> result = new ArrayList<URL>(1);
            result.add(url.setProtocol(Constants.EMPTY_PROTOCOL));
            return result;
        }
        return urls;
    }


    protected void notify(List<URL> urls) {
        if(urls == null || urls.isEmpty()) return;
        
        for (Map.Entry<URL, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
            URL url = entry.getKey();
            
            if(! UrlUtils.isMatch(url, urls.get(0))) {
                continue;
            }
            
            Set<NotifyListener> listeners = entry.getValue();
            if (listeners != null) {
                for (NotifyListener listener : listeners) {
                    try {
                        notify(url, listener, filterEmpty(url, urls));
                    } catch (Throwable t) {
                        log.error("Failed to notify registry event, urls: " +  urls + ", cause: " + t.getMessage(), t);
                    }
                }
            }
        }
    }

    protected void notify(URL url, NotifyListener listener, List<URL> urls) {
        if (url == null) {
            throw new IllegalArgumentException("notify url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("notify listener == null");
        }
        if ((urls == null || urls.size() == 0) 
                && ! Constants.ANY_VALUE.equals(url.getServiceInterface())) {
        	log.warn("Ignore empty notify urls for subscribe url " + url);
            return;
        }
        if (log.isInfoEnabled()) {
        	log.info("Notify urls for subscribe url " + url + ", urls: " + urls);
        }
        Map<String, List<URL>> result = new HashMap<String, List<URL>>();
        for (URL u : urls) {
            if (UrlUtils.isMatch(url, u)) {
            	String category = u.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
            	List<URL> categoryList = result.get(category);
            	if (categoryList == null) {
            		categoryList = new ArrayList<URL>();
            		result.put(category, categoryList);
            	}
            	categoryList.add(u);
            }
        }
        if (result.size() == 0) {
            return;
        }
        Map<String, List<URL>> categoryNotified = notified.get(url);
        if (categoryNotified == null) {
            notified.putIfAbsent(url, new ConcurrentHashMap<String, List<URL>>());
            categoryNotified = notified.get(url);
        }
        for (Map.Entry<String, List<URL>> entry : result.entrySet()) {
            String category = entry.getKey();
            List<URL> categoryList = entry.getValue();
            categoryNotified.put(category, categoryList);
            saveProperties(url);
            listener.notify(categoryList);
        }
    }

    private void saveProperties(URL url) {
        if (file == null) {
            return;
        }
        
        try {
            StringBuilder buf = new StringBuilder();
            Map<String, List<URL>> categoryNotified = notified.get(url);
            if (categoryNotified != null) {
                for (List<URL> us : categoryNotified.values()) {
                    for (URL u : us) {
                        if (buf.length() > 0) {
                            buf.append(URL_SEPARATOR);
                        }
                        buf.append(u.toFullString());
                    }
                }
            }
            properties.setProperty(url.getServiceKey(), buf.toString());
            long version = lastCacheChanged.incrementAndGet();
            if (syncSaveFile) {
                doSaveProperties(version);
            } else {
                registryCacheExecutor.execute(new SaveProperties(version));
            }
        } catch (Throwable t) {
            log.warn(t.getMessage(), t);
        }
    }

    public void destroy() {
        if (log.isInfoEnabled()){
        	log.info("Destroy registry:" + getURL());
        }
        Set<URL> destroyRegistered = new HashSet<URL>(getRegistered());
        if (! destroyRegistered.isEmpty()) {
            for (URL url : new HashSet<URL>(getRegistered())) {
                if (url.getParameter(Constants.DYNAMIC_KEY, true)) {
                    try {
                        unregister(url);
                        if (log.isInfoEnabled()) {
                        	log.info("Destroy unregister url " + url);
                        }
                    } catch (Throwable t) {
                    	log.warn("Failed to unregister url " + url + " to registry " + getURL() + " on destroy, cause: " + t.getMessage(), t);
                    }
                }
            }
        }
        Map<URL, Set<NotifyListener>> destroySubscribed = new HashMap<URL, Set<NotifyListener>>(getSubscribed());
        if (! destroySubscribed.isEmpty()) {
            for (Map.Entry<URL, Set<NotifyListener>> entry : destroySubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    try {
                        unsubscribe(url, listener);
                        if (log.isInfoEnabled()) {
                        	log.info("Destroy unsubscribe url " + url);
                        }
                    } catch (Throwable t) {
                    	log.warn("Failed to unsubscribe url " + url + " to registry " + getURL() + " on destroy, cause: " +t.getMessage(), t);
                    }
                }
            }
        }
    }

    public String toString() {
        return getURL().toString();
    }
}
