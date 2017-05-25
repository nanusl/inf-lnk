package com.ly.fn.inf.lnk.core;

import com.ly.fn.inf.lnk.api.ServiceGroup;
import com.ly.fn.inf.lnk.api.exception.InvokerException;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午12:56:14
 */
public interface InvokerServer {
    void registry(String serviceGroup, String serviceId, int version, int protocol, Object bean) throws InvokerException;
    void unregistry(String serviceGroup, String serviceId, int version, int protocol) throws InvokerException;
    void bind(ServiceGroup... serviceGroups);
    void start();
    void shutdown();
}
