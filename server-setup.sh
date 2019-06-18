#! /bin/bash

# 请自行安装jdk8

sudo su
mkdir /opt/socks5
cd /opt/socks5
wget https://github.com/arloor/socks5/releases/download/v1.0/server.jar

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