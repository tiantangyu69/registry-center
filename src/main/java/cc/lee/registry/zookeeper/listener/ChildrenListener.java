package cc.lee.registry.zookeeper.listener;

import java.util.List;
/**
 * 子节点变更监听接口
 * @author lizhitao
 */
public interface ChildrenListener {
	void changed(String path, List<String> children);
}
