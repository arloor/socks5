package com.arloor.sogo.server;

import com.arloor.sogo.common.RelayHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class InitHanler extends ChannelInboundHandlerAdapter {
    private SocketChannel remoteChannel;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
       if(msg instanceof Request){
           Request request=(Request)msg;
           if(request.getPayload()!=null&&request.getPayload().readableBytes()==4
                   &&request.getPayload().readByte()=='i'
                   &&request.getPayload().readByte()=='n'
                   &&request.getPayload().readByte()=='i'
                   &&request.getPayload().readByte()=='t'
           ){
               Bootstrap bootstrap = new Bootstrap();
               bootstrap.group(SogoServerBootstrap.workerGroup)
                       .channel(NioSocketChannel.class)
                       .option(ChannelOption.TCP_NODELAY, true)
                       .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                       .handler(new ChannelInitializer<SocketChannel>() {
                           @Override
                           protected void initChannel(SocketChannel ch) throws Exception {
                               remoteChannel=ch;
//                               ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                               ch.pipeline().addLast(new RelayHandler(ctx.channel()));
                           }
                       });
               ChannelFuture future = bootstrap.connect(request.getHost(), request.getPort());
               future.addListener(future1 -> {
                   if (future1.isSuccess()){
                       ctx.pipeline().remove(InitHanler.class);
                       ctx.pipeline().addLast(new RelayPayloadHandler(remoteChannel));
                       System.out.println(ctx.pipeline().names());
                       ctx.channel().writeAndFlush(Unpooled.wrappedBuffer("check".getBytes()));
                   }else {
                       ctx.close();
                   }
               });

           }
       }
    }
}
