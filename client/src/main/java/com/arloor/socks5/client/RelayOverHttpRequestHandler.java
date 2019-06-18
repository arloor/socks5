/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.arloor.socks5.client;

import com.arloor.socks5.common.MyBase64;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

public final class RelayOverHttpRequestHandler extends ChannelOutboundHandlerAdapter {

    private final static String fakeHost="qtgwuehaoisdhuaishdaisuhdasiuhlassjd.com";

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
            content.forEachByte(value -> {
                buf.writeByte((byte)~value);
                return true;
            });
            ctx.writeAndFlush(buf,promise);
            ReferenceCountUtil.release(content);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }


}
