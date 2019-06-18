package com.arloor.socks5.server;

import io.netty.buffer.ByteBuf;

public class Request {
    private String host;
    private int port;
    private ByteBuf payload;

    public Request(String host, int port, ByteBuf payload) {
        this.host = host;
        this.port = port;
        this.payload = payload;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ByteBuf getPayload() {
        return payload;
    }

    public void setPayload(ByteBuf payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        byte[] bytes = new byte[payload.readableBytes()];
        payload.markReaderIndex();
        payload.readBytes(bytes);
        payload.resetReaderIndex();
        String payloadStr=new String(bytes);

        return "Request{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", payload=" + payload +
                "}\n"+payloadStr+"结束\n";
    }
}
