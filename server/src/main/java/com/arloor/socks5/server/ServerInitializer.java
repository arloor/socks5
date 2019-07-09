
package com.arloor.socks5.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;


public final class ServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(
                new RelayOverHttpResponseHandler(),
                new ClientRequestDecoder(),
//                new LoggingHandler(LogLevel.INFO),
                new InitHandler()
//                new ProxyConnectionHandler(ch)
        );
    }
}
