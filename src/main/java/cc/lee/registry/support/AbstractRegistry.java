package cc.lee.registry.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.lee.registry.NotifyListener;
import cc.lee.registry.Registry;
import cc.lee.registry.util.ConcurrentHashSet;
import cc.lee.registry.util.NamedThreadFactory;

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
		syncSaveFile = true;
		// 缓存文件保存路径
		String fileName = System.getProperty("user.home") + "/.registry-center/registry.cache";
		File file = null;
		
		// 缓存文件不存在，创建缓存文件
		if (null != fileName && !fileName.isEmpty()) {
			file = new File(fileName);
			if (!file.exists() && file.getParentFile() != null && !file.getParentFile().exists()) {
				if (!file.getParentFile().mkdirs()) {
					throw new IllegalArgumentException("Invalid registry store file " + file + ",cause: Failed to create directory " + file.getParentFile() + "!");
				}
			}
		}
		this.file = file;
		loadProperties();// 加载缓存文件
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
    
	public boolean isAvailable() {
		return false;
	}

	public void destroy() {

	}

	public void register(URL url) {

	}

	public void unregister(URL url) {

	}

	public void subscribe(URL url, NotifyListener listener) {

	}

	public void unsubscribe(URL url, NotifyListener listener) {

	}

	public List<URL> lookup(URL url) {
		return null;
	}

}
