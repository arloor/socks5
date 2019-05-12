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
package com.arloor.sogo.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.Base64;

public final class SogoClientBootstrap {

    private static Logger logger= LoggerFactory.getLogger(SogoClientBootstrap.class);

    private static int localPort =1080;
    public static int remotePort =80;
    public static String remoteHost;
    public static String user;
    public static String pass;
    public static String basicAuth;



    public static void initConfig(String[] args) throws IOException {
        JSONObject config=null;
        if (args.length==2&&args[0].equals("-c")){
            File file=new File(args[1]);
            System.out.println("config @"+file.getAbsolutePath());
            if(!file.exists()){
                System.out.println("Error: the config file not exists");
                System.exit(-1);
            }

            ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
            Files.copy(file.toPath(),outputStream);
            config= JSON.parseObject(outputStream.toString());
            outputStream.close();
        }else{
            //        读取jar中resources下的sogo.json
            System.out.println("config @classpath:sogo.json");
            BufferedReader in = new BufferedReader(new InputStreamReader(SogoClientBootstrap.class.getClassLoader().getResourceAsStream("sogo.json")));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while ((line = in.readLine()) != null){
                buffer.append(line);
            }
            String input = buffer.toString();
            config= JSON.parseObject(input);
        }

        System.out.println("config : "+config);


        localPort =config.getInteger("ClientPort");
        user=config.getString("User");
        pass=config.getString("Pass");
        int use=config.getInteger("Use");
        JSONObject serverInfo=config.getJSONArray("Servers").getJSONObject(use);
        remotePort=serverInfo.getInteger("ProxyPort");
        remoteHost=serverInfo.getString("ProxyAddr");
        basicAuth= Base64.getEncoder().encodeToString((serverInfo.getString("UserName")+":"+serverInfo.getString("Password")).getBytes());
        System.out.println();
        System.out.println();
    }

    public static void printUsage(){
        System.out.println("> Usage: java -jar xxx.jar [-c sogo.json]");
        System.out.println("> if \"sogo.json\" path is not set, it will the default sogo.json in classpath");
        System.out.println("> which listen on 6666;and connect to proxy:80 whith auth: youwillneveruse/thissock5");
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        printUsage();
        initConfig(args);
        System.out.println("=========================START PROXY!=============================");

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
//             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new SocksServerInitializer());
            b.bind(localPort).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
