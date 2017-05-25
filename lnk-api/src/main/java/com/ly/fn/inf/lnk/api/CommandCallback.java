package com.ly.fn.inf.lnk.api;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月25日 下午4:40:52
 */
public interface CommandCallback<T> {
    void onComplete(T response);
    void onError(Throwable e);
}
