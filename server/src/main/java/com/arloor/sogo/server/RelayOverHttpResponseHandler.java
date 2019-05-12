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
package com.arloor.sogo.server;

import com.arloor.sogo.common.MyBase64;
import com.arloor.sogo.common.SocketChannelUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public final class RelayOverHttpResponseHandler extends ChannelInboundHandlerAdapter {

    private final static String fakeHost="qtgwuehaoisdhuaishdaisuhdasiuhlassjd.com";

    private String targetAddr;
    private int targetPort;
    private final Channel relayChannel;

    public RelayOverHttpResponseHandler(Channel relayChannel) {
        this.relayChannel = relayChannel;
        this.targetAddr="";
        this.targetPort=10;
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        boolean canWrite = ctx.channel().isWritable();
        //流量控制，不允许继续读
        relayChannel.config().setAutoRead(canWrite);
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        //"HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: "+strconv.Itoa(len(buf))+"\r\n\r\n"
        if (relayChannel.isActive()) {
            ByteBuf content=(ByteBuf)msg;
            ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer();
            buf.writeBytes("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: ".getBytes());
            buf.writeBytes(String.valueOf(content.readableBytes()).getBytes());
            buf.writeBytes("\r\n\r\n".getBytes());
            content.forEachByte(value -> {
                buf.writeByte(~value);
                return true;
            });
            relayChannel.writeAndFlush(buf);
            ReferenceCountUtil.release(content);
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println(ctx.channel().remoteAddress()+"关闭连接");
        if (relayChannel.isActive()) {
            SocketChannelUtils.closeOnFlush(relayChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }


}
