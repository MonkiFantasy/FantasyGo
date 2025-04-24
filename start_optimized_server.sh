#!/bin/bash
# 启动优化版围棋服务器

echo "编译优化版围棋服务器..."
cd server
cargo build --release

echo "设置系统网络参数..."
# 需要root权限，如果可能的话
if [ "$EUID" -eq 0 ]; then
  # 提高本地网络性能
  sysctl -w net.ipv4.tcp_fastopen=3
  sysctl -w net.core.somaxconn=1024
  sysctl -w net.ipv4.tcp_max_syn_backlog=1024
  sysctl -w net.ipv4.tcp_fin_timeout=15
  sysctl -w net.ipv4.tcp_keepalive_time=300
  sysctl -w net.ipv4.tcp_keepalive_intvl=30
  sysctl -w net.ipv4.tcp_keepalive_probes=3
  sysctl -w net.ipv4.tcp_tw_reuse=1
  
  echo "系统网络参数优化完成"
else
  echo "无root权限，跳过系统网络参数优化"
fi

echo "启动优化版围棋服务器..."
cd target/release
RUST_LOG=info ./go_server

echo "服务器已启动，监听端口 11434" 