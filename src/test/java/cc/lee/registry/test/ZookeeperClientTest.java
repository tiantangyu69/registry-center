package cc.lee.registry.test;

import java.util.List;

import org.junit.Test;

import cc.lee.registry.zookeeper.ZooKeeperClient;
import cc.lee.registry.zookeeper.listener.ChildrenListener;
import cc.lee.registry.zookeeper.support.ConnectInfo;
import cc.lee.registry.zookeeper.zkclient.ZkclientZooKeeperTransporter;

public class ZookeeperClientTest {
	@Test
	public void testClient() throws InterruptedException {
		ConnectInfo connectInfo = new ConnectInfo();
		connectInfo.setAddress("localhost:2181");
		ZooKeeperClient client = new ZkclientZooKeeperTransporter()
				.connect(connectInfo);
		String path = "/sward/provider";
		client.create(path, false);
		client.addChildrenListener(path, new ChildrenListener() {
			@Override
			public void changed(String path, List<String> children) {
				System.out.println(path + ", " + children);
			}
		});
		
		for (int i = 0; i < 5; i++) {
			client.create(path + "/node" + i , true);
		}
		Thread.sleep(3000);
		client.close();
	}
}
