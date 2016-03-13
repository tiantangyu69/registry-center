package cc.lee.registry.remoting.zookeeper.zkclient;

import cc.lee.registry.common.URL;
import cc.lee.registry.remoting.zookeeper.ZooKeeperClient;
import cc.lee.registry.remoting.zookeeper.support.ZooKeeperTransporter;

public class ZkclientZooKeeperTransporter implements ZooKeeperTransporter {
	@Override
	public ZooKeeperClient connect(URL connectInfo) {
		return new ZkclientZookeeperClient(connectInfo);
	}
}
