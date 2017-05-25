package com.ly.fn.inf.lnk.core.netty;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.FieldFilter;

import com.ly.fn.inf.lnk.api.Address;
import com.ly.fn.inf.lnk.api.Application;
import com.ly.fn.inf.lnk.api.InvokerCommand;
import com.ly.fn.inf.lnk.api.RemoteObjectFactory;
import com.ly.fn.inf.lnk.api.RemoteObjectFactoryAware;
import com.ly.fn.inf.lnk.api.annotation.Lnkwired;
import com.ly.fn.inf.lnk.api.cluster.LoadBalance;
import com.ly.fn.inf.lnk.api.exception.InvokerException;
import com.ly.fn.inf.lnk.api.exception.InvokerRejectException;
import com.ly.fn.inf.lnk.api.exception.InvokerTimeoutException;
import com.ly.fn.inf.lnk.api.flow.FlowController;
import com.ly.fn.inf.lnk.api.lookup.LookupModule;
import com.ly.fn.inf.lnk.core.Invoker;
import com.ly.fn.inf.lnk.core.InvokerCallback;
import com.ly.fn.inf.lnk.core.caller.InvokerRemoteObjectFactory;
import com.ly.fn.inf.lnk.remoting.RemotingCallback;
import com.ly.fn.inf.lnk.remoting.ReplyFuture;
import com.ly.fn.inf.lnk.remoting.exception.RemotingConnectException;
import com.ly.fn.inf.lnk.remoting.exception.RemotingSendRequestException;
import com.ly.fn.inf.lnk.remoting.exception.RemotingTimeoutException;
import com.ly.fn.inf.lnk.remoting.netty.NettyClientConfigurator;
import com.ly.fn.inf.lnk.remoting.netty.NettyRemotingClient;
import com.ly.fn.inf.lnk.remoting.protocol.ProtocolFactory;
import com.ly.fn.inf.lnk.remoting.protocol.ProtocolFactorySelector;
import com.ly.fn.inf.lnk.remoting.protocol.RemotingCommand;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午2:16:51
 */
