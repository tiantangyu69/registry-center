package cc.lee.registry.zookeeper;

import java.util.List;

import cc.lee.registry.zookeeper.listener.ChildrenListener;
import cc.lee.registry.zookeeper.listener.StateListener;
import cc.lee.registry.zookeeper.support.ConnectInfo;

/**
 * ZooKeeper操作接口
 * @author lizhitao
 */
public interface ZooKeeperClient {
	/**
	 * 新增节点
	 * @param path 节点路径
	 * @param ephemeral 是否为临时节点
	 */
	void create(String path, boolean ephemeral);
	
	/**
	 * 删除节点
	 * @param path 节点路径
	 */
	void delete(String path);
	
	/**
	 * 获取指定路径下的子节点列表
	 * @param path 节点路径
	 * @return List<String>
	 */
	List<String> getChildren(String path);
	
	/**
	 * 为指定路径添加子节点变更监听器
	 * @param path 节点路径
	 * @param listener 子节点变更监听器
	 * @return
	 */
	List<String> addChildrenListener(String path, ChildrenListener listener);

	/**
	 * 移除指定节点的指定监听器
	 * @param path 节点路径
	 * @param listener 监听器
	 */
	void removeChildrenListener(String path, ChildrenListener listener);

	/**
	 * 添加ZooKeeper状态监听器
	 * @param listener 状态监听器
	 */
	void addStateListener(StateListener listener);

	/**
	 * 移除ZooKeeper状态监听器
	 * @param listener
	 */
	void removeStateListener(StateListener listener);

	/**
	 * 查看是否已连接ZooKeeper
	 * @return
	 */
	boolean isConnected();

	/**
	 * 关闭ZooKeeper连接
	 */
	void close();
	
	/**
	 * 获取ZooKeeper连接信息
	 * @return
	 */
	ConnectInfo getConnectInfo();
}
