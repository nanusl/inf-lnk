package com.ly.fn.inf.lnk.cluster;

import java.util.List;

import com.ly.fn.inf.lnk.api.Address;
import com.ly.fn.inf.lnk.api.cluster.LoadBalance;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月24日 下午9:01:34
 */
public class ConsistencyHashLoadBalance implements LoadBalance {

    @Override
    public Address select(String loadBalanceFactor, String[] candidates) {
        int index = Math.abs(loadBalanceFactor.hashCode()) % candidates.length;
        return new Address(candidates[index]);
    }

    @Override
    public Address select(String loadBalanceFactor, List<String> candidates) {
        int index = Math.abs(loadBalanceFactor.hashCode()) % candidates.size();
        return new Address(candidates.get(index));
    }
}
