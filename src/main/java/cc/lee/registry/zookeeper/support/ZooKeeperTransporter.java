package cc.lee.registry.zookeeper.support;

import cc.lee.registry.client.zookeeper.ZooKeeperClient;
import cc.lee.registry.common.URL;
/**
 * @author lizhitao
 */
public interface ZooKeeperTransporter {
	ZooKeeperClient connect(URL connectInfo);
}
