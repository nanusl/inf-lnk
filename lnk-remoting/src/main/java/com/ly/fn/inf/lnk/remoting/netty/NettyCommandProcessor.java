package com.ly.fn.inf.lnk.remoting.netty;

import com.ly.fn.inf.lnk.remoting.protocol.RemotingCommand;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月19日 下午9:14:07
 */
public interface NettyCommandProcessor {
    RemotingCommand processCommand(ChannelHandlerContext ctx, RemotingCommand request) throws Throwable;
    boolean tryAcquireFailure(long timeoutMillis);
    void release();
}
