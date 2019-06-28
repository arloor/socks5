# socks5

## 使用指南

该socks5代理分为ss5-local和ss5-server。ss5-local放置在国内，ss5-server放置在国外，local与server之间伪装成http协议通信。该项目完全使用netty实现，有以下特点：

- 单端口多用户
- 密码认证
- 高性能（未做性能测试）
- 客户端限速（SpeedLimitKB字段限制）

## 安装说明：

以下以centos7为例，介绍怎么安装部署ss5-local和ss5-server。我比较喜欢centos7

1 安装jdk

```shell
wget  -O  jdk-8u131-linux-x64.rpm --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u131-b11/d54c1d3a095b4ff2b6607d096fa80163/jdk-8u131-linux-x64.rpm
rpm -ivh jdk-8u131-linux-x64.rpm
```

2 ss5-local部署与运行

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
  "SpeedLimitKB": 640,
  "Use": 0,
  "Servers": [
    {
      "ProxyAddr": "socks5",
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
  "Auth":true
}
EOF

#创建service
cat > /lib/systemd/system/ss5-local.service <<EOF
[Unit]
Description=socks5代理-客户端ARLOOR
After=network-online.target
Wants=network-online.target

[Service]
Restart=always
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
# vim /etc/socks5/client.json
# service ss5-local start
```

3 ss5-server部署与运行

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
Restart=always
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
