package com.ly.fn.inf.lnk.demo.client;

import java.util.List;

import com.ly.fn.inf.lnk.api.Address;
import com.ly.fn.inf.lnk.api.Application;
import com.ly.fn.inf.lnk.api.CommandCallback;
import com.ly.fn.inf.lnk.api.cluster.LoadBalance;
import com.ly.fn.inf.lnk.api.flow.FlowController;
import com.ly.fn.inf.lnk.api.lookup.LookupModule;
import com.ly.fn.inf.lnk.core.caller.InvokerRemoteObjectFactory;
import com.ly.fn.inf.lnk.core.netty.NettyInvoker;
import com.ly.fn.inf.lnk.core.protocol.InvokerProtocolFactorySelector;
import com.ly.fn.inf.lnk.demo.BasicInvoker;
import com.ly.fn.inf.lnk.demo.HelloService;
import com.ly.fn.inf.lnk.remoting.netty.NettyClientConfigurator;
import com.ly.fn.inf.lnk.remoting.protocol.JacksonProtocolFactory;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月24日 下午3:22:02
 */
public class BasicInvokerClient extends BasicInvoker {

    public static void main(String[] args) throws Throwable {
        BasicInvokerClient basicInvokerClient = new BasicInvokerClient();
        try {
            basicInvokerClient.start();
            InvokerRemoteObjectFactory remoteObjectFactory = new InvokerRemoteObjectFactory();
            remoteObjectFactory.setInvoker(basicInvokerClient.nettyInvoker);
            HelloService helloService = remoteObjectFactory.getRemoteObject(HelloService.class);
            System.err.println(helloService.welcome1("刘飞"));
            helloService.welcome2("刘飞", new CommandCallback<String>() {
                
                @Override
                public void onError(Throwable e) {}
                
                @Override
                public void onComplete(String response) {}
            });
        } catch (Exception e) {
            e.printStackTrace(System.err);
            basicInvokerClient.nettyInvoker.shutdown();
        }
    }

    private NettyInvoker nettyInvoker;

    private void start() {
        InvokerProtocolFactorySelector protocolFactorySelector = new InvokerProtocolFactorySelector();
        protocolFactorySelector.registry(new JacksonProtocolFactory());
        FlowController flowController = new FlowController() {

            @Override
            public boolean tryAcquireFailure(long timeoutMillis) {
                return false;
            }

            @Override
            public void release() {}};
        Application application = new Application();
        application.setApp("biz-pay-fi-ate-srv");
        application.setType("jar");
        LookupModule lookupModule = new LookupModule() {
            @Override
            public Address lookup(String serviceGroup, String serviceId, int version, int protocol, String loadBalanceFactor, LoadBalance loadBalance) {
                return loadBalance.select(loadBalanceFactor, new String[] {getLookup(serviceGroup + serviceId + version + protocol)});
            }
        };
        LoadBalance loadBalance = new LoadBalance() {
            @Override
            public Address select(String loadBalanceFactor, String[] candidates) {
                return new Address(candidates[0]);
            }

            @Override
            public Address select(String loadBalanceFactor, List<String> candidates) {
                return null;
            }
        };
        nettyInvoker = new NettyInvoker();
        nettyInvoker.setClientConfigurator(new NettyClientConfigurator());
        nettyInvoker.setLookupModule(lookupModule);
        nettyInvoker.setLoadBalance(loadBalance);
        nettyInvoker.setProtocolFactorySelector(protocolFactorySelector);
        nettyInvoker.setApplication(application);
        nettyInvoker.setFlowController(flowController);
        nettyInvoker.start();
    }
}
