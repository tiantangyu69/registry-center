package cc.lee.registry.remoting.zookeeper.curator;

import cc.lee.registry.common.URL;
import cc.lee.registry.remoting.zookeeper.ZooKeeperClient;
import cc.lee.registry.remoting.zookeeper.support.ZooKeeperTransporter;

public class CuratorZooKeeperTransporter implements ZooKeeperTransporter {
	@Override
	public ZooKeeperClient connect(URL connectInfo) {
		return new CuratorZooKeeperClient(connectInfo);
	}
}
