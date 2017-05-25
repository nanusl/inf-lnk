package com.ly.fn.inf.lnk.lookup.zk;

import java.util.List;

import com.ly.fn.inf.lnk.api.Address;
import com.ly.fn.inf.lnk.api.cluster.LoadBalance;

public class RegistryModuleTest {
    public static void main(String[] argv) {
        ZokLookupRegistryModule module = new ZokLookupRegistryModule("10.100.157.28:2181");
        Address addr = new Address("127.0.0.1:8081");
        module.registry("test-group", "RegistryModuleTest", 0, 0, addr);
        module.lookup("test-group", "RegistryModuleTest", 0, 0, "123", new LoadBalance() {

            @Override
            public Address select(String loadBalanceFactor, String[] candidates) {
                return null;
            }

            @Override
            public Address select(String loadBalanceFactor, List<String> candidates) {
                return null;
            }
        });
        module.unregistry("test-group", "RegistryModuleTest", 0, 0);
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            module.dumpCachedServers();
        }

    }
}
