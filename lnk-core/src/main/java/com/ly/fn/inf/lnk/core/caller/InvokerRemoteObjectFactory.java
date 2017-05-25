package com.ly.fn.inf.lnk.core.caller;

import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;

import com.ly.fn.inf.lnk.api.RemoteObject;
import com.ly.fn.inf.lnk.api.RemoteObjectFactory;
import com.ly.fn.inf.lnk.core.Invoker;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月23日 下午1:33:46
 */
@SuppressWarnings("unchecked")
public class InvokerRemoteObjectFactory implements RemoteObjectFactory {
    public static final String DEFAULT_INVOKER_REMOTE_OBJECT_FACTORY = "defaultInvokerRemoteObjectFactory";
    private Invoker invoker;
    private ConcurrentHashMap<String, Object> remoteObjects = new ConcurrentHashMap<String, Object>();

    @Override
    public <T> T getRemoteObject(String serializeStub, Class<T> serviceInterface) {
        Object remoteObject = remoteObjects.get(serializeStub);
        if (remoteObject != null) {
            return (T) remoteObject;
        }
        InvokerCaller caller = new InvokerCaller(invoker, serializeStub);
        remoteObject = Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class[] {serviceInterface, RemoteObject.class}, caller);
        remoteObjects.put(serializeStub, remoteObject);
        return (T) remoteObject;
    }

    @Override
    public <T> T getRemoteObject(Class<T> serviceInterface) {
        return getRemoteObject(new RemoteStub(serviceInterface).serializeStub(), serviceInterface);
    }
    
    @Override
    public Object getRemoteObject(RemoteObject remoteObject) {
        String serviceId = ((RemoteStub) remoteObject).getServiceId();
        try {
            Class<?> serviceInterface = Class.forName(serviceId);
            return getRemoteObject(remoteObject.serializeStub(), serviceInterface);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("can't found serviceId : " + serviceId + " class.");
        }
    }
    
    public void setInvoker(Invoker invoker) {
        this.invoker = invoker;
    }
}
