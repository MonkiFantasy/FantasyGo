package com.monki.util;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * KataGo运行器
 * 负责与KataGo进程交互
 */
public class KataGoRunner {
    private Process kataGoProcess;
    private BufferedWriter kataGoInput;
    private BufferedReader kataGoOutput;
    private static final String KATAGO_PATH = "/home/monki/GitClones/katago-v1.16.0-eigen-linux-x64/katago";
    private static final String MODEL_PATH = "/home/monki/Downloads/kata1-b28c512nbt-s8834891520-d4763401477.bin.gz";
    private static final String CONFIG_PATH = "/home/monki/GitClones/katago-v1.16.0-eigen-linux-x64/default_gtp.cfg";
    
    public void startKataGo() throws IOException, InterruptedException {
        // 确保KataGo和模型文件存在
        File katagoFile = new File(KATAGO_PATH);
        File modelFile = new File(MODEL_PATH);
        File configFile = new File(CONFIG_PATH);
        
        if (!katagoFile.exists()) {
            throw new FileNotFoundException("KataGo可执行文件未找到: " + KATAGO_PATH);
        }
        if (!modelFile.exists()) {
            throw new FileNotFoundException("KataGo模型文件未找到: " + MODEL_PATH);
        }
        if (!configFile.exists()) {
            throw new FileNotFoundException("KataGo配置文件未找到: " + CONFIG_PATH);
        }
        
        ProcessBuilder pb = new ProcessBuilder(
            KATAGO_PATH,
            "gtp",
            "-model", MODEL_PATH,
            "-config", CONFIG_PATH
        );
        
        // 设置工作目录
        pb.directory(new File("/home/monki/GitClones/katago-v1.16.0-eigen-linux-x64"));
        
        // 重定向错误流到标准输出
        pb.redirectErrorStream(true);
        
        // 启动进程
        kataGoProcess = pb.start();
        
        // 获取输入输出流
        kataGoInput = new BufferedWriter(new OutputStreamWriter(kataGoProcess.getOutputStream()));
        kataGoOutput = new BufferedReader(new InputStreamReader(kataGoProcess.getInputStream()));
        
        // 等待KataGo启动
        Thread.sleep(1000);
        
        // 测试KataGo是否正常运行
        String response = sendCommand("name");
        if (response == null || !response.toLowerCase().contains("katago")) {
            throw new IOException("KataGo启动失败");
        }
    }
    
    public void stopKataGo() {
        try {
            if (kataGoInput != null) {
                kataGoInput.write("quit\n");
                kataGoInput.flush();
                kataGoInput.close();
            }
            if (kataGoOutput != null) {
                kataGoOutput.close();
            }
            if (kataGoProcess != null) {
                kataGoProcess.destroy();
                kataGoProcess.waitFor(5, TimeUnit.SECONDS);
                if (kataGoProcess.isAlive()) {
                    kataGoProcess.destroyForcibly();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public String analyzeSgf(String sgfPath) {
        try {
            // 确保SGF文件存在
            File sgfFile = new File(sgfPath);
            if (!sgfFile.exists()) {
                System.err.println("SGF文件未找到: " + sgfPath);
                return null;
            }
            
            // 加载SGF文件
            String loadResponse = sendCommand("loadsgf " + sgfPath);
            if (loadResponse == null || loadResponse.toLowerCase().contains("error")) {
                System.err.println("加载SGF文件失败: " + loadResponse);
                return null;
            }
            
            // 使用kata-raw-nn命令分析当前局面
            String analysis = sendCommand("kata-raw-nn 0");
            
            // 即使包含"error"也返回结果，因为KataGo在返回的数据中包含error字样
            return analysis;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private String sendCommand(String command) throws IOException {
        // 发送命令
        kataGoInput.write(command + "\n");
        kataGoInput.flush();
        
        StringBuilder response = new StringBuilder();
        String line;
        
        // 读取响应直到遇到空行
        while ((line = kataGoOutput.readLine()) != null) {
            if (line.trim().isEmpty()) {
                break;
            }
            response.append(line).append("\n");
        }
        
        return response.toString();
    }
} 