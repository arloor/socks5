package com.arloor.sogo.server;

import com.arloor.sogo.common.ByteArrayUtils;
import com.arloor.sogo.common.SocketChannelUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ByteProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

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

    private final static String head1="GET";
    private final static String head2="POST";
    private final static String end="HTTP/1.1";

    private enum State{
        START,HEADER,CRLFCRLF,CONTENT
    }


//    //检查响应的其实部分是否正确
//    private boolean headValid(ByteBuf slice){
//        slice.markReaderIndex();
//        slice.readBytes(headStore);
//        slice.resetReaderIndex();
//
//        for (int i = 0; i < validHead.length; i++) {
//            if(headStore[i]!=validHead[i]){
//                return false;
//            }
//        }
//        return true;
//    }



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
                            //todo
                       }

                   }else{
                       logger.error("不是一个GET/POST请求，关闭连接！ 来自："+ctx.channel().remoteAddress());
                       SocketChannelUtils.closeOnFlush(ctx.channel());
                       return;
                   }
//                    System.out.println(cs);
                }else{return;}

        }
    }
}
