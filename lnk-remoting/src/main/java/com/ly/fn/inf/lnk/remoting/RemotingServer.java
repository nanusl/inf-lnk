package com.ly.fn.inf.lnk.remoting;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import com.ly.fn.inf.lnk.remoting.exception.RemotingSendRequestException;
import com.ly.fn.inf.lnk.remoting.exception.RemotingTimeoutException;
import com.ly.fn.inf.lnk.remoting.netty.NettyCommandProcessor;
import com.ly.fn.inf.lnk.remoting.protocol.RemotingCommand;

import io.netty.channel.Channel;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月18日 下午9:39:14
 */
public interface RemotingServer extends RemotingService {

    RemotingCommand invokeSync(final Channel channel, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException;

    void invokeAsync(final Channel channel, final RemotingCommand request, final long timeoutMillis, final RemotingCallback callback) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException;
    
    void invokeOneway(final Channel channel, final RemotingCommand request) throws InterruptedException, RemotingSendRequestException;

    Pair<NettyCommandProcessor, ExecutorService> getProcessorPair(int commandCode);

    void registerProcessor(int commandCode, NettyCommandProcessor processor, ExecutorService executor);

    void registerDefaultProcessor(NettyCommandProcessor processor, ExecutorService executor);

    Channel getServerChannel();
    
    InetSocketAddress getServerAddress();
}
