package cc.lee.registry.client.zookeeper.curator;

import cc.lee.registry.client.zookeeper.ZooKeeperClient;
import cc.lee.registry.client.zookeeper.support.ZooKeeperTransporter;
import cc.lee.registry.common.URL;

public class CuratorZooKeeperTransporter implements ZooKeeperTransporter {
	@Override
	public ZooKeeperClient connect(URL connectInfo) {
		return new CuratorZooKeeperClient(connectInfo);
	}
}
