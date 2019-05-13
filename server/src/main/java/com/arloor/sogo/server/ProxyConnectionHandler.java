package com.arloor.sogo.server;

import com.arloor.sogo.common.SocketChannelUtils;
import com.arloor.sogo.common.SoutBytebuf;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProxyConnectionHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(ProxyConnectionHandler.class);
    private SocketChannel remoteChannel = null;
    private SocketChannel localChannel = null;
    private ChannelFuture hostConnectFuture;
    private String s="GET / HTTP/1.1\r\n" +
            "Host: arloor.com\r\n" +
            "Connection: keep-alive\r\n" +
            "Upgrade-Insecure-Requests: 1\r\n" +
            "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36\r\n" +
            "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3\r\n" +
            "Referer: http://arloor.com/\r\n" +
            "Accept-Encoding: gzip, deflate\r\n" +
            "Accept-Language: zh,en;q=0.9,zh-CN;q=0.8\r\n" +
            "Cookie: _ga=GA1.2.1006490496.1556092145; _gid=GA1.2.1861383052.1557043297; _gat=1\r\n\r\n";


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
        if(content.readableBytes()!=0&&host!=null){
            if(remoteChannel==null)
                connectTarget();
            else {
                content.retain();
                remoteChannel.writeAndFlush(content).addListener(future2 -> {
                    if(future2.isSuccess()){
                        logger.info("写道远程成功22222");
                    }else{
                        logger.info("写到远程失败2222222"+future2.cause());
                    }
                    content.clear();
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
                        ch.pipeline().addLast(new RelayOverHttpResponseHandler(localChannel));
                    }
                });
        ChannelFuture future = bootstrap.connect(host, port);
        future.addListener(future1 -> {
            if (future1.isSuccess()) {
                content.retain();
                remoteChannel.writeAndFlush(content).addListener(future2 -> {
                    if(future2.isSuccess()){
                        logger.info("写道远程成功1111");
                    }else{
                        logger.info("写到远程失败1111"+future2.cause());
                    }
                    content.clear();
                });
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
