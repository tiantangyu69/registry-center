package cc.lee.registry.client.zookeeper.zkclient;

import java.util.List;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.zookeeper.Watcher.Event.KeeperState;

import cc.lee.registry.client.zookeeper.listener.ChildrenListener;
import cc.lee.registry.client.zookeeper.support.AbstractZooKeeperClient;
import cc.lee.registry.common.URL;

import com.alibaba.dubbo.remoting.zookeeper.StateListener;

/**
 * ZkClient
 * @author lizhitao
 */
public class ZkclientZookeeperClient extends AbstractZooKeeperClient<IZkChildListener> {
	private final ZkClient client;
	private volatile KeeperState state = KeeperState.SyncConnected;
	
	/**
	 * 初始ZkClient
	 * @param address 连接地址
	 */
	public ZkclientZookeeperClient(URL url) {
		super(url);
		client = new ZkClient(url.getBackupAddress());
		// 添加授权信息
		String authority = url.getAuthority();
		if (null != authority && authority.length() > 0) {
			client.addAuthInfo("digest", authority.getBytes());
		}
		client.subscribeStateChanges(new IZkStateListener() {// 监听ZooKeeper状态变化
			@Override
			public void handleStateChanged(KeeperState state) throws Exception {// 处理状态变化
				ZkclientZookeeperClient.this.state = state;
				if (state == KeeperState.Disconnected) {// 断开连接
					stateChanged(StateListener.DISCONNECTED);
				} else if (state == KeeperState.SyncConnected) {// 已连接
					stateChanged(StateListener.CONNECTED);
				}
			}
			@Override
			public void handleSessionEstablishmentError(Throwable error) throws Exception {
				log.error(error.getMessage());
			}
			@Override
			public void handleNewSession() throws Exception {
				stateChanged(StateListener.RECONNECTED);// 重新连接
			}
		});
	}

	@Override
	public void delete(String path) {
		try {
			client.delete(path);
		} catch (ZkNoNodeException e) {
		}
	}

	@Override
	public List<String> getChildren(String path) {
		try {
			return client.getChildren(path);
        } catch (ZkNoNodeException e) {
            return null;
        }
	}

	@Override
	public boolean isConnected() {
		return state == KeeperState.SyncConnected;
	}

	@Override
	public void createPersistent(String path) {
		try {
			client.createPersistent(path, true);
		} catch (ZkNodeExistsException e) {
		}
	}

	@Override
	public void createEphemeral(String path) {
		try {
			client.createEphemeral(path);
		} catch (ZkNodeExistsException e) {
		}
	}

	@Override
	public void doClose() {
		client.close();
	}

	@Override
	public IZkChildListener createTargetChildrenListener(String path, final ChildrenListener listener) {
		return new IZkChildListener() {
			public void handleChildChange(String parentPath, List<String> currentChilds)
					throws Exception {
				listener.changed(parentPath, currentChilds);
			}
		};
	}

	@Override
	public List<String> addTargetChildrenListener(String path, IZkChildListener listener) {
		return client.subscribeChildChanges(path, listener);
	}

	@Override
	public void removeTargetChildrenListener(String path, IZkChildListener listener) {
		client.unsubscribeChildChanges(path,  listener);
	}

}
