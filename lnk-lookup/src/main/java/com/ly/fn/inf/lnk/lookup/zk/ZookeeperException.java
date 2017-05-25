package com.ly.fn.inf.lnk.lookup.zk;

/**
 * @author scott
 *
 */
public class ZookeeperException extends Exception {

	private static final long serialVersionUID = 1L;

	public ZookeeperException(String error) {
		super(error);
	}

	public ZookeeperException(String error, Throwable tr) {
		super(error, tr);
	}
}
