package com.ly.fn.inf.lnk.core.processor;

import com.ly.fn.inf.lnk.api.InvokerCommand;
import com.ly.fn.inf.lnk.api.Module;
import com.ly.fn.inf.lnk.api.exception.NotFoundServiceException;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午6:26:14
 */
public interface ServiceObjectFinder extends Module {
    ServiceObject getServiceObject(InvokerCommand command) throws NotFoundServiceException;
}
