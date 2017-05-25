package com.ly.fn.inf.lnk.demo.def;

import com.ly.fn.inf.lnk.api.CommandCallback;
import com.ly.fn.inf.lnk.api.RemoteObjectFactory;
import com.ly.fn.inf.lnk.api.RemoteObjectFactoryAware;
import com.ly.fn.inf.lnk.demo.BasicService;
import com.ly.fn.inf.lnk.demo.HelloService;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月24日 下午2:58:27
 */
public class DefaultHelloService extends BasicService implements HelloService, RemoteObjectFactoryAware {

	private RemoteObjectFactory remoteObjectFactory;
	
	@Override
	public void setRemoteObjectFactory(RemoteObjectFactory remoteObjectFactory) {
		this.remoteObjectFactory = remoteObjectFactory;
	}

	@Override
	public String welcome1(String name) {
	    System.err.println("welcome1 remoteObjectFactory : " + remoteObjectFactory);
		return "你好 " + name + ", " + demoService.demo(name);
	}

	@Override
	public void welcome2(String name, CommandCallback<String> callback) {
        System.err.println("welcome2 remoteObjectFactory : " + remoteObjectFactory);
		System.err.println("hello " + name + ", " + demoService.demo(name));
		callback.onComplete("hello " + name + ", " + demoService.demo(name));
	}
}
