package com.ly.fn.inf.lnk.core.netty;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import com.ly.fn.inf.lnk.api.Address;
import com.ly.fn.inf.lnk.api.Application;
import com.ly.fn.inf.lnk.api.ServiceGroup;
import com.ly.fn.inf.lnk.api.annotation.LnkService;
import com.ly.fn.inf.lnk.api.exception.InvokerException;
import com.ly.fn.inf.lnk.api.flow.FlowController;
import com.ly.fn.inf.lnk.api.port.ServerPortAllocator;
import com.ly.fn.inf.lnk.api.registry.RegistryModule;
import com.ly.fn.inf.lnk.api.track.TrackModule;
import com.ly.fn.inf.lnk.core.InvokerServer;
import com.ly.fn.inf.lnk.core.processor.InvokerCommandProcessor;
import com.ly.fn.inf.lnk.core.processor.ServiceObjectFinder;
import com.ly.fn.inf.lnk.remoting.netty.NettyCommandProcessor;
import com.ly.fn.inf.lnk.remoting.netty.NettyRemotingServer;
import com.ly.fn.inf.lnk.remoting.netty.NettyServerConfigurator;
import com.ly.fn.inf.lnk.remoting.protocol.ProtocolFactorySelector;
import com.ly.fn.inf.lnk.remoting.utils.RemotingThreadFactory;
import com.ly.fn.inf.lnk.remoting.utils.RemotingUtils;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午2:16:27
 */
public class NettyInvokerServer implements InvokerServer, ApplicationListener<ContextClosedEvent>, BeanPostProcessor, PriorityOrdered, InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(NettyInvokerServer.class.getSimpleName());
    private Set<Class<?>> exportServiceInterfaces = new HashSet<Class<?>>();
    private NettyServerConfigurator serverConfigurator;
    private NettyRemotingServer remotingServer;
    private RegistryModule registryModule;
    private ServerPortAllocator serverPortAllocator;
    private Address serverAddress;
    private ProtocolFactorySelector protocolFactorySelector;
    private ServiceObjectFinder serviceObjectFinder;
    private FlowController flowController;
    private List<ServiceGroup> serviceGroups;
    private Application application;
    private TrackModule trackModule;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.start();
    }

    @Override
    public void start() {
        serverConfigurator.setListenPort(serverPortAllocator.selectPort(serverConfigurator.getListenPort(), application));
        remotingServer = new NettyRemotingServer(protocolFactorySelector, serverConfigurator);
        remotingServer.registerDefaultProcessor(this.createNettyCommandProcessor(),
                Executors.newFixedThreadPool(serverConfigurator.getDefaultServerWorkerProcessorThreads(), RemotingThreadFactory.newThreadFactory("NettyInvokerServerWorkerProcessor-%d", false)));
        remotingServer.start();
        serverAddress = new Address(RemotingUtils.getLocalAddress(), remotingServer.getServerAddress().getPort());
        log.info("NettyInvokerServer 'NettyRemotingServer' start success bind {}", serverAddress);
        if (CollectionUtils.isNotEmpty(serviceGroups)) {
            this.bind(serviceGroups.toArray(new ServiceGroup[serviceGroups.size()]));
        }
    }

    @Override
    public void bind(ServiceGroup... serviceGroups) {
        if (ArrayUtils.isEmpty(serviceGroups)) {
            log.info("bind serviceGroups is empty.");
            return;
        }
        for (ServiceGroup serviceGroup : serviceGroups) {
            int commandCode = serviceGroup.getServiceGroup().hashCode();
            this.remotingServer.registerProcessor(commandCode, this.createNettyCommandProcessor(), Executors.newFixedThreadPool(serviceGroup.getServiceGroupWorkerProcessorThreads(),
                    RemotingThreadFactory.newThreadFactory("NettyInvokerServerWorkerProcessor[" + serviceGroup.getServiceGroup() + "]-%d", false)));
            log.info("bind serviceGroup {} success.", serviceGroup.getServiceGroup());
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        this.serviceExport(bean.getClass(), bean);
        return bean;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        if (CollectionUtils.isNotEmpty(this.exportServiceInterfaces)) {
            for (Class<?> exportServiceInterface : this.exportServiceInterfaces) {
                if (exportServiceInterface.isAnnotationPresent(LnkService.class)) {
                    LnkService lnkService = exportServiceInterface.getAnnotation(LnkService.class);
                    this.unregistry(lnkService.group(), exportServiceInterface.getName(), lnkService.version(), lnkService.protocol());
                }
            }
        }
        this.shutdown();
    }

    private void serviceExport(Class<?> beanType, Object bean) {
        Class<?>[] beanInterfaces = beanType.getInterfaces();
        if (ArrayUtils.isNotEmpty(beanInterfaces)) {
            for (Class<?> beanInterface : beanInterfaces) {
                if (beanInterface.isInterface()) {
                    if (beanInterface.isAnnotationPresent(LnkService.class)) {
                        this.exportServiceInterfaces.add(beanInterface);
                        LnkService lnkService = beanInterface.getAnnotation(LnkService.class);
                        this.registry(lnkService.group(), beanInterface.getName(), lnkService.version(), lnkService.protocol(), bean);
                    }
                } else {
                    this.serviceExport(beanInterface, bean);
                }
            }
        }
    }

    @Override
    public void registry(String serviceGroup, String serviceId, int version, int protocol, Object bean) throws InvokerException {
        log.info("registry service serviceGroup : {}, serviceId : {}, version : {}, protocol : {}", new Object[] {serviceGroup, serviceId, version, protocol});
        this.registryModule.registry(serviceGroup, serviceId, version, protocol, serverAddress);
    }

    @Override
    public void unregistry(String serviceGroup, String serviceId, int version, int protocol) throws InvokerException {
        log.info("unregistry service serviceGroup : {}, serviceId : {}, version : {}, protocol : {}", new Object[] {serviceGroup, serviceId, version, protocol});
        this.registryModule.unregistry(serviceGroup, serviceId, version, protocol);
    }

    protected NettyCommandProcessor createNettyCommandProcessor() {
        InvokerCommandProcessor processor = new InvokerCommandProcessor();
        processor.setProtocolFactorySelector(protocolFactorySelector);
        processor.setServiceObjectFinder(serviceObjectFinder);
        processor.setFlowController(flowController);
        processor.setApplication(application);
        processor.setTrackModule(trackModule);
        return processor;
    }

    @Override
    public void shutdown() {
        if (remotingServer != null) {
            remotingServer.shutdown();
            remotingServer = null;
        }
    }

    @Override
    public void destroy() throws Exception {
        this.shutdown();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }

    public void setRegistryModule(RegistryModule registryModule) {
        this.registryModule = registryModule;
    }

    public void setServerConfigurator(NettyServerConfigurator serverConfigurator) {
        this.serverConfigurator = serverConfigurator;
    }

    public void setServerPortAllocator(ServerPortAllocator serverPortAllocator) {
        this.serverPortAllocator = serverPortAllocator;
    }

    public void setProtocolFactorySelector(ProtocolFactorySelector protocolFactorySelector) {
        this.protocolFactorySelector = protocolFactorySelector;
    }

    public void setServiceObjectFinder(ServiceObjectFinder serviceObjectFinder) {
        this.serviceObjectFinder = serviceObjectFinder;
    }

    public void setFlowController(FlowController flowController) {
        this.flowController = flowController;
    }

    public void setServiceGroups(List<ServiceGroup> serviceGroups) {
        this.serviceGroups = serviceGroups;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public void setTrackModule(TrackModule trackModule) {
        this.trackModule = trackModule;
    }
}
