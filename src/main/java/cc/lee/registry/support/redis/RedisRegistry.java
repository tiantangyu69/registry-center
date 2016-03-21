/**
 * 
 */
package cc.lee.registry.support.redis;

import cc.lee.registry.common.URL;
import cc.lee.registry.support.AbstractRegistry;

/**
 * @author lizhitao
 * 使用 redis 作为注册中心
 */
public class RedisRegistry extends AbstractRegistry {
	public RedisRegistry(URL url) {
		super(url);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cc.lee.registry.common.Node#isAvailable()
	 */
	@Override
	public boolean isAvailable() {
		return false;
	}

}
