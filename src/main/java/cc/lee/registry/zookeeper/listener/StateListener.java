package cc.lee.registry.zookeeper.listener;
/**
 * ZooKeeper状态监听接口
 * @author lizhitao
 */
public interface StateListener {
	/**
	 * 已断开连接状态
	 */
	int DISCONNECTED = 0;
	/**
	 * 已连接状态
	 */
	int CONNECTED = 1;
	/**
	 * 重新连接状态
	 */
	int RECONNECTED = 2;

	void changed(int connected);
}
