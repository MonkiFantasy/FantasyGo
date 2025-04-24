package com.monki.socket;

import com.monki.draw.MyFrame;
import com.monki.draw.MyPanel;
import com.monki.entity.Position;
import com.monki.entity.Stone;
import com.monki.util.Config;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.UUID;
import java.io.ByteArrayOutputStream;

import javax.swing.SwingUtilities;

/**
 * 新的统一客户端类，统一连接到公网Rust服务器
 */
public class GoClient implements Runnable {
    // 使用配置文件中的服务器地址和端口
    private static final String SERVER_IP = Config.SERVER_ADDRESS;
    private static final int SERVER_PORT = Config.SERVER_PORT;
    
    public static volatile Stone currentStone;
    
    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;
    private String playerId;
    private String roomId;
    
    private static final long HEARTBEAT_INTERVAL = 1000; // 心跳间隔改为1秒，极低延迟
    private static final int BUFFER_SIZE = 16384; // 超大缓冲区
    private volatile boolean running = true;
    private boolean connected = true;
    private boolean isHost = true;
    private boolean debug = false;

    public GoClient() {
        this.playerId = UUID.randomUUID().toString();
    }
    
    /**
     * 初始化连接 - 极低延迟版
     */
    private void initConnection() throws IOException {
        socket = new Socket();
        // 设置更激进的TCP选项来最大化实时性
        socket.setTcpNoDelay(true);     // 禁用Nagle算法，提高实时性
        socket.setKeepAlive(true);      // 保持连接
        socket.setReceiveBufferSize(BUFFER_SIZE * 4); // 进一步加大接收缓冲区
        socket.setSendBufferSize(BUFFER_SIZE * 4);    // 进一步加大发送缓冲区
        socket.setSoTimeout(5000);     // 减少超时时间，更快发现连接问题
        socket.setTrafficClass(0x10);   // 设置低延迟选项
        socket.setPerformancePreferences(1, 0, 0); // 优先考虑连接时间
        socket.setSoLinger(false, 0);   // 快速关闭
        
        // 连接服务器
        socket.connect(new java.net.InetSocketAddress(SERVER_IP, SERVER_PORT), 1000);
        
        // 使用带缓冲的高性能流
        dis = new DataInputStream(new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE * 4));
        dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE * 4));
        
        System.out.println("成功连接到服务器: " + SERVER_IP + ":" + SERVER_PORT);
    }
    
    /**
     * 创建新房间
     */
    public void createRoom() {
        try {
            if (dos != null) {
                // 创建简化版的房间ID - 使用6位数字
                int roomIdNumber = (int)(Math.random() * 900000) + 100000; // 生成6位数字
                this.roomId = String.valueOf(roomIdNumber);
                
                // 在控制台打印房间ID，方便复制
                System.out.println("\n===========================");
                System.out.println("房间ID: " + roomId);
                System.out.println("===========================\n");
                
                // 显示房间ID，无需等待服务器响应
                SwingUtilities.invokeLater(() -> {
                    javax.swing.JOptionPane.showMessageDialog(
                        null, 
                        "房间创建成功！房间ID: " + roomId + "\n请将此ID提供给对方加入游戏\n(ID已在控制台显示，可直接复制)",
                        "房间ID",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE
                    );
                });
                
                // 创建消息
                GameMessage createRoomMsg = new GameMessage(
                    GameMessageType.CreateRoom,
                    null,  // 不需要Stone对象
                    roomId  // 使用本地生成的简化ID
                );
                
                sendMessage(createRoomMsg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 加入现有房间
     * @param roomId 房间ID
     */
    public void joinRoom(String roomId) {
        try {
            if (dos != null) {
                // 在控制台打印准备加入的房间ID
                System.out.println("\n===========================");
                System.out.println("正在加入房间: " + roomId);
                System.out.println("===========================\n");
                
                // 保存房间ID
                this.roomId = roomId;
                System.out.println("设置当前客户端的房间ID: " + this.roomId);
                
                // 创建消息
                GameMessage joinRoomMsg = new GameMessage(
                    GameMessageType.JoinRoom,
                    null,  // 不需要Stone对象
                    roomId
                );
                
                sendMessage(joinRoomMsg);
            }
        } catch (Exception e) {
            System.out.println("加入房间失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 发送落子消息
     * @param stone 落子信息
     */
    public void sendMove(Stone stone) {
        try {
            if (dos != null) {
                System.out.println("\n===========================");
                System.out.println("发送落子: 颜色=" + (stone.getColor().equals(Color.BLACK) ? "黑" : "白") + 
                                   ", 位置=(" + stone.getIndex().getI() + "," + stone.getIndex().getJ() + ")");
                System.out.println("===========================\n");
                
                // 创建简化版的Stone对象用于网络传输
                NetworkStone networkStone = new NetworkStone(
                    stone.getCount(),
                    stone.getColor().equals(Color.BLACK) ? "BLACK" : "WHITE",
                    new NetworkPosition(stone.getCoordinate().getI(), stone.getCoordinate().getJ()),
                    new NetworkPosition(stone.getIndex().getI(), stone.getIndex().getJ())
                );
                
                // 创建消息
                GameMessage moveMsg = new GameMessage(
                    GameMessageType.Move,
                    networkStone,
                    roomId  // 确保设置了正确的房间ID
                );
                
                // 确认房间ID不为空
                if (roomId == null || roomId.isEmpty()) {
                    System.out.println("错误: 房间ID为空，无法发送落子消息");
                    return;
                }
                
                System.out.println("发送MOVE消息到房间: " + roomId);
                System.out.println("落子详情: 手数=" + stone.getCount() + 
                                  ", 颜色=" + (stone.getColor().equals(Color.BLACK) ? "BLACK" : "WHITE") + 
                                  ", 位置=(" + stone.getIndex().getI() + "," + stone.getIndex().getJ() + ")");
                System.out.println("当前角色: " + (Config.SERVER ? "创建者(黑)" : "加入者(白)"));
                
                sendMessage(moveMsg);
            } else {
                System.out.println("错误: 输出流为空，无法发送落子消息");
            }
        } catch (Exception e) {
            System.out.println("发送落子消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 发送心跳消息，保持连接活跃
     */
    private void sendHeartbeat() {
        try {
            if (dos != null) {
                GameMessage heartbeatMsg = new GameMessage(
                    GameMessageType.Heartbeat,
                    null,
                    roomId != null ? roomId : ""
                );
                
                sendMessage(heartbeatMsg, false); // 不打印心跳日志
            }
        } catch (Exception e) {
            if (debug) System.out.println("发送心跳消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送消息到服务器（默认记录日志）
     */
    private void sendMessage(GameMessage message) throws IOException {
        sendMessage(message, true);
    }
    
    /**
     * 发送消息到服务器 - 极低延迟版
     * @param message 要发送的消息
     * @param logMessage 是否记录日志
     */
    private void sendMessage(GameMessage message, boolean logMessage) throws IOException {
        if (dos == null) {
            throw new IOException("输出流为空");
        }
        
        // 使用更紧凑的JSON序列化配置
        String jsonMessage = Config.GSON.toJson(message);
        byte[] messageBytes = jsonMessage.getBytes("UTF-8");
        int messageLength = messageBytes.length;
        
        // 打印日志（如果需要）
        if (logMessage && message.type != GameMessageType.Heartbeat) {
            System.out.println("发送: " + message.type + 
                (message.type == GameMessageType.Move ? 
                " (" + message.networkStone.index.i + "," + message.networkStone.index.j + ")" : ""));
        }
        
        // 使用尽可能短的同步块
        synchronized (dos) {
            // 直接写入长度和内容，避免额外创建 ByteArrayOutputStream
            dos.writeInt(messageLength);
            dos.write(messageBytes);
            dos.flush();
        }
    }
    
    @Override
    public void run() {
        connected = true;
        
        // 先初始化连接
        try {
            if (socket == null || socket.isClosed()) {
                System.out.println("初始化连接到服务器...");
                initConnection();
            }
        } catch (Exception e) {
            System.out.println("初始连接失败: " + e.getMessage());
            reconnect();
        }
        
        // 启动一个线程定期发送心跳
        Thread heartbeatThread = new Thread(() -> {
            while (running) {
                try {
                    if (connected) sendHeartbeat();
                    Thread.sleep(HEARTBEAT_INTERVAL); // 使用更短的心跳间隔
                } catch (Exception e) {
                    if (debug) System.out.println("心跳线程异常: " + e.getMessage());
                    if (!connected) {
                        try {
                            reconnect();
                            Thread.sleep(200); // 减少重连后的等待时间
                        } catch (Exception re) {
                            // 忽略重连异常
                        }
                    }
                }
            }
        });
        heartbeatThread.setDaemon(true); // 设为守护线程，主线程结束时自动结束
        heartbeatThread.setPriority(Thread.MAX_PRIORITY); // 设置最高优先级
        heartbeatThread.start();
        
        // 接收消息循环 - 使用极低延迟缓冲区处理
        byte[] lengthBuffer = new byte[4];
        byte[] messageBuffer = new byte[BUFFER_SIZE * 2]; // 大幅增加缓冲区大小
        
        while (running) {
            try {
                // 检查连接状态
                if (socket == null || socket.isClosed() || dis == null) {
                    if (debug) System.out.println("连接已断开，尝试重新连接...");
                    reconnect();
                    Thread.sleep(100); // 大幅减少重连等待时间
                    continue;
                }
                
                // 读取消息长度前缀 (4字节)
                if (dis.read(lengthBuffer) != 4) {
                    if (debug) System.out.println("读取消息长度不完整");
                    continue;
                }
                
                // 使用更高效的方式解析长度
                int messageLength = ((lengthBuffer[0] & 0xFF) << 24) | 
                                   ((lengthBuffer[1] & 0xFF) << 16) | 
                                   ((lengthBuffer[2] & 0xFF) << 8) | 
                                    (lengthBuffer[3] & 0xFF);
                
                // 验证消息长度
                if (messageLength <= 0 || messageLength > BUFFER_SIZE * 2) {
                    if (debug) System.out.println("无效消息长度: " + messageLength);
                    continue;
                }
                
                // 确保缓冲区足够大
                if (messageLength > messageBuffer.length) {
                    messageBuffer = new byte[messageLength];
                }
                
                // 读取消息内容 - 优化的读取方式
                int totalRead = 0;
                int remaining = messageLength;
                while (remaining > 0) {
                    int bytesRead = dis.read(messageBuffer, totalRead, remaining);
                    if (bytesRead == -1) break; // 连接已关闭
                    totalRead += bytesRead;
                    remaining -= bytesRead;
                }
                
                if (totalRead != messageLength) {
                    if (debug) System.out.println("消息不完整，跳过");
                    continue;
                }
                
                // 加速处理 - 直接传递字节数组而不是转换为字符串再解析
                try {
                    // 解析JSON消息 - 直接从字节数组解析
                    GameMessage message = Config.GSON.fromJson(
                        new String(messageBuffer, 0, messageLength, "UTF-8"), 
                        GameMessage.class
                    );
                    
                    // 处理消息 - 快速路径
                    handleMessage(message);
                } catch (Exception e) {
                    if (debug) {
                        System.out.println("消息解析错误: " + e.getMessage());
                    }
                }
                
            } catch (SocketException | EOFException e) {
                System.out.println("连接异常: " + e.getMessage());
                reconnect();
            } catch (IOException e) {
                System.out.println("IO错误: " + e.getMessage());
                reconnect();
            } catch (Exception e) {
                if (debug) {
                    System.out.println("处理消息异常: " + e.getMessage());
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(100); // 减少异常处理等待时间
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    /**
     * 尝试重新连接服务器
     */
    private void reconnect() {
        try {
            closeConnection();
            
            // 等待一段时间再尝试重连
            Thread.sleep(100); // 减少重连等待时间
            
            System.out.println("尝试重新连接服务器...");
            initConnection();
            connected = true;
            
            // 如果已加入房间，尝试重新加入
            if (roomId != null && !roomId.isEmpty()) {
                System.out.println("尝试重新加入房间: " + roomId + ", 角色: " + (isHost ? "创建者" : "加入者"));
                
                // 先等待短暂时间确保连接稳定
                Thread.sleep(100); // 减少重连后等待时间
                
                if (isHost) {
                    // 创建者尝试恢复原有房间
                    GameMessage createRoomMsg = new GameMessage(
                        GameMessageType.CreateRoom,
                        null,
                        roomId
                    );
                    sendMessage(createRoomMsg);
                    System.out.println("重新创建房间消息已发送");
                } else {
                    // 加入者尝试重新加入
                    GameMessage joinRoomMsg = new GameMessage(
                        GameMessageType.JoinRoom,
                        null,
                        roomId
                    );
                    sendMessage(joinRoomMsg);
                    System.out.println("重新加入房间消息已发送");
                }
            }
            
            System.out.println("重新连接成功");
        } catch (Exception e) {
            System.out.println("重新连接失败: " + e.getMessage());
            connected = false;
            
            // 延迟后自动重试
            new Thread(() -> {
                try {
                    Thread.sleep(500); // 减少自动重试等待时间
                    System.out.println("自动重试连接...");
                    reconnect();
                } catch (Exception re) {
                    // 忽略重试异常
                }
            }).start();
        }
    }
    
    /**
     * 清理资源
     */
    private void closeConnection() {
        try {
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            if (debug) e.printStackTrace();
        }
    }
    
    /**
     * 处理从服务器接收的消息 - 高性能版
     */
    private void handleMessage(GameMessage message) {
        // 只对移动消息输出日志，减少其他消息的输出开销
        if (message.type == GameMessageType.Move) {
            if (message.networkStone != null) {
                System.out.println("收到落子: " + 
                    message.networkStone.color + " (" + message.networkStone.index.i + "," + message.networkStone.index.j + ")");
            }
        } else if (message.type != GameMessageType.Heartbeat && debug) {
            System.out.println("处理消息: " + message.type);
        }
        
        switch (message.type) {
            case CreateRoom:
                // 接收创建房间的响应
                if (message.roomId != null && !message.roomId.isEmpty()) {
                    this.roomId = message.roomId;
                    System.out.println("\n===========================");
                    System.out.println("服务器确认房间创建成功: " + roomId);
                    System.out.println("===========================\n");
                }
                break;
                
            case JoinRoom:
                // 接收加入房间的响应
                if (message.roomId != null && !message.roomId.isEmpty()) {
                    this.roomId = message.roomId;
                    System.out.println("\n===========================");
                    System.out.println("成功加入房间: " + message.roomId);
                    System.out.println("服务器确认房间ID: " + this.roomId);
                    System.out.println("===========================\n");
                }
                break;
                
            case Move:
                // 接收落子信息
                if (message.networkStone != null) {
                    // 直接创建本地Stone对象
                    NetworkStone netStone = message.networkStone;
                    final Stone localStone = new Stone(
                        netStone.count,
                        netStone.color.equals("BLACK") ? Color.BLACK : Color.WHITE,
                        new Position(netStone.coordinate.i, netStone.coordinate.j),
                        new Position(netStone.index.i, netStone.index.j)
                    );
                    
                    // 直接更新当前棋子，不做多余判断
                    currentStone = localStone;
                    
                    // 使用Swing事件分发线程更新UI，避免等待
                    SwingUtilities.invokeLater(() -> {
                        try {
                            // 使用updateStone会自动将这个棋子设置为lastStone
                            MyPanel.updateStone(localStone);
                            // 仅当面板存在时重绘
                            if (MyFrame.myPanel != null) {
                                MyFrame.myPanel.repaint();
                            }
                        } catch (Exception e) {
                            System.out.println("更新棋盘失败: " + e.getMessage());
                        }
                    });
                }
                break;
                
            case Heartbeat:
                // 心跳消息，仅在debug模式下记录
                if (debug) System.out.println("收到心跳响应");
                break;
                
            default:
                // 忽略未知消息类型，节省处理时间
                break;
        }
    }
    
    /**
     * 停止客户端
     */
    public void stop() {
        running = false;
        cleanup();
    }
    
    /**
     * 清理所有资源
     */
    private void cleanup() {
        try {
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 消息类型枚举
     */
    public enum GameMessageType {
        Join,
        CreateRoom,
        JoinRoom,
        Move,
        GameState,
        Heartbeat  // 添加心跳消息类型
    }
    
    /**
     * 游戏消息类
     */
    public static class GameMessage {
        public GameMessageType type;
        public NetworkStone networkStone;
        public String roomId;
        
        public GameMessage(GameMessageType type, NetworkStone networkStone, String roomId) {
            this.type = type;
            this.networkStone = networkStone;
            this.roomId = roomId;
        }
    }
    
    /**
     * 简化版的Stone类，用于网络传输
     */
    public static class NetworkStone {
        public int count;
        public String color; // "BLACK" or "WHITE"
        public NetworkPosition coordinate;
        public NetworkPosition index;
        
        public NetworkStone(int count, String color, NetworkPosition coordinate, NetworkPosition index) {
            this.count = count;
            this.color = color;
            this.coordinate = coordinate;
            this.index = index;
        }
    }
    
    /**
     * 简化版的Position类，用于网络传输
     */
    public static class NetworkPosition {
        public int i;
        public int j;
        
        public NetworkPosition(int i, int j) {
            this.i = i;
            this.j = j;
        }
    }
}
