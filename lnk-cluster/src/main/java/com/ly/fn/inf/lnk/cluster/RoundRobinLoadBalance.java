package com.ly.fn.inf.lnk.cluster;

import java.util.List;

import com.ly.fn.inf.lnk.api.Address;
import com.ly.fn.inf.lnk.api.cluster.LoadBalance;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月24日 下午9:05:46
 */
public class RoundRobinLoadBalance implements LoadBalance {
    private static Integer index = 0;

    @Override
    public Address select(String loadBalanceFactor, String[] candidates) {
        String candidate;
        synchronized (index) {
            if (index >= candidates.length) {
                index = 0;
            }
            candidate = candidates[index];
            index++;
        }
        return new Address(candidate);
    }

    @Override
    public Address select(String loadBalanceFactor, List<String> candidates) {
        String candidate;
        synchronized (index) {
            if (index >= candidates.size()) {
                index = 0;
            }
            candidate = candidates.get(index);
            index++;
        }
        return new Address(candidate);
    }

}
