package com.ly.fn.inf.lnk.api.registry;

import com.ly.fn.inf.lnk.api.Address;
import com.ly.fn.inf.lnk.api.Module;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月24日 上午11:49:43
 */
public interface RegistryModule extends Module {
    void registry(String serviceGroup, String serviceId, int version, int protocol, Address addr);
    void unregistry(String serviceGroup, String serviceId, int version, int protocol);
}
