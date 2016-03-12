package cc.lee.registry.zookeeper.support;

import cc.lee.registry.zookeeper.ZooKeeperClient;
/**
 * @author lizhitao
 */
public interface ZooKeeperTransporter {
	ZooKeeperClient connect(ConnectInfo connectInfo);
}
