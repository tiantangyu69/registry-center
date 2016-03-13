package cc.lee.registry.zookeeper.curator;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.CuratorFrameworkFactory.Builder;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.WatchedEvent;

import cc.lee.registry.common.URL;
import cc.lee.registry.zookeeper.listener.ChildrenListener;
import cc.lee.registry.zookeeper.listener.StateListener;
import cc.lee.registry.zookeeper.support.AbstractZooKeeperClient;
/**
 * curator客户端
 * @author lizhitao
 */
public class CuratorZooKeeperClient extends AbstractZooKeeperClient<CuratorWatcher> {
	private final CuratorFramework client;
	
	public CuratorZooKeeperClient(URL url) {
		super(url);
		Builder builder = CuratorFrameworkFactory.builder()
				.connectString(url.getBackupAddress())
				.retryPolicy(new RetryNTimes(Integer.MAX_VALUE, 1000))
				.connectionTimeoutMs(5000);
		// 添加授权信息
		String authority = url.getAuthority();
		if (null != authority && authority.length() > 0) {
			builder.authorization("digest", authority.getBytes());
		}
		client = builder.build();
		client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
				public void stateChanged(CuratorFramework client, ConnectionState state) {
					if (state == ConnectionState.LOST) {// 断开连接
						CuratorZooKeeperClient.this.stateChanged(StateListener.DISCONNECTED);
					} else if (state == ConnectionState.CONNECTED) {// 已连接
						CuratorZooKeeperClient.this.stateChanged(StateListener.CONNECTED);
					} else if (state == ConnectionState.RECONNECTED) {// 重新连接
						CuratorZooKeeperClient.this.stateChanged(StateListener.RECONNECTED);
					}
				}
		});
		client.start();
	}

	@Override
	public void delete(String path) {
		try {
			client.delete().forPath(path);
		} catch (NoNodeException e) {
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	@Override
	public List<String> getChildren(String path) {
		try {
			return client.getChildren().forPath(path);
		} catch (NoNodeException e) {
			return null;
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	@Override
	public boolean isConnected() {
		return client.getZookeeperClient().isConnected();
	}

	@Override
	protected void createPersistent(String path) {
		try {
			client.create().forPath(path);
		} catch (NodeExistsException e) {
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	@Override
	protected void createEphemeral(String path) {
		try {
			client.create().withMode(CreateMode.EPHEMERAL).forPath(path);
		} catch (NodeExistsException e) {
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	@Override
	protected void doClose() {
		client.close();
	}

	@Override
	protected CuratorWatcher createTargetChildrenListener(String path, ChildrenListener listener) {
		return new CuratorWatcherImpl(listener);
	}

	@Override
	protected List<String> addTargetChildrenListener(String path, CuratorWatcher listener) {
		try {
			return client.getChildren().usingWatcher(listener).forPath(path);
		} catch (NoNodeException e) {
			return null;
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	@Override
	protected void removeTargetChildrenListener(String path, CuratorWatcher listener) {
		((CuratorWatcherImpl) listener).unwatch();
	}
	
	private class CuratorWatcherImpl implements CuratorWatcher {

		private volatile ChildrenListener listener;

		public CuratorWatcherImpl(ChildrenListener listener) {
			this.listener = listener;
		}

		public void unwatch() {
			this.listener = null;
		}

		public void process(WatchedEvent event) throws Exception {
			if (listener != null) {
				listener.changed(event.getPath(), client.getChildren().usingWatcher(this).forPath(event.getPath()));
			}
		}
	}
}
