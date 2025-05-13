package com.monki.util;

/**
 * KataGo评估结果处理工具类
 */
public class KataGoEvaluator {
    private static final int BOARD_SIZE = 19;
    
    /**
     * 解析KataGo的原始输出并设置到PositionEvaluator中
     * @param kataGoOutput KataGo的原始输出字符串
     */
    public static void processKataGoOutput(String kataGoOutput) {
        try {
            System.out.println("开始解析KataGo输出...");
            
            if (kataGoOutput == null || kataGoOutput.trim().isEmpty()) {
                System.err.println("KataGo输出为空");
                return;
            }
            
            // 输出原始数据的一部分，用于调试
            String debugOutput = kataGoOutput.length() > 500 ? 
                kataGoOutput.substring(0, 500) + "..." : kataGoOutput;
            System.out.println("KataGo原始输出片段: \n" + debugOutput);
            
            // 解析胜率
            double whiteWin = parseValue(kataGoOutput, "whiteWin");
            System.out.println("解析得到胜率: " + whiteWin);
            
            // 解析目差
            double whiteLead = parseValue(kataGoOutput, "whiteLead");
            System.out.println("解析得到目差: " + whiteLead);
            
            // 初始化矩阵
            double[][] ownership = new double[BOARD_SIZE + 2][BOARD_SIZE + 2];
            double[][] policy = new double[BOARD_SIZE + 2][BOARD_SIZE + 2];
            
            // 初始化为NaN以便于调试
            for (int i = 0; i < BOARD_SIZE + 2; i++) {
                for (int j = 0; j < BOARD_SIZE + 2; j++) {
                    ownership[i][j] = Double.NaN;
                    policy[i][j] = Double.NaN;
                }
            }
            
            // 解析ownership矩阵（在KataGo输出中为whiteOwnership）
            String[] lines = kataGoOutput.split("\\n");
            boolean inOwnership = false;
            int ownershipRow = 1;
            boolean inPolicy = false;
            int policyRow = 1;
            
            for (String line : lines) {
                // 处理ownership矩阵
                if (line.trim().equals("whiteOwnership")) {
                    inOwnership = true;
                    inPolicy = false;
                    ownershipRow = 1;
                    continue;
                }
                
                // 处理policy矩阵
                if (line.trim().equals("policy")) {
                    inPolicy = true;
                    inOwnership = false;
                    policyRow = 1;
                    continue;
                }
                
                // 填充ownership矩阵
                if (inOwnership && ownershipRow <= BOARD_SIZE) {
                    String[] values = line.trim().split("\\s+");
                    if (values.length >= BOARD_SIZE) {
                        for (int col = 1; col <= BOARD_SIZE && col-1 < values.length; col++) {
                            if (!values[col-1].equalsIgnoreCase("NAN") && !values[col-1].isEmpty()) {
                                try {
                                    ownership[ownershipRow][col] = Double.parseDouble(values[col-1]);
                                } catch (NumberFormatException e) {
                                    System.err.println("解析ownership时出错，位置[" + ownershipRow + "," + col + "]，值：" + values[col-1]);
                                    ownership[ownershipRow][col] = 0.0;
                                }
                            } else {
                                ownership[ownershipRow][col] = 0.0;
                            }
                        }
                        ownershipRow++;
                    }
                }
                
                // 填充policy矩阵
                if (inPolicy && policyRow <= BOARD_SIZE) {
                    String[] values = line.trim().split("\\s+");
                    if (values.length >= BOARD_SIZE) {
                        for (int col = 1; col <= BOARD_SIZE && col-1 < values.length; col++) {
                            if (!values[col-1].equalsIgnoreCase("NAN") && !values[col-1].isEmpty()) {
                                try {
                                    policy[policyRow][col] = Double.parseDouble(values[col-1]);
                                } catch (NumberFormatException e) {
                                    System.err.println("解析policy时出错，位置[" + policyRow + "," + col + "]，值：" + values[col-1]);
                                    policy[policyRow][col] = 0.0;
                                }
                            } else {
                                policy[policyRow][col] = 0.0;
                            }
                        }
                        policyRow++;
                    }
                }
            }
            
            System.out.println("解析得到ownership矩阵，行数：" + (ownershipRow - 1));
            System.out.println("解析得到policy矩阵，行数：" + (policyRow - 1));
            
            // 验证数据是否有效
            if (ownershipRow <= 1 || policyRow <= 1) {
                System.err.println("警告：矩阵数据似乎不完整，请检查KataGo输出格式");
            }
            
            // 设置评估结果
            PositionEvaluator.setKataGoEvaluation(whiteWin, whiteLead, ownership, policy);
            System.out.println("成功设置KataGo评估结果");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("解析KataGo输出失败：" + e.getMessage());
        }
    }
    
    /**
     * 从KataGo输出中解析特定值
     */
    private static double parseValue(String output, String key) {
        String[] lines = output.split("\\n");
        for (String line : lines) {
            if (line.startsWith(key)) {
                try {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        return Double.parseDouble(parts[1]);
                    }
                } catch (Exception e) {
                    System.err.println("解析" + key + "时出错: " + e.getMessage());
                }
            }
        }
        System.err.println("未找到" + key + "值");
        return 0.0;
    }
} 