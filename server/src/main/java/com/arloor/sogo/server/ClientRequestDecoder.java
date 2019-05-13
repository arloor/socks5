package com.arloor.sogo.server;

import com.arloor.sogo.common.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.ByteProcessor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

//POST /target?at=AMeVrMaYq7NWq7eYrdPYra== HTTP/1.1
//        Host: qtgwuehaoisdhuaishdaisuhdasiuhlassjd.com
//        Authorization: Basic YTpi
//        Accept: */*
//Content-Type: text/plain
//accept-encoding: gzip, deflate
//content-length: 265

//todo:修改这个类

public class ClientRequestDecoder extends ByteToMessageDecoder {
    private static Logger logger= LoggerFactory.getLogger(ClientRequestDecoder.class);
    private int contentLength=0;
    private State state= State.START;
    private final int tempByteStoreLength=200;
    private byte[] tempByteStore=new byte[tempByteStoreLength];
    private String path;
    private String mehtod;
    private Map<String,String> headers=new HashMap<>();
    private final static String fakeHost="qtgwuehaoisdhuaishdaisuhdasiuhlassjd.com";

    private final static String head1="GET";
    private final static String head2="POST";
    private final static String end="HTTP/1.1";

    private enum State{
        START,HEADER,CRLFCRLF,CONTENT
    }
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state){
            case START:
                int index=in.forEachByte(ByteProcessor.FIND_CR);
                if(index!=-1){
                    int length=index-in.readerIndex();
                    in.readBytes(tempByteStore,0,length);
                    String initLine=new String(tempByteStore,0,length);
                    if(initLine.endsWith(end)&&(initLine.startsWith(head1)||initLine.startsWith(head2))){
                       int index1=initLine.indexOf(" ");
                       int index2=initLine.lastIndexOf(" ");
                       if(index2==index1){
                           logger.error("不是一个GET/POST请求，关闭连接！ 来自："+ctx.channel().remoteAddress());
                           SocketChannelUtils.closeOnFlush(ctx.channel());
                           return;
                       }else{
                           path=initLine.substring(index1+1,index2);
                           mehtod =initLine.substring(0,index1);
                           state=State.HEADER;
                       }
                   }else{
                       logger.error("不是一个GET/POST请求，关闭连接！ 来自："+ctx.channel().remoteAddress());
                       SocketChannelUtils.closeOnFlush(ctx.channel());
                       return;
                   }
                }else{return;}
            case HEADER:
                while(headers.get("content-length")==null){
                    int newIndex=in.forEachByte(ByteProcessor.FIND_NON_CRLF);
                    if(newIndex==-1){
                        return;
                    }
                    in.readerIndex(newIndex);
                    newIndex=in.forEachByte(ByteProcessor.FIND_CR);
                    int length=newIndex-in.readerIndex();
                    in.readBytes(tempByteStore,0,length);
                    String header=new String(tempByteStore,0,length);
                    int split=header.indexOf(": ");
                    if(split!=-1&&split<header.length()-2){
                        headers.put(header.substring(0,split),header.substring(split+2));
                    }else {
                        logger.error("这。。不符合契约，关闭 "+ctx.channel().remoteAddress());
                        SocketChannelUtils.closeOnFlush(ctx.channel());
                        return;
                    }
                }
                contentLength=Integer.parseInt(headers.get("content-length"));
                state=State.CRLFCRLF;
            case CRLFCRLF:
                int newIndex=in.forEachByte(ByteProcessor.FIND_NON_CRLF);
                if(newIndex==-1){
                    return;
                }else {
                    in.readerIndex(newIndex);
                    state=State.CONTENT;
                }
            case CONTENT:
                if(in.readableBytes()<contentLength){
                    return;
                }
                ByteBuf slice=in.readSlice(contentLength);
                ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer();
                slice.forEachByte(value -> {
                    buf.writeByte(~value);
                    return true;
                });
                headers.remove("content-length");
                state=State.START;
                if(path.startsWith("/target?at=")){
                    String hostAndPort=new String(Objects.requireNonNull(MyBase64.decode(path.substring(11).getBytes())));
                    int splitIndex=hostAndPort.indexOf(":");
                    String host=hostAndPort.substring(0,splitIndex);
                    int port=Integer.parseInt(hostAndPort.substring(splitIndex+1));
                    Request request=new Request(host,port,buf);
                    //todo
                    out.add(request);
                }
        }
    }
}
