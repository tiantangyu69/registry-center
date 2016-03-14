package cc.lee.registry.common;

/**
 * @author lizhitao
 */
public interface Node {
	/**
	 * get url
	 * 
	 * @return URL
	 */
	URL getURL();

	/**
	 * is available
	 * 
	 * @return boolean
	 */
	boolean isAvailable();

	/**
	 * destroy.
	 */
	void destroy();
}
