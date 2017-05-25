package com.ly.fn.inf.lnk.api.cluster;

import java.util.List;

import com.ly.fn.inf.lnk.api.Address;
import com.ly.fn.inf.lnk.api.Module;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午2:42:57
 */
public interface LoadBalance extends Module {
    Address select(String loadBalanceFactor, String[] candidates);
    Address select(String loadBalanceFactor, List<String> candidates);
}