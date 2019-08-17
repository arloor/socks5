
package com.arloor.socks5.client;

import com.alibaba.fastjson.JSONObject;
import com.arloor.socks5.common.ExceptionUtil;
import com.arloor.socks5.common.RelayHandler;
import com.arloor.socks5.common.SocketChannelUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Random;

//不可共享
//@ChannelHandler.Sharable
public final class SocksServerConnectHandler extends SimpleChannelInboundHandler<SocksMessage> {

    private static Logger logger= LoggerFactory.getLogger(SocksServerConnectHandler.class.getSimpleName());

    private  int remotePort=80;
    private  String remoteHost;
    private  String basicAuth;

    public SocksServerConnectHandler() {
        super();
        int use= ClientBootStrap.use;
        if(use==-1){
            Random rand = new Random();
            use=rand.nextInt(ClientBootStrap.servers.size());
        }
        JSONObject serverInfo= ClientBootStrap.servers.getJSONObject(use);
        remotePort=serverInfo.getInteger("ProxyPort");
        remoteHost=serverInfo.getString("ProxyAddr");
        basicAuth= Base64.getEncoder().encodeToString((serverInfo.getString("UserName")+":"+serverInfo.getString("Password")).getBytes());
    }

    private final Bootstrap b = new Bootstrap();

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final SocksMessage message) throws Exception {
        if (message instanceof Socks4CommandRequest) {
            //不处理sock4,直接关闭channel
            logger.warn("socks4 request from"+ctx.channel().remoteAddress());
            ctx.close();
        } else if (message instanceof Socks5CommandRequest) {
            final Socks5CommandRequest request = (Socks5CommandRequest) message;
            //禁止CONNECT域名和ipv6
            if(ClientBootStrap.blockedAddressType.contains(request.dstAddrType())){
                logger.warn("NOT support: "+request.dstAddr()+":"+request.dstPort()+"  <<<<<  "+ctx.channel().remoteAddress());
                ctx.close();
                return;
            }

            Promise<Channel> promise = ctx.executor().newPromise();
            promise.addListener(
                    new FutureListener<Channel>() {
                        @Override
                        public void operationComplete(final Future<Channel> future) throws Exception {
                            final Channel outboundChannel = future.getNow();
                            if (future.isSuccess()) {
                                ChannelFuture responseFuture =
                                        ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                                                Socks5CommandStatus.SUCCESS,
                                                request.dstAddrType(),
                                                request.dstAddr(),
                                                request.dstPort()));

                                responseFuture.addListener(new ChannelFutureListener() {
                                    @Override
                                    public void operationComplete(ChannelFuture channelFuture) {
                                        if(channelFuture.isSuccess()){
                                            ctx.pipeline().remove(SocksServerConnectHandler.this);
                                            outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                                            logger.info(request.dstAddr()+":"+request.dstPort()+"  <<<<<<<  "+ctx.channel().remoteAddress());
                                            ctx.pipeline().addLast(new RelayHandler(outboundChannel));
                                        }
                                    }
                                });
                            } else {
                                ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                                        Socks5CommandStatus.FAILURE, request.dstAddrType()));
                                SocketChannelUtils.closeOnFlush(ctx.channel());
                            }
                        }
                    });

            final Channel inboundChannel = ctx.channel();
            b.group(inboundChannel.eventLoop())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new DirectClientHandler(promise,request.dstAddr(),request.dstPort(),basicAuth));

            b.connect(remoteHost, remotePort).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        // Connection established use handler provided results
//                        System.out.println("连接成功");
                    } else {
                        // Close the connection if the connection attempt has failed.
                        logger.error("connect to: "+remoteHost+":"+remotePort+" failed! == "+ ExceptionUtil.getMessage(future.cause()));
                        ctx.channel().writeAndFlush(
                                new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
                        SocketChannelUtils.closeOnFlush(ctx.channel());
                    }
                }
            });
        } else {
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        SocketChannelUtils.closeOnFlush(ctx.channel());
    }
}