public class NettyInvoker implements Invoker, BeanPostProcessor, BeanFactoryAware, PriorityOrdered, InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(NettyInvoker.class.getSimpleName());
    private NettyClientConfigurator clientConfigurator;
    private NettyRemotingClient remotingClient;
    private LookupModule lookupModule;
    private LoadBalance loadBalance;
    private ProtocolFactorySelector protocolFactorySelector;
    private Application application;
    private FlowController flowController;
    private BeanFactory beanFactory;
    private RemoteObjectFactory remoteObjectFactory;

    private final class LnkwiredFieldFilter implements FieldFilter {
        public boolean matches(Field field) {
            return field.isAnnotationPresent(Lnkwired.class);
        }
    }

    private final class LnkwiredFieldCallback implements FieldCallback {
        private final Object bean;

        public LnkwiredFieldCallback(Object bean) {
            super();
            this.bean = bean;
        }

        public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
            if (bean instanceof RemoteObjectFactoryAware) {
                if (field.getType().equals(RemoteObjectFactory.class)) {
                    return;
                }
            }
            Lnkwired lnkwired = field.getAnnotation(Lnkwired.class);
            if (lnkwired == null) {
                return;
            }
            BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(bean);
            if (lnkwired.localWiredPriority()) {
                Object wiredBean = beanFactory.getBean(field.getType());
                if (wiredBean != null) {
                    beanWrapper.setPropertyValue(field.getName(), wiredBean);
                    return;
                }
            }
            beanWrapper.setPropertyValue(field.getName(), remoteObjectFactory.getRemoteObject(field.getType()));
        }
    }

    @Override
    public void start() {
        remotingClient = new NettyRemotingClient(protocolFactorySelector, clientConfigurator);
        remotingClient.start();
        log.info("NettyInvoker 'NettyRemotingClient' start success.");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.start();
        this.remoteObjectFactory = beanFactory.getBean(InvokerRemoteObjectFactory.DEFAULT_INVOKER_REMOTE_OBJECT_FACTORY, RemoteObjectFactory.class);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof RemoteObjectFactoryAware) {
            ((RemoteObjectFactoryAware) bean).setRemoteObjectFactory(remoteObjectFactory);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        ReflectionUtils.doWithFields(bean.getClass(), new LnkwiredFieldCallback(bean), new LnkwiredFieldFilter());
        return bean;
    }

    @Override
    public InvokerCommand invokeSync(final InvokerCommand command, final long timeoutMillis) throws InvokerException, InvokerTimeoutException {
        if (this.tryAcquire(timeoutMillis)) {
            throw new InvokerRejectException(command.commandSignature());
        }
        try {
            long startMillis = System.currentTimeMillis();
            command.setApplication(application);
            int commandCode = command.getServiceGroup().hashCode();
            ProtocolFactory protocolFactory = protocolFactorySelector.select(command.getProtocolCode());
            RemotingCommand request = new RemotingCommand();
            request.setCode(commandCode);
            request.setVersion(command.getVersion());
            request.setProtocolCode(command.getProtocolCode());
            protocolFactory.encode(command, request);
            Address addr = lookupModule.lookup(command.getServiceGroup(), command.getServiceId(), command.getVersion(), command.getProtocolCode(), command.getId(), loadBalance);
            RemotingCommand response = remotingClient.invokeSync(addr.toString(), request, timeoutMillis);
            if (commandCode == response.getCode()) {
                InvokerCommand invokerCommand = protocolFactory.decode(InvokerCommand.class, response);
                long endMillis = System.currentTimeMillis();
                log.info("invoker correlationId<{}>, serviceId<{}>, used {}(ms) success.", new Object[] {command.getId(), command.commandSignature(), (endMillis - startMillis)});
                return invokerCommand;
            }
            log.error("invoker correlationId<" + command.getId() + ">, serviceId<{}>, code<{}> Error.", new Object[] {command.commandSignature(), response.getCode()});
            throw new InvokerException("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + ">, code<" + response.getCode() + "> Error.");
        } catch (RemotingConnectException e) {
            log.error("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
            throw new InvokerException("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
        } catch (RemotingSendRequestException e) {
            log.error("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
            throw new InvokerException("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
        } catch (RemotingTimeoutException e) {
            log.error("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> timeout " + e.getLocalizedMessage(), e);
            throw new InvokerTimeoutException("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> timeout " + e.getLocalizedMessage(), e);
        } catch (InterruptedException e) {
            log.error("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
            throw new InvokerException("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
        } finally {
            this.release();
        }
    }

    @Override
    public void invokeAsync(final InvokerCommand command, final long timeoutMillis, final InvokerCallback callback) throws InvokerException, InvokerTimeoutException {
        if (this.tryAcquire(timeoutMillis)) {
            throw new InvokerRejectException(command.commandSignature());
        }
        try {
            final long startMillis = System.currentTimeMillis();
            command.setApplication(application);
            final int commandCode = command.getServiceGroup().hashCode();
            final ProtocolFactory protocolFactory = protocolFactorySelector.select(command.getProtocolCode());
            RemotingCommand request = new RemotingCommand();
            request.setCode(commandCode);
            request.setVersion(command.getVersion());
            request.setProtocolCode(command.getProtocolCode());
            protocolFactory.encode(command, request);
            Address addr = lookupModule.lookup(command.getServiceGroup(), command.getServiceId(), command.getVersion(), command.getProtocolCode(), command.getId(), loadBalance);
            remotingClient.invokeAsync(addr.toString(), request, timeoutMillis, new RemotingCallback() {
                @Override
                public void onComplete(ReplyFuture replyFuture) {
                    if (replyFuture.getResponse() == null) {
                        callback.onError(new InvokerException("Can't found RemotingCommand response from ReplyFuture."));
                        return;
                    }
                    RemotingCommand response = replyFuture.getResponse();
                    if (commandCode == response.getCode()) {
                        InvokerCommand invokerCommand = protocolFactory.decode(InvokerCommand.class, response);
                        long endMillis = System.currentTimeMillis();
                        log.info("invoker correlationId<{}>, serviceId<{}>, used {}(ms) success.", new Object[] {command.getId(), command.commandSignature(), (endMillis - startMillis)});
                        callback.onComplete(invokerCommand);
                        return;
                    }
                    callback.onError(new InvokerException("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + ">, code<" + response.getCode() + "> Error."));
                }
            });
        } catch (RemotingConnectException e) {
            log.error("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
            throw new InvokerException("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
        } catch (RemotingSendRequestException e) {
            log.error("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
            throw new InvokerException("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
        } catch (RemotingTimeoutException e) {
            log.error("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> timeout " + e.getLocalizedMessage(), e);
            throw new InvokerTimeoutException("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> timeout " + e.getLocalizedMessage(), e);
        } catch (InterruptedException e) {
            log.error("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
            throw new InvokerException("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
        } finally {
            this.release();
        }
    }

    @Override
    public void invokeOneway(InvokerCommand command) throws InvokerException, InvokerTimeoutException {
        if (this.tryAcquire(3000L)) {
            throw new InvokerRejectException(command.commandSignature());
        }
        try {
            long startMillis = System.currentTimeMillis();
            command.setApplication(application);
            int commandCode = command.getServiceGroup().hashCode();
            ProtocolFactory protocolFactory = protocolFactorySelector.select(command.getProtocolCode());
            RemotingCommand request = new RemotingCommand();
            request.setCode(commandCode);
            request.setVersion(command.getVersion());
            request.setProtocolCode(command.getProtocolCode());
            protocolFactory.encode(command, request);
            Address addr = lookupModule.lookup(command.getServiceGroup(), command.getServiceId(), command.getVersion(), command.getProtocolCode(), command.getId(), loadBalance);
            remotingClient.invokeOneway(addr.toString(), request);
            long endMillis = System.currentTimeMillis();
            log.info("invoker correlationId<{}>, serviceId<{}>, used {}(ms) success.", new Object[] {command.getId(), command.commandSignature(), (endMillis - startMillis)});
        } catch (RemotingConnectException e) {
            log.error("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
            throw new InvokerException("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
        } catch (RemotingSendRequestException e) {
            log.error("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
            throw new InvokerException("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
        } catch (InterruptedException e) {
            log.error("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
            throw new InvokerException("invoker correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
        } finally {
            this.release();
        }
    }

    @Override
    public void shutdown() {
        if (remotingClient != null) {
            remotingClient.shutdown();
            remotingClient = null;
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void destroy() throws Exception {
        this.shutdown();
    }

    private boolean tryAcquire(long timeoutMillis) {
        if (flowController == null) {
            return false;
        }
        return flowController.tryAcquireFailure(timeoutMillis);
    }
    
    private void release() {
        if (flowController == null) {
            return;
        }
        flowController.release();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    public void setClientConfigurator(NettyClientConfigurator clientConfigurator) {
        this.clientConfigurator = clientConfigurator;
    }

    public void setLookupModule(LookupModule lookupModule) {
        this.lookupModule = lookupModule;
    }

    public void setLoadBalance(LoadBalance loadBalance) {
        this.loadBalance = loadBalance;
    }

    public void setProtocolFactorySelector(ProtocolFactorySelector protocolFactorySelector) {
        this.protocolFactorySelector = protocolFactorySelector;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public void setFlowController(FlowController flowController) {
        this.flowController = flowController;
    }
}
