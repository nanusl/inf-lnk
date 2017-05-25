package com.ly.fn.inf.lnk.port;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月25日 上午10:46:34
 */
public interface PortDetector {
    boolean isAvailable(int listenPort);
}
