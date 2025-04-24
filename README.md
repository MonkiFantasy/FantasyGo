# 奇弈围棋 - 网络对弈系统升级

## 项目概述

奇弈围棋是一个基于Java开发的围棋游戏程序，支持单机对弈和网络对弈。本次升级优化了网络对弈模式，采用中央服务器架构替代了原来的点对点连接方式。

## 新的网络架构

### 之前的架构

之前的网络架构采用点对点连接方式：
- 一方作为服务器，一方作为客户端
- 需要手动输入IP地址和端口号
- 存在NAT穿透问题，连接成功率低
- 代码维护复杂，要同时处理服务端和客户端逻辑

### 新的架构

新的网络架构采用中央服务器模式：
- Rust编写的高性能中央服务器
- 所有玩家都作为客户端连接到服务器
- 服务器处理房间匹配和游戏状态同步
- 不需要手动输入IP和端口，只需房间ID
- 更好的安全性和可扩展性

## 项目结构

- `src/main/java`: Java客户端源代码
  - `com.monki.draw`: 界面相关代码
  - `com.monki.socket`: 网络通信代码
  - `com.monki.entity`: 游戏实体类
  - `com.monki.core`: 游戏核心逻辑
  - `com.monki.util`: 工具类
- `server/`: Rust服务器源代码
  - `src/main.rs`: 服务器主程序
  - `Cargo.toml`: Rust项目配置文件

## 运行项目

### 编译和运行Java客户端

```bash
# 编译
javac -d target/classes @sources.txt

# 运行
java -cp target/classes com.monki.Main
```

### 编译和运行Rust服务器

```bash
cd server
cargo run --release
```

## 网络通信协议

客户端和服务器通过JSON消息进行通信，每个消息包含以下字段：
- `type`: 消息类型 (JOIN, CREATE_ROOM, JOIN_ROOM, MOVE, GAME_STATE)
- `stone`: 落子信息 (仅用于MOVE消息)
- `roomId`: 房间ID (用于房间相关操作)

## 部署指南

### 服务器部署

1. 在公网服务器上安装Rust
2. 克隆项目代码
3. 编译服务器: `cargo build --release`
4. 运行服务器: `./target/release/go_server`
5. 确保8080端口对外开放

### 客户端配置

修改 `src/main/java/com/monki/util/Config.java` 中的 SERVER_ADDRESS 为公网服务器IP地址。

## 主要改进

1. 全新的客户端-服务器架构
2. 统一的客户端代码，简化维护
3. 高性能Rust服务器，支持大量并发连接
4. 简化的用户连接流程，提升用户体验
5. 房间管理系统，支持创建和加入房间

## 未来计划

- 添加用户认证系统
- 实现排名和积分系统
- 添加观战功能
- 实现对局记录和回放功能

