package cc.lee.registry.util;

public class ConfigUtils {
	public static boolean isNotEmpty(String value) {
        return ! isEmpty(value);
    }
	
	public static boolean isEmpty(String value) {
		return value == null || value.length() == 0
				|| "false".equalsIgnoreCase(value)
				|| "0".equalsIgnoreCase(value)
				|| "null".equalsIgnoreCase(value)
				|| "N/A".equalsIgnoreCase(value);
	}
}
