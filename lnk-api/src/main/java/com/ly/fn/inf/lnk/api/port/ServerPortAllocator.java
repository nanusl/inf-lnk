package com.ly.fn.inf.lnk.api.port;

import com.ly.fn.inf.lnk.api.Application;
import com.ly.fn.inf.lnk.api.Module;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午4:15:11
 */
public interface ServerPortAllocator extends Module {
    int selectPort(int expectListenPort, Application application);
}
