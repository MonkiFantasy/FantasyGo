package com.monki.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Config {
    public static final int X=400;//棋盘左上角顶点x坐标
    public static final int Y=50;//棋盘左上角顶点y坐标
    public static final int LENGTH=792;//棋盘宽度
    public static final int PATH=19;//围棋路数
    public static final int SPACE=LENGTH/(PATH-1);
    public static int MODE=1;//对弈模式 0-单机对弈 1-联机对战
    public static int GAMESTATUS=0;//状态判断 是否在进行棋局 0否 1是
    public static Boolean SERVER=null;
    
    // 用于JSON序列化/反序列化的GSON实例 - 优化版
    // 用于JSON序列化/反序列化的GSON实例 - 极低延迟版
    // 用于JSON序列化/反序列化的GSON实例 - 极低延迟版
    // 用于JSON序列化/反序列化的GSON实例 - 极低延迟版
    public static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .disableInnerClassSerialization()
            .create();
    
    // 新增 - 服务器配置
    public static final String SERVER_ADDRESS = "127.0.0.1"; // 使用本地服务器，极低延迟 // 使用本地服务器，极低延迟 // 使用本地服务器，极低延迟 // 修改为本地地址
    public static final int SERVER_PORT = 11434;
}
