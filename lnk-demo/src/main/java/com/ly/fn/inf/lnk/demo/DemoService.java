package com.ly.fn.inf.lnk.demo;

import com.ly.fn.inf.lnk.api.InvokeType;
import com.ly.fn.inf.lnk.api.annotation.LnkMethod;
import com.ly.fn.inf.lnk.api.annotation.LnkService;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月24日 下午8:52:14
 */

@LnkService(group = "biz-pay-bgw-payment.router.srv")
public interface DemoService {
    @LnkMethod(type = InvokeType.SYNC)
    String demo(String name);
}
