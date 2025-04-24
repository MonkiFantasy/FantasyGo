#!/bin/bash
# 运行Go游戏客户端的脚本

echo "启动奇弈围棋客户端..."

# 设置正确的类路径
CLASSPATH="target/classes:lib/gson-2.10.1.jar"

# 如果classpath.txt存在，添加其内容
if [ -f "classpath.txt" ]; then
  EXTRA_CLASSPATH=$(cat classpath.txt)
  CLASSPATH="$CLASSPATH:$EXTRA_CLASSPATH"
fi

# 打印类路径用于调试
echo "使用的类路径: $CLASSPATH"

# 运行主程序
java -cp "$CLASSPATH" com.monki.Main

echo "客户端已退出" 