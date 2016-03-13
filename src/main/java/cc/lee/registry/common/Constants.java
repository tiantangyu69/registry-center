package cc.lee.registry.common;

import java.util.regex.Pattern;

public class Constants {
	private Constants() {
	}

	public static final String BACKUP_KEY = "backup";
	public static final String DEFAULT_KEY_PREFIX = "default.";
	public static final Pattern COMMA_SPLIT_PATTERN = Pattern
			.compile("\\s*[,]+\\s*");
	public static final String LOCALHOST_KEY = "localhost";
	public static final String ANYHOST_VALUE = "0.0.0.0";
	public static final String ANYHOST_KEY = "anyhost";
	public static final String GROUP_KEY = "group";
	public static final String VERSION_KEY = "version";
	public static final String INTERFACE_KEY = "interface";
	public static final String EMPTY_PROTOCOL = "empty";
	public static final String DYNAMIC_KEY = "dynamic";
	public static final String CATEGORY_KEY = "category";
	public static final String PROVIDERS_CATEGORY = "providers";
	public static final String DEFAULT_CATEGORY = PROVIDERS_CATEGORY;
	public static final String ANY_VALUE = "*";
	public static final String ENABLED_KEY = "enabled";
	public static final String CLASSIFIER_KEY = "classifier";
	public static final String REMOVE_VALUE_PREFIX = "-";
	public static final String FILE_KEY = "file";
	public static final String CHECK_KEY = "check";
	public static final String CONSUMER_PROTOCOL = "consumer";
	/**
	 * 注册中心是否同步存储文件，默认异步
	 */
	public static final String REGISTRY_FILESAVE_SYNC_KEY = "save.file";
	/**
	 * 注册中心失败事件重试事件
	 */
	public static final String REGISTRY_RETRY_PERIOD_KEY = "retry.period";
	/**
	 * 重试周期
	 */
	public static final int DEFAULT_REGISTRY_RETRY_PERIOD = 5 * 1000;
}
