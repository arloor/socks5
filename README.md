# socks5

这是一个新的、使用netty实现的、使用http协议进行混淆的socks5代理。解决了前作[sogo](https://github.com/arloor/sogo)项目因go内存回收机制/内存复用机制弱带来的cpu/内存占用高的问题。<!--more-->

## 项目简介

项目地址：[https://github.com/arloor/socks5](https://github.com/arloor/socks5)

该socks5代理分为ss5-local和ss5-server。ss5-local放置在国内，ss5-server放置在国外，local与server之间伪装成http协议通信。该项目完全使用netty实现，有以下特点：

- 使用http协议进行混淆（能干什么你们懂的）
- 单端口多用户
- 密码认证
- 高性能（日常使用得出的结论，未做性能测试）
- 客户端限速（使用SpeedLimitKB字段）

目前这个socks5代理，已经在某生产环境下使用较长时间，可靠性与性能是经过考验的。各位朋友可以踩踩坑，看看是否符合自己的需求。

## 安装说明

我比较喜欢centos7。以下以centos7为例，介绍怎么安装部署ss5-local和ss5-server。

1、安装jdk

```shell
wget  -O  jdk-8u131-linux-x64.rpm --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u131-b11/d54c1d3a095b4ff2b6607d096fa80163/jdk-8u131-linux-x64.rpm
rpm -ivh jdk-8u131-linux-x64.rpm
```

2、 ss5-local部署与运行

```shell
#! /bin/bash

# 请自行安装jdk8

sudo su
mkdir /opt/socks5
cd /opt/socks5
wget -O client.jar https://github.com/arloor/socks5/releases/download/latest/client-3.0-jar-with-dependencies.jar

mkdir /etc/socks5
cat > /etc/socks5/client.json <<EOF
{
  "ClientPort": 6666,
  "SpeedLimitKB": 0,
  "Use": 0,
  "Servers": [
    {
      "ProxyAddr": "ss5-server",
      "ProxyPort": 80,
      "UserName": "a",
      "Password": "b"
    },
    {
      "ProxyAddr": "localhost",
      "ProxyPort": 80,
      "UserName": "a",
      "Password": "b"
    }
  ],
  "User": "this",
  "Pass": "socks5",
  "Auth":false
}
EOF

#创建service
cat > /lib/systemd/system/ss5-local.service <<EOF
[Unit]
Description=socks5代理-客户端ARLOOR
After=network-online.target
Wants=network-online.target

[Service]
WorkingDirectory=/opt/socks5
ExecStart=/usr/bin/java -jar /opt/socks5/client.jar -c /etc/socks5/client.json
LimitNOFILE=100000
Restart=always
RestartSec=2

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable ss5-local
# 编辑配置文件
# vim /etc/socks5/client.json
# service ss5-local start
```

3、 ss5-server部署与运行

```shell
#! /bin/bash

# 请自行安装jdk8

sudo su
mkdir /opt/socks5
cd /opt/socks5
wget -O server.jar https://github.com/arloor/socks5/releases/download/latest/server-3.0-jar-with-dependencies.jar

mkdir /etc/socks5
cat > /etc/socks5/server.json <<EOF
{
  "ServerPort": 80,
  "RedirctAddr": "www.grove.co.uk:80",
  "Users": [
    {
      "UserName": "a",
      "Password": "b"
    }
  ],
  "Dev":true
}
EOF

#创建service
cat > /lib/systemd/system/ss5-server.service <<EOF
[Unit]
Description=socks5代理-服务端ARLOOR
After=network-online.target
Wants=network-online.target

[Service]
WorkingDirectory=/opt/socks5
ExecStart=/usr/bin/java -jar /opt/socks5/server.jar -c /etc/socks5/server.json
LimitNOFILE=100000
Restart=always
RestartSec=2

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable ss5-server
service ss5-server start
```

## 更新client.jar

```
cd /opt/socks5
wget -O client.jar https://github.com/arloor/socks5/releases/download/latest/client-3.0-jar-with-dependencies.jar
service ss5-local restart
```

## 更新server.jar

```
cd /opt/socks5
wget -O server.jar https://github.com/arloor/socks5/releases/download/latest/server-3.0-jar-with-dependencies.jar
service ss5-server restart
```