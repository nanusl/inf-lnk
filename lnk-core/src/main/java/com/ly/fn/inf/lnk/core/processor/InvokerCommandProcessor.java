package com.ly.fn.inf.lnk.core.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ly.fn.inf.lnk.api.Application;
import com.ly.fn.inf.lnk.api.InvokerCommand;
import com.ly.fn.inf.lnk.api.flow.FlowController;
import com.ly.fn.inf.lnk.api.track.TrackModule;
import com.ly.fn.inf.lnk.remoting.netty.NettyCommandProcessor;
import com.ly.fn.inf.lnk.remoting.protocol.ProtocolFactory;
import com.ly.fn.inf.lnk.remoting.protocol.ProtocolFactorySelector;
import com.ly.fn.inf.lnk.remoting.protocol.RemotingCommand;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午4:09:36
 */
public class InvokerCommandProcessor implements NettyCommandProcessor {
    private static final Logger log = LoggerFactory.getLogger(InvokerCommandProcessor.class.getSimpleName());
    private ProtocolFactorySelector protocolFactorySelector;
    private ServiceObjectFinder serviceObjectFinder;
    private FlowController flowController;
    private Application application;
    private TrackModule trackModule;

    @Override
    public RemotingCommand processCommand(ChannelHandlerContext ctx, RemotingCommand request) throws Throwable {
        long startMillis = System.currentTimeMillis();
        ProtocolFactory protocolFactory = protocolFactorySelector.select(request.getProtocolCode());
        InvokerCommand command = protocolFactory.decode(InvokerCommand.class, request);
        ServiceObject serviceObject = serviceObjectFinder.getServiceObject(command);
        this.track(command);
        try {
            command.setRetObject(serviceObject.invoke(command));
        } catch (Throwable e) {
            command.setT(e);
            log.error("invoke correlationId<" + command.getId() + ">, serviceId<" + command.commandSignature() + "> Error.", e);
        }
        RemotingCommand response = RemotingCommand.replyCommand(request, request.getCode());
        protocolFactory.encode(command, response);
        long endMillis = System.currentTimeMillis();
        log.info("server invoker correlationId<{}>, serviceId<{}>, used {}(ms) success.", new Object[] {command.getId(), command.commandSignature(), (endMillis - startMillis)});
        return response;
    }

    private void track(InvokerCommand command) {
        if (trackModule == null) {
            return;
        }
        try {
            trackModule.track(command, application);
        } catch (Throwable e) {
            log.error("track serviceId<" + command.commandSignature() + "> invoke on application " + application.getApp() + " Error.", e);
        }
    }

    @Override
    public boolean tryAcquireFailure(long timeoutMillis) {
        if (flowController == null) {
            return false;
        }
        return flowController.tryAcquireFailure(timeoutMillis);
    }

    @Override
    public void release() {
        if (flowController == null) {
            return;
        }
        flowController.release();
    }

    public void setProtocolFactorySelector(ProtocolFactorySelector protocolFactorySelector) {
        this.protocolFactorySelector = protocolFactorySelector;
    }

    public void setServiceObjectFinder(ServiceObjectFinder serviceObjectFinder) {
        this.serviceObjectFinder = serviceObjectFinder;
    }

    public void setFlowController(FlowController flowController) {
        this.flowController = flowController;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public void setTrackModule(TrackModule trackModule) {
        this.trackModule = trackModule;
    }
}
