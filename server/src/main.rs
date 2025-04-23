use std::collections::HashMap;
use std::sync::Arc;
use tokio::net::{TcpListener, TcpStream};
use tokio::sync::Mutex;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use serde::{Serialize, Deserialize};
use uuid::Uuid;
use std::io::IoSlice;
use rand::{thread_rng, Rng};

// 定义棋子结构
#[derive(Debug, Clone, Serialize, Deserialize)]
struct Stone {
    count: i32,
    color: String, // "BLACK" or "WHITE"
    coordinate: Position,
    index: Position,
}

// 定义坐标结构
#[derive(Debug, Clone, Serialize, Deserialize)]
struct Position {
    i: i32,
    j: i32,
}

// 定义消息类型
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type")]
enum GameMessage {
    Join { roomId: String },
    CreateRoom { roomId: String },
    JoinRoom { roomId: String },
    Move { networkStone: Stone },
    GameState { state: String },
    Heartbeat { roomId: String },
}

// 游戏房间结构
struct Room {
    id: String,
    player_black: Option<String>,
    player_white: Option<String>,
    moves: Vec<Stone>,
    turn: i32, // -1黑, 1白
}

// 玩家连接
struct PlayerConnection {
    id: String,
    stream: Arc<Mutex<TcpStream>>,
    room_id: Option<String>,
}

// 全局状态
struct GameServer {
    rooms: HashMap<String, Room>,
    players: HashMap<String, PlayerConnection>,
    waiting_players: Vec<String>,
    debug: bool, // 控制日志输出
}

impl GameServer {
    fn new() -> Self {
        GameServer {
            rooms: HashMap::new(),
            players: HashMap::new(),
            waiting_players: Vec::new(),
            debug: false, // 默认禁用详细日志
        }
    }

    // 创建新房间
    async fn create_room(&mut self, player_id: &str) -> String {
        let room_id = Uuid::new_v4().to_string();
        
        self.rooms.insert(
            room_id.clone(), 
            Room {
                id: room_id.clone(),
                player_black: Some(player_id.to_string()),
                player_white: None,
                moves: Vec::new(),
                turn: -1,
            }
        );
        
        if let Some(player) = self.players.get_mut(player_id) {
            player.room_id = Some(room_id.clone());
        }
        
        room_id
    }
    
    // 加入房间
    async fn join_room(&mut self, player_id: &str, room_id: &str) -> bool {
        if let Some(room) = self.rooms.get_mut(room_id) {
            if room.player_white.is_none() {
                room.player_white = Some(player_id.to_string());
                
                if let Some(player) = self.players.get_mut(player_id) {
                    player.room_id = Some(room_id.to_string());
                }
                
                return true;
            }
        }
        
        false
    }
    
    // 处理落子消息 - 优化版
    async fn process_move(&mut self, player_id: &str, stone: Stone) -> bool {
        // 简化日志，只输出必要信息
        println!("落子: {} ({},{})", stone.color, stone.index.i, stone.index.j);
        
        // 快速获取房间ID - 使用引用避免克隆
        let room_id = match self.players.get(player_id).and_then(|p| p.room_id.as_ref()) {
            Some(id) => id,
            None => return false,
        };
        
        // 获取房间引用 - 使用值引用避免HashMap查找开销
        let room = match self.rooms.get_mut(room_id) {
            Some(r) => r,
            None => return false,
        };
        
        // 记录落子 - 使用clone而不是引用
        room.moves.push(stone.clone());
        // 更换轮次
        room.turn = -room.turn;
        
        // 立即广播游戏状态
        let broadcast_result = self.broadcast_game_state(room_id, &stone).await;
        if let Err(e) = broadcast_result {
            println!("广播失败: {}", e);
        }
        
        true
    }
    
    // 广播游戏状态 - 高性能版
    async fn broadcast_game_state(&self, room_id: &str, stone: &Stone) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let room = match self.rooms.get(room_id) {
            Some(r) => r,
            None => return Err("Room not found".into()),
        };
        
        // 预先序列化消息一次 - 避免为每个玩家重复序列化
        let move_message = GameMessage::Move { 
            networkStone: stone.clone() 
        };
        let json = serde_json::to_string(&move_message)?;
        let msg_bytes = json.as_bytes();
        let len = msg_bytes.len() as u32;
        let len_bytes = len.to_be_bytes();
        
        // 并行发送消息给玩家
        let mut send_tasks = Vec::new();
        
        // 发送给黑方玩家
        if let Some(black_id) = &room.player_black {
            if let Some(player) = self.players.get(black_id) {
                let player_stream = Arc::clone(&player.stream);
                let len_bytes_clone = len_bytes.clone();
                let msg_bytes_clone = msg_bytes.to_vec();
                
                let task = tokio::spawn(async move {
                    let mut stream = player_stream.lock().await;
                    let mut bufs = [
                        IoSlice::new(&len_bytes_clone),
                        IoSlice::new(&msg_bytes_clone),
                    ];
                    let _ = stream.write_vectored(&mut bufs).await;
                });
                
                send_tasks.push(task);
            }
        }
        
