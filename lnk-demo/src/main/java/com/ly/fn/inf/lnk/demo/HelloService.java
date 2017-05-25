package com.ly.fn.inf.lnk.demo;

import com.ly.fn.inf.lnk.api.CommandCallback;
import com.ly.fn.inf.lnk.api.InvokeType;
import com.ly.fn.inf.lnk.api.annotation.LnkMethod;
import com.ly.fn.inf.lnk.api.annotation.LnkService;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月24日 下午2:53:30
 */
@LnkService(group = "biz-pay-bgw-payment.srv")
public interface HelloService {

    @LnkMethod(type = InvokeType.SYNC)
    String welcome1(String name);

    @LnkMethod(type = InvokeType.ASYNC)
    void welcome2(String name, CommandCallback<String> callback);
}
