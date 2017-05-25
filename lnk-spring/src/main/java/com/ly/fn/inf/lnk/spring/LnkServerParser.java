package com.ly.fn.inf.lnk.spring;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.config.AopNamespaceUtils;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import com.ly.fn.inf.lnk.api.Application;
import com.ly.fn.inf.lnk.api.ServiceGroup;
import com.ly.fn.inf.lnk.api.flow.FlowController;
import com.ly.fn.inf.lnk.api.port.ServerPortAllocator;
import com.ly.fn.inf.lnk.api.registry.RegistryModule;
import com.ly.fn.inf.lnk.api.track.TrackModule;
import com.ly.fn.inf.lnk.core.netty.NettyInvokerServer;
import com.ly.fn.inf.lnk.core.processor.def.DefaultServiceObjectFinder;
import com.ly.fn.inf.lnk.core.protocol.InvokerProtocolFactorySelector;
import com.ly.fn.inf.lnk.flow.SemaphoreFlowController;
import com.ly.fn.inf.lnk.lookup.consul.ConsulLookupRegistryModuleModule;
import com.ly.fn.inf.lnk.lookup.zk.ZokLookupRegistryModule;
import com.ly.fn.inf.lnk.port.DefaultServerPortAllocator;
import com.ly.fn.inf.lnk.remoting.netty.NettyServerConfigurator;
import com.ly.fn.inf.lnk.remoting.protocol.JacksonProtocolFactory;
import com.ly.fn.inf.lnk.spring.utils.BeanRegister;
import com.ly.fn.inf.lnk.track.LoggerTrackModule;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月23日 下午3:56:52
 */
public class LnkServerParser extends AbstractSingleBeanDefinitionParser {
    private static final Logger log = LoggerFactory.getLogger(LnkServerParser.class.getSimpleName());

    @Override
    protected Class<?> getBeanClass(Element element) {
        return NettyInvokerServer.class;
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(parserContext, element);
        NettyServerConfigurator serverConfigurator = new NettyServerConfigurator();
        int port = NumberUtils.toInt(element.getAttribute("listen-port"), -1);
        if (port > 0) {
            serverConfigurator.setListenPort(port);
        }
        serverConfigurator.setServerWorkerThreads(NumberUtils.toInt(element.getAttribute("server-worker-threads"), 10));
        serverConfigurator.setServerSelectorThreads(NumberUtils.toInt(element.getAttribute("server-selector-threads"), 5));
        serverConfigurator.setServerChannelMaxIdleTimeSeconds(NumberUtils.toInt(element.getAttribute("server-channel-maxidletime-seconds"), 120));
        serverConfigurator.setServerSocketSndBufSize(NumberUtils.toInt(element.getAttribute("server-socket-sndbuf-size"), 65535));
        serverConfigurator.setServerSocketRcvBufSize(NumberUtils.toInt(element.getAttribute("server-socket-rcvbuf-size"), 65535));
        serverConfigurator.setServerPooledByteBufAllocatorEnable(BooleanUtils.toBoolean(StringUtils.defaultString(element.getAttribute("server-socket-rcvbuf-size"), "true")));
        serverConfigurator.setDefaultServerWorkerProcessorThreads(NumberUtils.toInt(element.getAttribute("default-server-worker-processor-threads"), 10));
        serverConfigurator.setDefaultServerExecutorThreads(NumberUtils.toInt(element.getAttribute("default-server-executor-threads"), 8));
        serverConfigurator.setUseEpollNativeSelector(BooleanUtils.toBoolean(StringUtils.defaultString(element.getAttribute("use-epoll-native-selector"), "false")));
        builder.addPropertyValue("serverConfigurator", serverConfigurator);
        List<Element> registryElements = DomUtils.getChildElementsByTagName(element, "registry");
        Element registryElement = registryElements.get(0);
        RegistryModule registryModule = null;
        String type = StringUtils.defaultString(registryElement.getAttribute("type"));
        String address = StringUtils.defaultString(registryElement.getAttribute("address"));
        switch (type) {
            case "zk":
                registryModule = new ZokLookupRegistryModule(address);
                break;
            case "consul":
                registryModule = new ConsulLookupRegistryModuleModule(address);
                break;
        }
        builder.addPropertyValue("registryModule", registryModule);
        ServerPortAllocator serverPortAllocator = new DefaultServerPortAllocator();
        builder.addPropertyValue("serverPortAllocator", serverPortAllocator);
        InvokerProtocolFactorySelector protocolFactorySelector = new InvokerProtocolFactorySelector();
        protocolFactorySelector.registry(new JacksonProtocolFactory());
        builder.addPropertyValue("protocolFactorySelector", protocolFactorySelector);
        String serviceObjectFinderId = "defaultServiceObjectFinder";
        BeanRegister.register(serviceObjectFinderId, DefaultServiceObjectFinder.class, element, parserContext);
        builder.addPropertyValue("serviceObjectFinder", new RuntimeBeanReference(serviceObjectFinderId));
        List<Element> flowControlElements = DomUtils.getChildElementsByTagName(element, "flow-control");
        if (CollectionUtils.isNotEmpty(flowControlElements)) {
            Element flowControlElement = flowControlElements.get(0);
            String typeFlow = StringUtils.defaultString(flowControlElement.getAttribute("type"));
            String permitsString = StringUtils.defaultString(flowControlElement.getAttribute("permits"));
            int permits = NumberUtils.toInt(permitsString);
            if (StringUtils.isNotBlank(permitsString) && permits > 0) {
                FlowController flowController = null;
                switch (typeFlow) {
                    case "semaphore":
                        flowController = new SemaphoreFlowController(permits);
                        break;
                }
                builder.addPropertyValue("flowController", flowController);
            }
        }
        List<Element> applicationElements = DomUtils.getChildElementsByTagName(element, "application");
        Element applicationElement = applicationElements.get(0);
        Application application = new Application();
        application.setApp(applicationElement.getAttribute("app"));
        application.setType(applicationElement.getAttribute("type"));
        builder.addPropertyValue("application", application);
        List<Element> trackElements = DomUtils.getChildElementsByTagName(element, "track");
        if (CollectionUtils.isNotEmpty(trackElements)) {
            TrackModule trackModule = null;
            Element trackElement = trackElements.get(0);
            String trackType = StringUtils.defaultString(trackElement.getAttribute("type"));
            switch (trackType) {
                case "logger":
                    trackModule = new LoggerTrackModule();
                    break;
                case "lnk":
                    break;
                case "cat":
                    break;
            }
            builder.addPropertyValue("trackModule", trackModule);
        }
        List<Element> bindElements = DomUtils.getChildElementsByTagName(element, "bind");
        Element bindElement = bindElements.get(0);
        List<Element> serviceGroupElements = DomUtils.getChildElementsByTagName(bindElement, "service-group");
        List<ServiceGroup> serviceGroups = new ArrayList<ServiceGroup>();
        for (Element serviceGroupElement : serviceGroupElements) {
            ServiceGroup serviceGroup = new ServiceGroup();
            serviceGroup.setServiceGroup(StringUtils.trimToEmpty(serviceGroupElement.getAttribute("service-group")));
            serviceGroup.setServiceGroupWorkerProcessorThreads(NumberUtils.toInt(serviceGroupElement.getAttribute("service-group-worker-processor-threads"), 10));
            serviceGroups.add(serviceGroup);
        }
        builder.addPropertyValue("serviceGroups", serviceGroups);
        log.info("parse NettyInvokerServer bean success.");
    }

    @Override
    protected String getBeanClassName(Element element) {
        return element.getAttribute("id");
    }
}
