package cc.lee.registry.zookeeper.support;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.lee.registry.URL;
import cc.lee.registry.zookeeper.ZooKeeperClient;
import cc.lee.registry.zookeeper.listener.ChildrenListener;
import cc.lee.registry.zookeeper.listener.StateListener;

/**
 * 抽象ZooKeeper客户端
 * @author lizhitao
 */
public abstract class AbstractZooKeeperClient<TargetChildrenListener> implements ZooKeeperClient {
	protected static final Logger log = LoggerFactory.getLogger(AbstractZooKeeperClient.class);
	/**
	 * 状态监听器集合
	 */
	private final Set<StateListener> stateListeners = new CopyOnWriteArraySet<StateListener>();
	/**
	 * 子节点监听器集合
	 */
	private final ConcurrentMap<String, ConcurrentMap<ChildrenListener, TargetChildrenListener>> childrenListeners = new ConcurrentHashMap<String, ConcurrentMap<ChildrenListener, TargetChildrenListener>>();
	/**
	 * ZooKeeper关闭状态
	 */
	private volatile boolean closed = false;
	
	private final URL connectInfo;
	
	public AbstractZooKeeperClient(URL connectInfo) {
		this.connectInfo = connectInfo;
	}

	/**
	 * 获取ZooKeeper连接地址
	 */
	@Override
	public URL getConnectInfo() {
		return this.connectInfo;
	}

	/**
	 * 创建节点
	 */
	@Override
	public void create(String path, boolean ephemeral) {
		// 递归创建子路径
		int i = path.lastIndexOf('/');
		if (i > 0) {
			create(path.substring(0, i), false);
		}
		
		if (ephemeral) {// 创建临时节点
			createEphemeral(path);
		} else {// 创建持久节点
			createPersistent(path);
		}
	}
	
	public Set<StateListener> getStateListeners(){
		return this.stateListeners;
	}

	@Override
	public List<String> addChildrenListener(String path, ChildrenListener listener) {
		ConcurrentMap<ChildrenListener, TargetChildrenListener> listeners = childrenListeners.get(path);
		if (listeners == null) {
			childrenListeners.putIfAbsent(path, new ConcurrentHashMap<ChildrenListener, TargetChildrenListener>());
			listeners = childrenListeners.get(path);
		}
		TargetChildrenListener targetListener = listeners.get(listener);
		if (targetListener == null) {
			listeners.putIfAbsent(listener, createTargetChildrenListener(path, listener));
			targetListener = listeners.get(listener);
		}
		return addTargetChildrenListener(path, targetListener);
	}

	@Override
	public void removeChildrenListener(String path, ChildrenListener listener) {
		ConcurrentMap<ChildrenListener, TargetChildrenListener> listeners = childrenListeners.get(path);
		if (listeners != null) {
			TargetChildrenListener targetListener = listeners.remove(listener);
			if (targetListener != null) {
				removeTargetChildrenListener(path, targetListener);
			}
		}
	}

	@Override
	public void addStateListener(StateListener listener) {
		stateListeners.add(listener);
	}

	@Override
	public void removeStateListener(StateListener listener) {
		stateListeners.remove(listener);
	}

	/**
	 * 状态变化监听
	 * @param state
	 */
	protected void stateChanged(int state) {
		for (StateListener stateListener : getStateListeners()) {
			stateListener.changed(state);
		}
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		try {
			doClose();
		} catch (Throwable t) {
			log.warn(t.getMessage(), t);
		}
	}

	/**
	 * 创建持久节点
	 * @param path 节点路径
	 */
	protected abstract void createPersistent(String path);

	/**
	 * 创建临时节点
	 * @param path 节点路径
	 */
	protected abstract void createEphemeral(String path);

	/**
	 * 关闭连接
	 */
	protected abstract void doClose();
	
	protected abstract TargetChildrenListener createTargetChildrenListener(String path, ChildrenListener listener);

	protected abstract List<String> addTargetChildrenListener(String path, TargetChildrenListener listener);

	protected abstract void removeTargetChildrenListener(String path, TargetChildrenListener listener);
}
