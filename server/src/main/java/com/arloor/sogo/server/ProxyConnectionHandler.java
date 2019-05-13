package com.arloor.sogo.server;

import com.arloor.sogo.common.PrintAllInboundByteBufHandler;
import com.arloor.sogo.common.SocketChannelUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProxyConnectionHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(ProxyConnectionHandler.class);
    private SocketChannel remoteChannel = null;
    private SocketChannel localChannel = null;
    private ChannelFuture hostConnectFuture;


    private ByteBuf content = PooledByteBufAllocator.DEFAULT.buffer();
    private String host;
    private int port;

    public ProxyConnectionHandler(SocketChannel localChannel) {
        this.localChannel = localChannel;
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        boolean canWrite = ctx.channel().isWritable();
        logger.warn(ctx.channel() + " 可写性：" + canWrite);
        //流量控制，不允许继续读
        remoteChannel.config().setAutoRead(canWrite);
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Request) {
            Request request = ((Request) msg);
            ByteBuf buf = request.getPayload();
            host = request.getHost();
            port = request.getPort();
            content.writeBytes(buf);
            ReferenceCountUtil.release(msg);
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {


        SoutBytebuf.print(content);

        if (host != null) {//可能是空的readComplete事件
            if (remoteChannel != null) {
                content.retain();
                remoteChannel.writeAndFlush(content);
                content.clear();
            } else {
                if (hostConnectFuture == null) {//进行连接
                    hostConnectFuture = connectTarget();
                }
                hostConnectFuture.addListener(future -> {
                    if (future.isSuccess()) {
                        content.retain();
                        remoteChannel.writeAndFlush(content);
                        content.clear();
                    }
                });
            }
        }
//        super.channelReadComplete(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ReferenceCountUtil.release(content);
        if (remoteChannel != null && remoteChannel.isActive()) {
            logger.info("主动关闭：" + ctx.channel().remoteAddress() + "--被动关闭" + remoteChannel.remoteAddress());
            SocketChannelUtils.closeOnFlush(remoteChannel);
        } else {
            logger.info("主动关闭：" + ctx.channel().remoteAddress());
        }
    }

    private ChannelFuture connectTarget() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(SogoServerBootstrap.workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        remoteChannel = ch;
//                        ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                        ch.pipeline().addLast(new RelayOverHttpResponseHandler(localChannel));
                    }
                });
        ChannelFuture future = bootstrap.connect(host, port);
        future.addListener(future1 -> {
            if (future1.isSuccess()) {
                logger.info("连接成功: " + host + ":" + port + " <FROM> " + localChannel.remoteAddress());
            } else {
                logger.error("连接失败: " + host + ":" + port + " <FROM> " + localChannel.remoteAddress());
                SocketChannelUtils.closeOnFlush(localChannel);
            }
        });
        return future;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
        super.exceptionCaught(ctx, cause);
    }
}
