package cc.lee.registry.zookeeper.curator;

import cc.lee.registry.zookeeper.ZooKeeperClient;
import cc.lee.registry.zookeeper.support.ConnectInfo;
import cc.lee.registry.zookeeper.support.ZooKeeperTransporter;

public class CuratorZooKeeperTransporter implements ZooKeeperTransporter {
	@Override
	public ZooKeeperClient connect(ConnectInfo connectInfo) {
		return new CuratorZooKeeperClient(connectInfo);
	}
}
