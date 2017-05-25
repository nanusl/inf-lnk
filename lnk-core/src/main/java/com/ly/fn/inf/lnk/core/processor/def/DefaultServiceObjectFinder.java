package com.ly.fn.inf.lnk.core.processor.def;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ReflectionUtils;

import com.ly.fn.inf.lnk.api.InvokerCommand;
import com.ly.fn.inf.lnk.api.exception.NotFoundServiceException;
import com.ly.fn.inf.lnk.core.processor.ServiceObject;
import com.ly.fn.inf.lnk.core.processor.ServiceObjectFinder;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月23日 下午4:44:10
 */
public class DefaultServiceObjectFinder implements ServiceObjectFinder, ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(DefaultServiceObjectFinder.class.getSimpleName());
    private ApplicationContext applicationContext;
    private ConcurrentHashMap<String, ServiceObject> serviceObjects = new ConcurrentHashMap<String, ServiceObject>();

    @Override
    public ServiceObject getServiceObject(InvokerCommand command) throws NotFoundServiceException {
        ServiceObject serviceObject = serviceObjects.get(command.commandSignature());
        if (serviceObject != null) {
            return serviceObject;
        }
        String serviceId = command.getServiceId();
        try {
            Class<?> serviceInterface = Class.forName(serviceId);
            Object serviceBean = applicationContext.getBean(serviceInterface);
            Method serviceMethod = ReflectionUtils.findMethod(serviceBean.getClass(), command.getMethod(), command.getSignature());
            serviceObject = new ServiceObject();
            serviceObject.setService(serviceBean);
            serviceObject.setMethod(serviceMethod);
            serviceObjects.put(command.commandSignature(), serviceObject);
            return serviceObject;
        } catch (ClassNotFoundException e) {
            log.error("load class " + serviceId + " Error.", e);
            throw new NotFoundServiceException(serviceId, e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
