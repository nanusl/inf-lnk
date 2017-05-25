package com.ly.fn.inf.lnk.api.flow;

import com.ly.fn.inf.lnk.api.Module;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午6:50:00
 */
public interface FlowController extends Module {
    boolean tryAcquireFailure(long timeoutMillis);
    void release();
}
