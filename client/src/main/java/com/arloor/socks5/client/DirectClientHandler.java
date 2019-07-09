
package com.arloor.socks5.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Promise;

public final class DirectClientHandler extends ChannelInboundHandlerAdapter {

    private final Promise<Channel> promise;
    private final String dstAddr;
    private final int dstPort;
    private final String basicAuth;

    public DirectClientHandler(Promise<Channel> promise, String targetAddr, int targetPort,String basicAuth) {
        this.promise = promise;
        this.dstAddr=targetAddr;
        this.dstPort=targetPort;
        this.basicAuth=basicAuth;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        //连接完毕后，增加handler：所有写操作包裹http请求
        ctx.pipeline().addLast(new RelayOverHttpRequestHandler(dstAddr,dstPort,basicAuth));
        //连接完毕后，增加handler：去除读到的http响应包裹
        ctx.pipeline().addLast(new HttpResponseDecoder());
        ctx.pipeline().addLast("check",new CheckConnectedHandler());
        byte[] raw="init".getBytes();
        ctx.channel().writeAndFlush(Unpooled.wrappedBuffer(raw));
    }

    private class CheckConnectedHandler extends SimpleChannelInboundHandler<ByteBuf> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            if(msg.readableBytes()==5
                    &&msg.readByte()=='c'
                    &&msg.readByte()=='h'
                    &&msg.readByte()=='e'
                    &&msg.readByte()=='c'
                    &&msg.readByte()=='k'
            ){
                ctx.pipeline().remove("check");
                //宣告成功
                ctx.pipeline().remove(DirectClientHandler.this);
                promise.setSuccess(ctx.channel());
            }else {
                ctx.close();
                promise.setFailure(new Throwable("Socks5 Connect Request Consum FAILED"));
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
            ctx.close();
            promise.setFailure(throwable);
        }


    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        promise.setFailure(throwable);
        ctx.close();
    }
}
