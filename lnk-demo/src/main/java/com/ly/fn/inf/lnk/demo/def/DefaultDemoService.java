package com.ly.fn.inf.lnk.demo.def;

import com.ly.fn.inf.lnk.demo.BasicService;
import com.ly.fn.inf.lnk.demo.DemoService;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月24日 下午8:53:29
 */
public class DefaultDemoService extends BasicService implements DemoService {

    @Override
    public String demo(String name) {
        return "I'm " + name;
    }
}