        // 发送给白方玩家
        if let Some(white_id) = &room.player_white {
            if let Some(player) = self.players.get(white_id) {
                let player_stream = Arc::clone(&player.stream);
                let len_bytes_clone = len_bytes.clone();
                let msg_bytes_clone = msg_bytes.to_vec();
                
                let task = tokio::spawn(async move {
                    let mut stream = player_stream.lock().await;
                    let mut bufs = [
                        IoSlice::new(&len_bytes_clone),
                        IoSlice::new(&msg_bytes_clone),
                    ];
                    let _ = stream.write_vectored(&mut bufs).await;
                });
                
                send_tasks.push(task);
            }
        }
        
        // 等待所有发送任务完成 - 但有超时限制
        for task in send_tasks {
            match tokio::time::timeout(tokio::time::Duration::from_millis(100), task).await {
                Ok(_) => {}, // 任务正常完成
                Err(_) => {
                    // 任务超时但继续处理
                    println!("消息发送超时");
                }
            }
        }
        
        Ok(())
    }
    
    // 发送原始消息 - 新增高效发送方法
    async fn send_raw_message(&self, player: &PlayerConnection, len_bytes: &[u8], msg_bytes: &[u8]) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let mut stream = player.stream.lock().await;
        
        // 使用writev优化，避免多次系统调用
        let mut bufs = [
            IoSlice::new(len_bytes),
            IoSlice::new(msg_bytes),
        ];
        
        match stream.write_vectored(&mut bufs).await {
            Ok(_) => Ok(()),
            Err(e) => Err(e.into()),
        }
    }
    
    // 发送消息给玩家 - 优化版
    async fn send_message(&self, player: &PlayerConnection, message: GameMessage) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let json = serde_json::to_string(&message)?;
        let msg_bytes = json.as_bytes();
        let len = msg_bytes.len() as u32;
        let len_bytes = len.to_be_bytes();
        
        self.send_raw_message(player, &len_bytes, msg_bytes).await
    }
}

// 处理单个客户端连接
async fn handle_client(stream: TcpStream, server: Arc<Mutex<GameServer>>) {
    // 初始化连接
    let addr = match stream.peer_addr() {
        Ok(addr) => addr,
        Err(_) => return,
    };
    
    // 设置Socket选项提高性能
    if let Err(_) = stream.set_nodelay(true) {
        // 忽略错误继续
    }
    
    println!("新连接: {}", addr);
    
    let player_id = Uuid::new_v4().to_string();
    let stream = Arc::new(Mutex::new(stream));
    
    // 注册玩家
    {
        let mut server = server.lock().await;
        server.players.insert(player_id.clone(), PlayerConnection {
            id: player_id.clone(),
            stream: Arc::clone(&stream),
            room_id: None,
        });
    }
    
    // 预分配更大的缓冲区，减少内存分配
    let mut length_buf = [0u8; 4];
    let mut message_buf = Vec::with_capacity(8192); // 更大的预分配大小
    
    // 读取消息循环
    loop {
        // 读取消息长度前缀
        let read_result = {
            stream.lock().await.read_exact(&mut length_buf).await
        };
        
        // 检查读取结果
        if let Err(e) = read_result {
            if e.kind() != std::io::ErrorKind::UnexpectedEof {
                println!("读取消息长度错误: {:?}", e);
            }
            break;
        }
        
        // 解析消息长度
        let msg_len = u32::from_be_bytes(length_buf);
        
        // 验证消息长度是否合理
        if msg_len == 0 || msg_len > 1024 * 64 {
            // 消息长度不合理，可能是错误数据
            println!("接收到无效消息长度: {} 字节", msg_len);
            continue;
        }
        
        // 调整缓冲区大小
        message_buf.resize(msg_len as usize, 0);
        
        // 读取消息内容
        let read_result = {
            let mut stream_guard = stream.lock().await;
            stream_guard.read_exact(&mut message_buf).await
        };
        
        // 检查读取结果
        if let Err(_) = read_result {
            break;
        }
        
        // 解析JSON消息 - 使用零拷贝方式
        match serde_json::from_slice::<GameMessage>(&message_buf) {
            Ok(message) => {
                // 消息处理放入单独任务以避免阻塞主循环
                let server_clone = Arc::clone(&server);
                let player_id_clone = player_id.clone();
                let message_clone = message.clone(); // 克隆消息以便传入任务
                
                // 使用tokio::spawn创建异步任务
                tokio::spawn(async move {
                    process_client_message(server_clone, &player_id_clone, message_clone).await;
                });
            }
            Err(e) => {
                // JSON解析错误，简单记录并继续
                println!("JSON解析错误: {}", e);
                continue;
            }
        }
    }
    
    // 客户端断开连接，清理资源
    cleanup_player(&server, &player_id).await;
    println!("连接断开: {}", addr);
}

