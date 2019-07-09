
package com.arloor.socks5.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

public final class ServerBootstrap {
    public static final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private static Logger logger= LoggerFactory.getLogger(ServerBootstrap.class);


    private static int serverPort;
    private static String redirctAddr;
    private final static List<String> basicAuthList =new LinkedList<>();



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
            System.out.println("config @classpath:server.json");
            BufferedReader in = new BufferedReader(new InputStreamReader(ServerBootstrap.class.getClassLoader().getResourceAsStream("server.json")));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while ((line = in.readLine()) != null){
                buffer.append(line);
            }
            String input = buffer.toString();
            config= JSON.parseObject(input);
        }

        System.out.println("config : "+config);


        serverPort=config.getInteger("ServerPort");
        redirctAddr=config.getString("RedirctAddr");

        JSONArray users=config.getJSONArray("Users");
        for (int i = 0; i <users.size() ; i++) {
            JSONObject user=users.getJSONObject(i);
            String basicAuth= Base64.getEncoder().encodeToString((user.getString("UserName")+":"+user.getString("Password")).getBytes());
            basicAuthList.add(basicAuth);
        }

        System.out.println();
        System.out.println();
    }

    public static void printUsage(){
        System.out.println("> Usage: java -jar xxx.jar [-c server.json]");
        System.out.println("> if \"server.json\" path is not set, it will the default server.json in classpath");
        System.out.println("> which listen on 80; and allow user \"a\" with passwd \"b\" to connect this proxy");
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        printUsage();
        initConfig(args);
        System.out.println("=========================START Server!=============================");

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);

        try {
            io.netty.bootstrap.ServerBootstrap b = new io.netty.bootstrap.ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
//             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ServerInitializer());
            b.bind(serverPort).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
