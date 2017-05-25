package com.ly.fn.inf.lnk.remoting.netty.codec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ly.fn.inf.lnk.remoting.netty.NettySystemConfigurator;
import com.ly.fn.inf.lnk.remoting.protocol.RemotingCommand;
import com.ly.fn.inf.lnk.remoting.utils.RemotingUtils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月18日 上午11:57:41
 */
public class ProtocolDecoder extends LengthFieldBasedFrameDecoder {
    private static final Logger log = LoggerFactory.getLogger(ProtocolDecoder.class.getSimpleName());
    private static final int FRAME_MAX_LENGTH = Integer.getInteger(NettySystemConfigurator.IO_REMOTING_FRAME_MAXLENGTH, (Integer.MAX_VALUE - RemotingCommand.COMMAND_HEADER_LENGTH));

    public ProtocolDecoder() {
        super(FRAME_MAX_LENGTH, (RemotingCommand.COMMAND_HEADER_LENGTH - RemotingCommand.HEADER_BODY_LENGTH), RemotingCommand.HEADER_BODY_LENGTH, 0, 0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = null;
        try {
            frame = (ByteBuf) super.decode(ctx, in);
            if (frame == null) {
                return null;
            }
            return RemotingCommand.decode(frame.nioBuffer());
        } catch (Throwable e) {
            log.error("decode ByteBuf to RemotingCommand Error, RemoteAddr : " + RemotingUtils.parseChannelRemoteAddr(ctx.channel()), e);
            RemotingUtils.closeChannel(ctx.channel());
        } finally {
            if (frame != null) {
                frame.release();
            }
        }
        return null;
    }
}