// 处理客户端消息
async fn process_client_message(server: Arc<Mutex<GameServer>>, player_id: &str, message: GameMessage) {
    // 尽量减少锁的持有时间，只在必要的时候获取锁
    match &message {
        GameMessage::CreateRoom { roomId } => {
            let final_room_id;
            {
                let mut server_guard = server.lock().await;
                
                final_room_id = if roomId.is_empty() {
                    // 生成6位数字房间ID，更易记
                    let mut rng = thread_rng();
                    let room_num = rng.gen_range(100000..999999);
                    room_num.to_string()
                } else {
                    roomId.clone()
                };
                
                // 创建房间
                server_guard.rooms.insert(
                    final_room_id.clone(), 
                    Room {
                        id: final_room_id.clone(),
                        player_black: Some(player_id.to_string()),
                        player_white: None,
                        moves: Vec::new(),
                        turn: -1,
                    }
                );
                
                if let Some(player) = server_guard.players.get_mut(player_id) {
                    player.room_id = Some(final_room_id.clone());
                }
                
                println!("房间创建: {} -> {}", player_id, final_room_id);
                
                // 发送确认消息
                if let Some(player) = server_guard.players.get(player_id) {
                    let response = GameMessage::CreateRoom { roomId: final_room_id.clone() };
                    let _ = server_guard.send_message(player, response).await;
                }
            }
        }
        
        GameMessage::JoinRoom { roomId } => {
            let mut server_guard = server.lock().await;
            let success = server_guard.join_room(player_id, roomId).await;
            
            if success {
                if let Some(player) = server_guard.players.get(player_id) {
                    // 发送确认消息
                    let response = GameMessage::JoinRoom { roomId: roomId.clone() };
                    let _ = server_guard.send_message(player, response).await;
                }
                
                println!("加入房间: {} -> {}", player_id, roomId);
            }
        }
        
        GameMessage::Move { networkStone } => {
            let mut server_guard = server.lock().await;
            // 使用快速路径处理落子消息
            let _ = server_guard.process_move(player_id, networkStone.clone()).await;
            // 不等待落子处理完成，立即返回
        }
        
        GameMessage::Heartbeat { roomId } => {
            // 心跳消息只在需要时响应
            if !roomId.is_empty() {
                let mut server_guard = server.lock().await;
                if let Some(player) = server_guard.players.get(player_id) {
                    let response = GameMessage::Heartbeat { roomId: roomId.clone() };
                    let _ = server_guard.send_message(player, response).await;
                }
            }
        }
        
        _ => {
            // 其他消息类型忽略
        }
    }
}

// 清理断开连接的玩家
async fn cleanup_player(server: &Arc<Mutex<GameServer>>, player_id: &str) {
    let mut server = server.lock().await;
    
    if let Some(player) = server.players.remove(player_id) {
        if let Some(room_id) = player.room_id {
            if let Some(room) = server.rooms.get_mut(&room_id) {
                if room.player_black.as_ref().map_or(false, |id| id == player_id) {
                    room.player_black = None;
                }
                if room.player_white.as_ref().map_or(false, |id| id == player_id) {
                    room.player_white = None;
                }
                
                // 如果房间空了，删除房间
                if room.player_black.is_none() && room.player_white.is_none() {
                    server.rooms.remove(&room_id);
                }
            }
        }
    }
}

#[tokio::main]
async fn main() {
    // 设置日志级别
    std::env::set_var("RUST_LOG", "info");
    env_logger::init();
    
    // 初始化服务器状态
    let server = Arc::new(Mutex::new(GameServer::new()));
    
    // 设置监听地址
    let addr = "0.0.0.0:8080";
    println!("围棋服务器启动，监听: {}", addr);
    
    // 创建TCP监听器
    let listener = match TcpListener::bind(addr).await {
        Ok(listener) => listener,
        Err(e) => {
            eprintln!("无法绑定到地址 {}: {}", addr, e);
            return;
        }
    };
    
    // 处理连接请求
    loop {
        match listener.accept().await {
            Ok((stream, addr)) => {
                println!("接受新连接: {}", addr);
                
                // 为每个新连接创建一个任务
                let server_clone = Arc::clone(&server);
                
                tokio::spawn(async move {
                    handle_client(stream, server_clone).await;
                });
            }
            Err(e) => {
                eprintln!("接受连接错误: {}", e);
                // 短暂暂停以避免CPU占用过高
                tokio::time::sleep(tokio::time::Duration::from_millis(100)).await;
            }
        }
    }
} 