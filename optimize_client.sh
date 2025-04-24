#!/bin/bash
# 围棋客户端极速优化脚本

# 查找并替换 GoClient.java 中的sleep时间
echo "优化 GoClient.java 中的睡眠时间..."
sed -i 's/Thread.sleep(HEARTBEAT_INTERVAL);/Thread.sleep(1000); \/\/ 极低延迟心跳间隔/g' src/main/java/com/monki/socket/GoClient.java
sed -i 's/private static final long HEARTBEAT_INTERVAL = [0-9]\+;/private static final long HEARTBEAT_INTERVAL = 1000; \/\/ 心跳间隔改为1秒，极低延迟/g' src/main/java/com/monki/socket/GoClient.java
sed -i 's/Thread.sleep(1000);/Thread.sleep(100);/g' src/main/java/com/monki/socket/GoClient.java
sed -i 's/Thread.sleep(500);/Thread.sleep(100);/g' src/main/java/com/monki/socket/GoClient.java
sed -i 's/Thread.sleep(3000);/Thread.sleep(500);/g' src/main/java/com/monki/socket/GoClient.java
sed -i 's/Thread.sleep(2000);/Thread.sleep(500);/g' src/main/java/com/monki/socket/GoClient.java
sed -i 's/socket.setSoTimeout([0-9]\+);/socket.setSoTimeout(5000);/g' src/main/java/com/monki/socket/GoClient.java

# 更改本地服务器地址
echo "设置为本地服务器地址..."
sed -i 's/SERVER_ADDRESS = ".*";/SERVER_ADDRESS = "127.0.0.1"; \/\/ 使用本地服务器，极低延迟/g' src/main/java/com/monki/util/Config.java

# 修改socket选项以提高性能
echo "优化网络连接设置..."
sed -i 's/setReceiveBufferSize(BUFFER_SIZE [^)]*);/setReceiveBufferSize(BUFFER_SIZE * 4);/g' src/main/java/com/monki/socket/GoClient.java
sed -i 's/setSendBufferSize(BUFFER_SIZE [^)]*);/setSendBufferSize(BUFFER_SIZE * 4);/g' src/main/java/com/monki/socket/GoClient.java

# 增加缓冲区大小
echo "超大缓冲区优化..."
sed -i 's/private static final int BUFFER_SIZE = [0-9]\+;/private static final int BUFFER_SIZE = 16384; \/\/ 超大缓冲区/g' src/main/java/com/monki/socket/GoClient.java

# 优化缓冲区处理方式
echo "优化流缓冲区大小..."
sed -i 's/new BufferedInputStream(socket.getInputStream(), [^)]*)/new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE * 4)/g' src/main/java/com/monki/socket/GoClient.java
sed -i 's/new BufferedOutputStream(socket.getOutputStream(), [^)]*)/new BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE * 4)/g' src/main/java/com/monki/socket/GoClient.java

# 添加性能优先选项
echo "添加额外性能选项..."
sed -i '/setTrafficClass/a\\        socket.setPerformancePreferences(1, 0, 0); \/\/ 优先考虑连接时间' src/main/java/com/monki/socket/GoClient.java
sed -i '/setPerformancePreferences/a\\        socket.setSoLinger(false, 0);   \/\/ 快速关闭' src/main/java/com/monki/socket/GoClient.java

# 优化JSON处理
echo "优化JSON序列化..."
sed -i '/GSON =/,/create();/ c\\    \/\/ 用于JSON序列化\/反序列化的GSON实例 - 极低延迟版\n    public static final Gson GSON = new GsonBuilder()\n            .serializeNulls()\n            .disableHtmlEscaping()\n            .disableInnerClassSerialization()\n            .create();' src/main/java/com/monki/util/Config.java

# 优化消息发送方法
echo "优化消息发送机制..."
sed -i '/private void sendMessage(GameMessage message, boolean logMessage)/,/synchronized \(dos\) {/ c\\    private void sendMessage(GameMessage message, boolean logMessage) throws IOException {\n        if (dos == null) {\n            throw new IOException("输出流为空");\n        }\n        \n        \/\/ 使用更紧凑的JSON序列化配置\n        String jsonMessage = Config.GSON.toJson(message);\n        byte[] messageBytes = jsonMessage.getBytes("UTF-8");\n        int messageLength = messageBytes.length;\n        \n        \/\/ 打印日志（如果需要）\n        if (logMessage && message.type != GameMessageType.Heartbeat) {\n            System.out.println("发送: " + message.type + \n                (message.type == GameMessageType.Move ? \n                " (" + message.networkStone.index.i + "," + message.networkStone.index.j + ")" : ""));\n        }\n        \n        \/\/ 使用尽可能短的同步块\n        synchronized (dos) {' src/main/java/com/monki/socket/GoClient.java

sed -i '/dos.write(baos.toByteArray());/,/}/ c\\            \/\/ 直接写入长度和内容，避免额外创建 ByteArrayOutputStream\n            dos.writeInt(messageLength);\n            dos.write(messageBytes);\n            dos.flush();\n        }' src/main/java/com/monki/socket/GoClient.java

# 给心跳线程添加高优先级
echo "优化心跳机制..."
sed -i '/heartbeatThread.setDaemon(true);/a\\        heartbeatThread.setPriority(Thread.MAX_PRIORITY);  \/\/ 设置最高优先级' src/main/java/com/monki/socket/GoClient.java

# 编译项目
echo "重新编译项目..."
mvn clean compile

echo "极速优化完成! 所有网络参数已设置为最低延迟模式"
echo "本地连接应该会有极大改善，延迟应降低到毫秒级" 