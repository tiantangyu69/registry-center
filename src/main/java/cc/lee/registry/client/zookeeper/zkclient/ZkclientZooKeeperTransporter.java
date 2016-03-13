package cc.lee.registry.client.zookeeper.zkclient;

import cc.lee.registry.client.zookeeper.ZooKeeperClient;
import cc.lee.registry.client.zookeeper.support.ZooKeeperTransporter;
import cc.lee.registry.common.URL;

public class ZkclientZooKeeperTransporter implements ZooKeeperTransporter {
	@Override
	public ZooKeeperClient connect(URL connectInfo) {
		return new ZkclientZookeeperClient(connectInfo);
	}
}
