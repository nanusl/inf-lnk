package com.ly.fn.inf.lnk.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.ly.fn.inf.lnk.api.InvokeType;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午9:19:37
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface LnkMethod {
    public long timeoutMillis() default 3000L;
    public InvokeType type() default InvokeType.SYNC;
}
