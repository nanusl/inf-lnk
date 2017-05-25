package com.ly.fn.inf.lnk.api.lookup;

import com.ly.fn.inf.lnk.api.Address;
import com.ly.fn.inf.lnk.api.Module;
import com.ly.fn.inf.lnk.api.cluster.LoadBalance;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午2:22:17
 */
public interface LookupModule extends Module {
    Address lookup(String serviceGroup, String serviceId, int version, int protocol, String loadBalanceFactor, LoadBalance loadBalance);
}
