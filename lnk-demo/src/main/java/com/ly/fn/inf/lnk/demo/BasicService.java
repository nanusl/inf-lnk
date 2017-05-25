package com.ly.fn.inf.lnk.demo;

import com.ly.fn.inf.lnk.api.annotation.Lnkwired;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月25日 下午7:10:35
 */
public class BasicService {

    @Lnkwired(localWiredPriority = false)
    protected HelloService helloService;
    
    @Lnkwired(localWiredPriority = false)
    protected DemoService demoService;
    
    public void setHelloService(HelloService helloService) {
        System.err.println("wired HelloService : " + helloService);
        this.helloService = helloService;
    }
    
    public void setDemoService(DemoService demoService) {
        System.err.println("wired DemoService : " + demoService);
        this.demoService = demoService;
    }
}
