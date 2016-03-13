package cc.lee.registry.zookeeper.curator;

import cc.lee.registry.client.zookeeper.ZooKeeperClient;
import cc.lee.registry.common.URL;
import cc.lee.registry.zookeeper.support.ZooKeeperTransporter;

public class CuratorZooKeeperTransporter implements ZooKeeperTransporter {
	@Override
	public ZooKeeperClient connect(URL connectInfo) {
		return new CuratorZooKeeperClient(connectInfo);
	}
}
