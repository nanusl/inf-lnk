package com.ly.fn.inf.lnk.cluster;

import java.util.List;
import java.util.Random;

import com.ly.fn.inf.lnk.api.Address;
import com.ly.fn.inf.lnk.api.cluster.LoadBalance;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月24日 下午9:02:00
 */
public class RandomLoadBalance implements LoadBalance {

    @Override
    public Address select(String loadBalanceFactor, String[] candidates) {
        int candidatesNum = candidates.length;
        Random random = new Random();
        int randomNum = random.nextInt(candidatesNum);
        return new Address(candidates[randomNum]);
    }

    @Override
    public Address select(String loadBalanceFactor, List<String> candidates) {
        int candidatesNum = candidates.size();
        Random random = new Random();
        int randomNum = random.nextInt(candidatesNum);
        return new Address(candidates.get(randomNum));
    }
}
