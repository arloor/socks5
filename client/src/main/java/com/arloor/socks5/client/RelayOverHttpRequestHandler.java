
package com.arloor.socks5.client;

import com.arloor.socks5.common.ExceptionUtil;
import com.arloor.socks5.common.MyBase64;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RelayOverHttpRequestHandler extends ChannelOutboundHandlerAdapter {

    private final static String fakeHost="qtgwuehaoisdhuaishdaisuhdasiuhlassjd.com";
    private static Logger logger = LoggerFactory.getLogger(RelayOverHttpRequestHandler.class.getSimpleName());
    private String targetAddr;
    private int targetPort;
    private final String basicAuth;

    public RelayOverHttpRequestHandler( String targetAddr, int targetPort, String basicAuth) {
        this.targetAddr=targetAddr;
        this.targetPort=targetPort;
        this.basicAuth=basicAuth;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf content=(ByteBuf)msg;
        if(content.readableBytes()==0){
            ctx.writeAndFlush(content,promise);
        }else {
            ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer();
            buf.writeBytes("POST /target?at=".getBytes());
            buf.writeBytes(MyBase64.encode((targetAddr+":"+targetPort).getBytes()));
            buf.writeBytes((" HTTP/1.1\r\nHost: " + fakeHost + "\r\nAuthorization: Basic " + basicAuth + "\r\nAccept: */*\r\nContent-Type: text/plain\r\naccept-encoding: gzip, deflate\r\ncontent-length: ").getBytes());
            buf.writeBytes(String.valueOf(content.readableBytes()).getBytes());
            buf.writeBytes("\r\n\r\n".getBytes());
            while(content.isReadable()){
                buf.writeByte(~content.readByte());
            }
            ctx.writeAndFlush(buf,promise).addListener(future -> {
                if(!future.isSuccess()){
                    logger.warn(ExceptionUtil.getMessage(future.cause()));
                    ctx.close();
                }
            });
            ReferenceCountUtil.release(content);
        }
    }


}
