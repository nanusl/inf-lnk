package com.ly.fn.inf.lnk.lookup.consul;

import com.ly.fn.inf.lnk.api.Address;
import com.ly.fn.inf.lnk.api.cluster.LoadBalance;
import com.ly.fn.inf.lnk.api.lookup.LookupModule;
import com.ly.fn.inf.lnk.api.registry.RegistryModule;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月24日 下午9:31:42
 */
public class ConsulLookupRegistryModuleModule implements LookupModule, RegistryModule {
    private final String consulUrl;

    public ConsulLookupRegistryModuleModule(String consulUrl) {
        super();
        this.consulUrl = consulUrl;
    }

    @Override
    public void unregistry(String serviceGroup, String serviceId, int version, int protocol) {}

    @Override
    public Address lookup(String serviceGroup, String serviceId, int version, int protocol, String loadBalanceFactor, LoadBalance loadBalance) {
        return null;
    }

    @Override
    public void registry(String serviceGroup, String serviceId, int version, int protocol, Address addr) {
        
    }
}
