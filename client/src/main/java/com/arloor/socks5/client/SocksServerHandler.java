
package com.arloor.socks5.client;

import com.arloor.socks5.common.SocketChannelUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public final class SocksServerHandler extends SimpleChannelInboundHandler<SocksMessage> {
    private static Logger logger= LoggerFactory.getLogger(SocksServerHandler.class);

    public static final SocksServerHandler INSTANCE = new SocksServerHandler();

    private SocksServerHandler() { }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, SocksMessage socksRequest) throws Exception {
        switch (socksRequest.version()) {
            case SOCKS4a:
                //不处理sock4,直接关闭channel
                logger.warn("socks4 request from"+ctx.channel().remoteAddress());
                ctx.close();
                break;
            case SOCKS5:
                if (socksRequest instanceof Socks5InitialRequest) {
                    if(ClientBootStrap.auth){//需要密码认证
                        ctx.pipeline().addFirst(new Socks5PasswordAuthRequestDecoder());
                        ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.PASSWORD));
                    }else{//不需要密码认证
                        ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
                        ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
                    }
                } else if (socksRequest instanceof Socks5PasswordAuthRequest) {

                    Socks5PasswordAuthRequest authRequest=(Socks5PasswordAuthRequest) socksRequest;
                    if(authRequest.username().equals(ClientBootStrap.user)&&authRequest.password().equals(ClientBootStrap.pass)){
                        ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
                        ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
                    }else{
                        logger.warn("Error auth from "+ctx.channel().remoteAddress()+" === "+authRequest.username()+"/"+authRequest.password());
                        ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE));
                        SocketChannelUtils.closeOnFlush(ctx.channel());
                    }
                } else if (socksRequest instanceof Socks5CommandRequest) {
                    Socks5CommandRequest socks5CmdRequest = (Socks5CommandRequest) socksRequest;
                    if (socks5CmdRequest.type() == Socks5CommandType.CONNECT) {
                        ctx.pipeline().addLast(new SocksServerConnectHandler());
                        ctx.pipeline().remove(this);
                        ctx.fireChannelRead(socksRequest);
                    } else {
                        ctx.close();
                    }
                } else {
                    ctx.close();
                }
                break;
            case UNKNOWN:
                ctx.close();
                break;
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
        SocketChannelUtils.closeOnFlush(ctx.channel());
    }
}
