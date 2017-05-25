package com.ly.fn.inf.lnk.demo.server;

import com.ly.fn.inf.lnk.api.Application;
import com.ly.fn.inf.lnk.api.CommandVersion;
import com.ly.fn.inf.lnk.api.InvokerCommand;
import com.ly.fn.inf.lnk.api.ServiceGroup;
import com.ly.fn.inf.lnk.api.exception.NotFoundServiceException;
import com.ly.fn.inf.lnk.api.flow.FlowController;
import com.ly.fn.inf.lnk.api.port.ServerPortAllocator;
import com.ly.fn.inf.lnk.api.track.TrackModule;
import com.ly.fn.inf.lnk.core.netty.NettyInvokerServer;
import com.ly.fn.inf.lnk.core.processor.ServiceObject;
import com.ly.fn.inf.lnk.core.processor.ServiceObjectFinder;
import com.ly.fn.inf.lnk.core.protocol.InvokerProtocolFactorySelector;
import com.ly.fn.inf.lnk.demo.BasicInvoker;
import com.ly.fn.inf.lnk.demo.HelloService;
import com.ly.fn.inf.lnk.demo.def.DefaultHelloService;
import com.ly.fn.inf.lnk.remoting.netty.NettyServerConfigurator;
import com.ly.fn.inf.lnk.remoting.protocol.JacksonProtocolFactory;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月24日 下午3:21:28
 */
public class BasicInvokerServer extends BasicInvoker {

    public static void main(String[] args) {
        BasicInvokerServer basicInvokerServer = new BasicInvokerServer();
        basicInvokerServer.start();
    }

    private NettyInvokerServer nettyInvokerServer;

    private void start() {
        ServerPortAllocator serverPortAllocator = new ServerPortAllocator() {
            @Override
            public int selectPort(int expectListenPort, Application application) {
                return 8888;
            }
        };
        InvokerProtocolFactorySelector protocolFactorySelector = new InvokerProtocolFactorySelector();
        protocolFactorySelector.registry(new JacksonProtocolFactory());
        ServiceObjectFinder serviceObjectFinder = new ServiceObjectFinder() {
            @Override
            public ServiceObject getServiceObject(InvokerCommand command) throws NotFoundServiceException {
                ServiceObject serviceObject = new ServiceObject();
                serviceObject.setService(new DefaultHelloService());
                try {
                    if (command.getServiceId().equals(HelloService.class.getName())) {
                        if (command.getMethod().equals("welcome1")) {
                            serviceObject.setMethod(HelloService.class.getMethod("welcome1", new Class<?>[] {String.class}));
                        } else if (command.getMethod().equals("welcome2")) {
                            serviceObject.setMethod(HelloService.class.getMethod("welcome2", new Class<?>[] {String.class}));
                        }
                    }
                } catch (NoSuchMethodException e) {
                    e.printStackTrace(System.err);
                } catch (SecurityException e) {
                    e.printStackTrace(System.err);
                }
                return serviceObject;
            }
        };
        FlowController flowController = new FlowController() {

            @Override
            public boolean tryAcquireFailure(long timeoutMillis) {
                return false;
            }

            @Override
            public void release() {}};
        Application application = new Application();
        application.setApp("biz-pay-bgw-payment-srv");
        application.setType("jar");
        TrackModule trackModule = new TrackModule() {
            @Override
            public void track(InvokerCommand command, Application application) {
                System.err.println("track : " + jacksonSerializer.serializeAsString(command) + " invoke " + application.getApp());
            }
        };
        nettyInvokerServer = new NettyInvokerServer();
        nettyInvokerServer.setServerConfigurator(new NettyServerConfigurator());
        nettyInvokerServer.setRegistryModule(registryModule);
        nettyInvokerServer.setServerPortAllocator(serverPortAllocator);
        nettyInvokerServer.setProtocolFactorySelector(protocolFactorySelector);
        nettyInvokerServer.setServiceObjectFinder(serviceObjectFinder);
        nettyInvokerServer.setFlowController(flowController);
        nettyInvokerServer.setApplication(application);
        nettyInvokerServer.setTrackModule(trackModule);
        nettyInvokerServer.start();
        ServiceGroup serviceGroup = new ServiceGroup();
        serviceGroup.setServiceGroup("biz-pay-bgw-payment.srv");
        nettyInvokerServer.bind(serviceGroup);
        nettyInvokerServer.registry(serviceGroup.getServiceGroup(), HelloService.class.getName(), CommandVersion.V1, 0, new DefaultHelloService());
    }
}
