package com.ly.fn.inf.lnk.lookup.zk;

import java.util.List;

public interface ChildListener {
	void childChanged(String path, List<String> children);
}
