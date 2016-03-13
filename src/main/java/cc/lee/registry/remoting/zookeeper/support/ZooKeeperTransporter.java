package cc.lee.registry.remoting.zookeeper.support;

import cc.lee.registry.common.URL;
import cc.lee.registry.remoting.zookeeper.ZooKeeperClient;
/**
 * @author lizhitao
 */
public interface ZooKeeperTransporter {
	ZooKeeperClient connect(URL connectInfo);
}
