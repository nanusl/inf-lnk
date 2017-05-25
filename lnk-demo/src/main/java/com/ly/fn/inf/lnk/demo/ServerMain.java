package com.ly.fn.inf.lnk.demo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.ly.fn.inf.lnk.api.CommandCallback;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月24日 下午8:41:42
 */
public class ServerMain {
    private static final Logger log = LoggerFactory.getLogger(ServerMain.class);
    static ClassPathXmlApplicationContext context;
    static HelloService helloService;
    static {
        System.setProperty("host.name", "lf");
        System.setProperty("app.name", "lnk.test");
        System.setProperty("ins.num", "1");
        System.setProperty("lf.lnk.test.1.port", "30000");
        String configLocation = "lnk-config.xml";
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(configLocation);
        ctx.registerShutdownHook();
        final HelloService helloService = ctx.getBean(HelloService.class);
        ServerMain.context = ctx;
        ServerMain.helloService = helloService;
        log.info("LNK Server started.");
        System.out.println("LNK Server started.");
    }
    
    public static void main(String[] args) throws Exception {
        helloService.welcome2("刘飞", new CommandCallback<String>() {
            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }
            
            @Override
            public void onComplete(String response) {
                System.err.println("async callback : " + response);
            }
        });
    }

    public static void main1(String[] args) throws Exception {
        ExecutorService exec = Executors.newCachedThreadPool();
        final Semaphore semp = new Semaphore(100000);
        for (int i = 0; i < 200000; i++) {
            final int num = i;
            Runnable run = new Runnable() {
                @Override
                public void run() {
                    try {
                        semp.acquire();
                        System.err.println("第 " + num + " 个线程 " + helloService.welcome1("刘飞"));
                        semp.release();
                    } catch (InterruptedException e) {
                        e.printStackTrace(System.err);
                    }
                }
            };
            exec.execute(run);
        }
    }
}
