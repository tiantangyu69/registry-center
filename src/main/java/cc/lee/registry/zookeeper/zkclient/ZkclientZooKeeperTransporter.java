package cc.lee.registry.zookeeper.zkclient;

import cc.lee.registry.zookeeper.ZooKeeperClient;
import cc.lee.registry.zookeeper.support.ConnectInfo;
import cc.lee.registry.zookeeper.support.ZooKeeperTransporter;

public class ZkclientZooKeeperTransporter implements ZooKeeperTransporter {
	@Override
	public ZooKeeperClient connect(ConnectInfo connectInfo) {
		return new ZkclientZookeeperClient(connectInfo);
	}
}