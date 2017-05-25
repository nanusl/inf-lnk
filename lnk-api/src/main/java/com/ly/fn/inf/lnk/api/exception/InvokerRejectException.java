package com.ly.fn.inf.lnk.api.exception;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午2:58:23
 */
public class InvokerRejectException extends RuntimeException {

    private static final long serialVersionUID = 4101671557585323670L;

    public InvokerRejectException(String serviceId) {
        this(serviceId, null);
    }

    public InvokerRejectException(String serviceId, Throwable cause) {
        super("invoker reject serviceId : " + serviceId, cause);
    }
}
