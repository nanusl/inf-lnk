package com.ly.fn.inf.lnk.core.caller;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.apache.commons.lang3.ArrayUtils;

import com.ly.fn.inf.lnk.api.CommandCallback;
import com.ly.fn.inf.lnk.api.InvokeType;
import com.ly.fn.inf.lnk.api.InvokerCommand;
import com.ly.fn.inf.lnk.api.RemoteObject;
import com.ly.fn.inf.lnk.api.annotation.LnkMethod;
import com.ly.fn.inf.lnk.api.utils.CorrelationIds;
import com.ly.fn.inf.lnk.core.Invoker;
import com.ly.fn.inf.lnk.core.InvokerCallback;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午9:12:40
 */
public class InvokerCaller implements InvocationHandler {
    private final Invoker invoker;
    private final RemoteStub remoteObject;

    public InvokerCaller(Invoker invoker, RemoteStub remoteObject) {
        super();
        this.invoker = invoker;
        this.remoteObject = remoteObject;
    }

    public InvokerCaller(Invoker invoker, String serializeStub) {
        this(invoker, new RemoteStub(serializeStub));
    }

    public InvokerCaller(Invoker invoker, Class<?> serviceInterface) {
        this(invoker, new RemoteStub(serviceInterface));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass().equals(RemoteObject.class)) {
            return method.invoke(this.remoteObject, args);
        }
        if (method.getDeclaringClass().equals(Object.class)) {
            return method.invoke(this, args);
        }
        InvokeType type = InvokeType.SYNC;
        long timeoutMillis = 3000L;
        if (method.isAnnotationPresent(LnkMethod.class)) {
            LnkMethod lnkMethod = method.getAnnotation(LnkMethod.class);
            timeoutMillis = lnkMethod.timeoutMillis();
            type = lnkMethod.type();
        }
        CommandCallback<Object> callbackArg = null;
        if (method.getReturnType() == void.class) {
            callbackArg = getCommandCallback(args);
            if (callbackArg != null) {
                type = InvokeType.ASYNC;
            } else {
                type = InvokeType.ONEWAY;
            }
        }
        InvokerCommand command = new InvokerCommand();
        command.setId(CorrelationIds.buildGuid());
        command.setVersion(this.remoteObject.getVersion());
        command.setProtocolCode(this.remoteObject.getProtocolCode());
        command.setServiceGroup(this.remoteObject.getServiceGroup());
        command.setServiceId(this.remoteObject.getServiceId());
        command.setMethod(method.getName());
        command.setSignature(method.getParameterTypes());
        command.setArgs(args);
        switch (type) {
            case SYNC:
                InvokerCommand syncResponse = this.invoker.invokeSync(command, timeoutMillis);
                if (syncResponse.getT() != null) {
                    throw syncResponse.getT();
                }
                return syncResponse.getRetObject();
            case ASYNC:
                this.invoker.invokeAsync(command, timeoutMillis, new InvokerCommandCallback(callbackArg));
                break;
            case ONEWAY:
                this.invoker.invokeOneway(command);
                break;
        }
        return null;
    }
    
    private class InvokerCommandCallback implements InvokerCallback {

        private final CommandCallback<Object> callback;
        
        public InvokerCommandCallback(CommandCallback<Object> callback) {
            super();
            this.callback = callback;
        }

        @Override
        public void onComplete(InvokerCommand command) {
            if (callback != null) {
                callback.onComplete(command.getRetObject());
            }
        }

        @Override
        public void onError(Throwable e) {
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private CommandCallback<Object> getCommandCallback(Object[] args) {
        if (ArrayUtils.isEmpty(args)) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof CommandCallback) {
                return (CommandCallback<Object>) arg;
            }
        }
        return null;
    }

    public RemoteStub getRemoteObject() {
        return remoteObject;
    }
}
