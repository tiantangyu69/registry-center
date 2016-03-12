package cc.lee.registry.zookeeper.support;

/**
 * ZooKeeper连接信息
 * @author lizhitao
 */
public class ConnectInfo {
	/**
	 * 连接地址
	 */
	private String address;
	/**
	 * 授权信息
	 */
	private String authority;

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getAuthority() {
		return authority;
	}

	public void setAuthority(String authority) {
		this.authority = authority;
	}
}
