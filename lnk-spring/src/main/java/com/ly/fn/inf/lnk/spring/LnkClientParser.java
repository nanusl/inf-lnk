package com.ly.fn.inf.lnk.spring;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.config.AopNamespaceUtils;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import com.ly.fn.inf.lnk.api.Application;
import com.ly.fn.inf.lnk.api.cluster.LoadBalance;
import com.ly.fn.inf.lnk.api.flow.FlowController;
import com.ly.fn.inf.lnk.api.lookup.LookupModule;
import com.ly.fn.inf.lnk.cluster.ConsistencyHashLoadBalance;
import com.ly.fn.inf.lnk.cluster.RandomLoadBalance;
import com.ly.fn.inf.lnk.cluster.RoundRobinLoadBalance;
import com.ly.fn.inf.lnk.core.caller.InvokerRemoteObjectFactory;
import com.ly.fn.inf.lnk.core.netty.NettyInvoker;
import com.ly.fn.inf.lnk.core.protocol.InvokerProtocolFactorySelector;
import com.ly.fn.inf.lnk.flow.SemaphoreFlowController;
import com.ly.fn.inf.lnk.lookup.consul.ConsulLookupRegistryModuleModule;
import com.ly.fn.inf.lnk.lookup.zk.ZokLookupRegistryModule;
import com.ly.fn.inf.lnk.remoting.netty.NettyClientConfigurator;
import com.ly.fn.inf.lnk.remoting.protocol.JacksonProtocolFactory;
import com.ly.fn.inf.lnk.spring.utils.BeanRegister;
import com.ly.fn.inf.lnk.spring.utils.BeanRegister.BeanDefinitionCallback;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月23日 下午3:56:52
 */
public class LnkClientParser extends AbstractSingleBeanDefinitionParser {
    private static final Logger log = LoggerFactory.getLogger(LnkClientParser.class.getSimpleName());
    
    @Override
    protected Class<?> getBeanClass(Element element) {
        return NettyInvoker.class;
    }

    @Override
    protected void doParse(final Element element, final ParserContext parserContext, final BeanDefinitionBuilder builder) {
        AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(parserContext, element);
        BeanRegister.register(InvokerRemoteObjectFactory.DEFAULT_INVOKER_REMOTE_OBJECT_FACTORY, InvokerRemoteObjectFactory.class, element, parserContext, new BeanDefinitionCallback() {
            public void doInRegister(RootBeanDefinition beanDefinition) {
                beanDefinition.getPropertyValues().addPropertyValue("invoker", new RuntimeBeanReference(LnkClientParser.this.getBeanClassName(element)));
            }
        });
        NettyClientConfigurator clientConfigurator = new NettyClientConfigurator();
        clientConfigurator.setClientWorkerThreads(NumberUtils.toInt(element.getAttribute("client-worker-threads"), 4));
        clientConfigurator.setConnectTimeoutMillis(NumberUtils.toInt(element.getAttribute("connect-timeout-millis"), 3000));
        clientConfigurator.setClientChannelMaxIdleTimeSeconds(NumberUtils.toInt(element.getAttribute("client-channel-maxidletime-seconds"), 120));
        clientConfigurator.setClientSocketSndBufSize(NumberUtils.toInt(element.getAttribute("client-socket-sndbuf-size"), 65535));
        clientConfigurator.setClientSocketRcvBufSize(NumberUtils.toInt(element.getAttribute("client-socket-rcvbuf-size"), 65535));
        clientConfigurator.setDefaultClientExecutorThreads(NumberUtils.toInt(element.getAttribute("default-client-executor-threads"), 4));
        builder.addPropertyValue("clientConfigurator", clientConfigurator);
        List<Element> lookupElements = DomUtils.getChildElementsByTagName(element, "lookup");
        Element lookupElement = lookupElements.get(0);
        String type = StringUtils.defaultString(lookupElement.getAttribute("type"));
        String address = StringUtils.defaultString(lookupElement.getAttribute("address"));
        LookupModule lookupModule = null;
        switch (type) {
            case "zk":
                lookupModule = new ZokLookupRegistryModule(address);
                break;
            case "consul":
                lookupModule = new ConsulLookupRegistryModuleModule(address);
                break;
        }
        
        builder.addPropertyValue("lookupModule", lookupModule);
        List<Element> loadBalanceElements = DomUtils.getChildElementsByTagName(element, "load-balance");
        Element loadBalanceElement= loadBalanceElements.get(0);
        String loadBalanceType = StringUtils.defaultString(loadBalanceElement.getAttribute("type"));
        LoadBalance loadBalance = null;
        switch (loadBalanceType) {
            case "hash":
                loadBalance = new ConsistencyHashLoadBalance();
                break;
            case "random":
                loadBalance = new RandomLoadBalance();
                break;
            case "roundrobin":
                loadBalance = new RoundRobinLoadBalance();
                break;
            default:
                break;
        }
        builder.addPropertyValue("loadBalance", loadBalance);
        InvokerProtocolFactorySelector protocolFactorySelector = new InvokerProtocolFactorySelector();
        protocolFactorySelector.registry(new JacksonProtocolFactory());
        builder.addPropertyValue("protocolFactorySelector", protocolFactorySelector);
        List<Element> applicationElements = DomUtils.getChildElementsByTagName(element, "application");
        Element applicationElement = applicationElements.get(0);
        Application application = new Application();
        application.setApp(applicationElement.getAttribute("app"));
        application.setType(applicationElement.getAttribute("type"));
        builder.addPropertyValue("application", application);
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
        log.info("parse NettyInvoker bean success.");
    }
    
    @Override
    protected String getBeanClassName(Element element) {
        return element.getAttribute("id");
    }
}
