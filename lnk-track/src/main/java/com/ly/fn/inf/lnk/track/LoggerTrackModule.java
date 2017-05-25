package com.ly.fn.inf.lnk.track;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ly.fn.inf.lnk.api.Application;
import com.ly.fn.inf.lnk.api.InvokerCommand;
import com.ly.fn.inf.lnk.api.track.TrackModule;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月25日 下午2:51:17
 */
public class LoggerTrackModule implements TrackModule {
    private static final Logger log = LoggerFactory.getLogger("LnkTracker");

    @Override
    public void track(InvokerCommand command, Application application) {
        log.info("app[{}] invoke app[{}]-service[{}]", new Object[] {command.getApplication().getApp(), application.getApp(), command.commandSignature()});
    }
}
