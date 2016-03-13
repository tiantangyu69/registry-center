package cc.lee.registry.zookeeper.support;

import cc.lee.registry.URL;
import cc.lee.registry.zookeeper.ZooKeeperClient;
/**
 * @author lizhitao
 */
public interface ZooKeeperTransporter {
	ZooKeeperClient connect(URL connectInfo);
}
