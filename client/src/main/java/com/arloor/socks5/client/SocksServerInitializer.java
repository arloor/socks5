
package com.arloor.socks5.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

import static com.arloor.socks5.client.ClientBootStrap.SpeedLimitKB;

public final class SocksServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        if(SpeedLimitKB>0){
            ch.pipeline().addLast(new ChannelTrafficShapingHandler(1024*SpeedLimitKB,0,1000));
        }
        ch.pipeline().addLast(
                new SocksPortUnificationServerHandler(),
                SocksServerHandler.INSTANCE);
    }
}
