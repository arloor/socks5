package com.arloor.sogo.server;

import io.netty.buffer.ByteBuf;

public class SoutBytebuf {
    public static void print(ByteBuf content){
        byte[] bytes = new byte[content.readableBytes()];
        content.markReaderIndex();
        content.readBytes(bytes);
        content.resetReaderIndex();
        String payloadStr = new String(bytes);
        System.out.println(payloadStr);
    }
}
