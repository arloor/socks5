
package com.arloor.socks5.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.LinkedHashSet;

public final class ClientBootStrap {

    private static Logger logger = LoggerFactory.getLogger(ClientBootStrap.class.getSimpleName());

    private static int localPort = 1080;

    public static int use = -1;
    public static int SpeedLimitKB = 0;
    public static String user;
    public static String pass;
    public static boolean auth;
    public static boolean passDomain = true;
    public static boolean passIPv4 = true;
    public static boolean passIPv6 = false;

    public static LinkedHashSet<Socks5AddressType> blockedAddressType=new LinkedHashSet<>();

    public static JSONArray servers;


    public static void initConfig(String[] args) throws IOException {
        JSONObject config = null;
        if (args.length == 2 && args[0].equals("-c")) {
            File file = new File(args[1]);
            System.out.println("config @" + file.getAbsolutePath());
            if (!file.exists()) {
                System.out.println("Error: the config file not exists");
                System.exit(-1);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Files.copy(file.toPath(), outputStream);
            config = JSON.parseObject(outputStream.toString());
            outputStream.close();
        } else {
            //        读取jar中resources下的sogo.json
            System.out.println("config @classpath:client.json");
            BufferedReader in = new BufferedReader(new InputStreamReader(ClientBootStrap.class.getClassLoader().getResourceAsStream("client.json")));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while ((line = in.readLine()) != null) {
                buffer.append(line);
            }
            String input = buffer.toString();
            config = JSON.parseObject(input);
        }

        System.out.println("config : " + config);
        if (config.containsKey("SupportDomain")&&!config.getBoolean("SupportDomain"))
            blockedAddressType.add(Socks5AddressType.DOMAIN);
        if (config.containsKey("SupportIPv4")&&!config.getBoolean("SupportIPv4"))
            blockedAddressType.add(Socks5AddressType.IPv4);
        if (config.containsKey("SupportIPv6")&&!config.getBoolean("SupportIPv6"))
            blockedAddressType.add(Socks5AddressType.IPv6);
        localPort = config.getInteger("ClientPort");
        user = config.getString("User");
        SpeedLimitKB = config.getInteger("SpeedLimitKB");
        pass = config.getString("Pass");
        auth = config.getBoolean("Auth");
        use = config.getInteger("Use");
        servers = config.getJSONArray("Servers");

        System.out.println();
        System.out.println();
    }

    public static void printUsage() {
        System.out.println("> Usage: java -jar xxx.jar [-c client.json]");
        System.out.println("> if \"client.json\" path is not set, it will the default client.json in classpath");
        System.out.println("> which listen on 6666;and connect to ss5-server:80");
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        printUsage();
        initConfig(args);
        System.out.println("=========================START Client!=============================");

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
