package com.ly.fn.inf.lnk.demo.test.thread;

public enum ResponseType {
	Fallback("fallback"),
	Success("success"),
	Fail("fail"),
	Lost("lost");
	private String type;
	private ResponseType(String type){
		this.type = type;
	}
}
