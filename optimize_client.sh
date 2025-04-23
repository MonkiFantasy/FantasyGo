#!/bin/bash
# 围棋客户端优化脚本

# 查找并替换 GoClient.java 中的sleep时间
echo "优化 GoClient.java 中的睡眠时间..."
sed -i 's/Thread.sleep(HEARTBEAT_INTERVAL); \/\/ 每15秒发送一次心跳/Thread.sleep(5000); \/\/ 降低心跳间隔到5秒/g' src/main/java/com/monki/socket/GoClient.java
sed -i 's/private static final long HEARTBEAT_INTERVAL = 15000;/private static final long HEARTBEAT_INTERVAL = 5000; \/\/ 降低心跳间隔到5秒/g' src/main/java/com/monki/socket/GoClient.java
sed -i 's/Thread.sleep(1000);/Thread.sleep(300);/g' src/main/java/com/monki/socket/GoClient.java
sed -i 's/Thread.sleep(3000);/Thread.sleep(1000);/g' src/main/java/com/monki/socket/GoClient.java
sed -i 's/Thread.sleep(5000);/Thread.sleep(2000);/g' src/main/java/com/monki/socket/GoClient.java
sed -i 's/socket.setSoTimeout(30000);/socket.setSoTimeout(10000);/g' src/main/java/com/monki/socket/GoClient.java

# 修改socket选项以提高性能
echo "优化网络连接设置..."
sed -i 's/setReceiveBufferSize(BUFFER_SIZE);/setReceiveBufferSize(BUFFER_SIZE * 2);/g' src/main/java/com/monki/socket/GoClient.java
sed -i 's/setSendBufferSize(BUFFER_SIZE);/setSendBufferSize(BUFFER_SIZE * 2);/g' src/main/java/com/monki/socket/GoClient.java

# 优化缓冲区处理方式
echo "优化流缓冲区大小..."
sed -i 's/new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE)/new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE * 2)/g' src/main/java/com/monki/socket/GoClient.java
sed -i 's/new BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE)/new BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE * 2)/g' src/main/java/com/monki/socket/GoClient.java

# 添加低延迟选项
echo "添加低延迟网络选项..."
sed -i '/socket.setSoTimeout(/a\\        socket.setTrafficClass(0x10);  \/\/ 设置低延迟选项' src/main/java/com/monki/socket/GoClient.java

# 编译项目
echo "重新编译项目..."
mvn clean compile

echo "优化完成! 所有sleep时间已减少，网络参数已优化"
echo "请重新运行客户端测试性能" 