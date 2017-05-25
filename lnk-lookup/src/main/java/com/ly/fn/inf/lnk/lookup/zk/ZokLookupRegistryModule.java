package com.ly.fn.inf.lnk.lookup.zk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.ly.fn.inf.lnk.api.Address;
import com.ly.fn.inf.lnk.api.cluster.LoadBalance;
import com.ly.fn.inf.lnk.api.lookup.LookupModule;
import com.ly.fn.inf.lnk.api.registry.RegistryModule;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月24日 下午9:30:16
 */
public class ZokLookupRegistryModule extends ZookeeperClient implements LookupModule, RegistryModule {
    private static final String LOOKUP_NS = "lnk/rpc/ns";
    private Map<String, List<String>> cachedServers = new ConcurrentHashMap<String, List<String>>();
    private Map<String, Integer> registryServicesFlag = new ConcurrentHashMap<String, Integer>();
    private Map<String, Set<String>> registryServices = new ConcurrentHashMap<String, Set<String>>();

    public ZokLookupRegistryModule(String zkServers) {
        super(zkServers, LOOKUP_NS);
    }

    public void dumpCachedServers() {
        for (String key : cachedServers.keySet()) {
            System.out.println("key :" + key + ", value:" + cachedServers.get(key).toString());
        }
    }

    public boolean isServiceActive(String path) {
        Integer ret = registryServicesFlag.get(path);
        if (ret == null || ret == 0) {
            return false;
        }
        return true;
    }

    @Override
    public void unregistry(String serviceGroup, String serviceId, int version, int protocol) {
        for (String key : registryServicesFlag.keySet()) {
            String path = super.createPath(serviceGroup, serviceId, version, protocol);
            if (key.startsWith(path)) {
                registryServicesFlag.put(key, 0);
            }
        }
        for (String key : registryServices.keySet()) {
            Set<String> serverSet = registryServices.get(key);
            if (serverSet != null) {
                for (String server : serverSet) {
                    String delPath = key + "/" + server;
                    try {
                        deletePath(delPath);
                    } catch (Exception e) {

                    }
                }
            }
        }
    }

    @Override
    public void registry(String serviceGroup, String serviceId, int version, int protocol, Address addr) {
        String path = super.createPath(serviceGroup, serviceId, version, protocol);
        String nodePath = path + "/" + addr.getHost() + ":" + addr.getPort();
        try {
            registryServicesFlag.put(nodePath, 1);
            register(nodePath, "");
            Set<String> serverSet = registryServices.get(path);
            if (serverSet == null) {
                serverSet = new HashSet<String>();
                registryServices.put(path, serverSet);
            }
            serverSet.add(addr.getHost() + ":" + addr.getPort());
            log.info("registry path : {} Address : {} success.", path, addr);
        } catch (Throwable e) {
            log.error("registry path : " + path + " Address : " + addr + " Error.", e);
        }
    }

    @Override
    public Address lookup(String serviceGroup, String serviceId, int version, int protocol, String loadBalanceFactor, LoadBalance loadBalance) {
        String path = super.createPath(serviceGroup, serviceId, version, protocol);
        try {
            List<String> servers = getServers(path);
            if (servers != null && !servers.isEmpty()) {
                return loadBalance.select(loadBalanceFactor, servers);
            }
        } catch (Throwable e) {
            log.warn("lookup path:{} failed.", path);
        }
        return null;
    }

    private List<String> getServers(String path) throws ZookeeperException {
        List<String> servers = cachedServers.get(path);
        if (servers == null) {
            synchronized (this) {
                if (servers == null) {
                    servers = getChildrensWithWatcher(path, new ChildListener() {
                        @Override
                        public void childChanged(String path, List<String> children) {
                            if (children == null) {
                                children = new ArrayList<String>();
                            }
                            cachedServers.put(path, children);
                        }
                    });
                    if (servers == null) {
                        cachedServers.put(path, new ArrayList<String>());
                        return null;
                    }
                    cachedServers.put(path, servers);
                }
            }
        }
        return servers;
    }
}
