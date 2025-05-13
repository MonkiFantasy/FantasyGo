package com.monki.util;

import com.monki.core.Board;
import com.monki.entity.Position;
import com.monki.entity.Stone;
import com.monki.core.StoneString;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * 形势判断工具类
 * 整合KataGo的评估结果
 */
public class PositionEvaluator {
    // 棋盘状态常量
    private static final int EMPTY = 0;
    private static final int BLACK = -1;
    private static final int WHITE = 1;
    
    // 棋盘大小
    private static final int BOARD_SIZE = 19;
    
    // KataGo评估结果
    private static double whiteWinrate = 0.0;
    private static double whiteLead = 0.0;
    private static double[][] ownership = new double[BOARD_SIZE + 2][BOARD_SIZE + 2];
    private static double[][] policy = new double[BOARD_SIZE + 2][BOARD_SIZE + 2];
    
    // 每个点的领地属性：-100到100之间的值，负值表示黑方领地，正值表示白方领地，绝对值表示强度
    private static int[][] territoryMap = new int[BOARD_SIZE + 2][BOARD_SIZE + 2];
    
    // 死子标记
    private static boolean[][] deadStones = new boolean[BOARD_SIZE + 2][BOARD_SIZE + 2];
    
    // 计算结果
    private static int blackTerritory = 0;
    private static int whiteTerritory = 0;
    private static int blackCaptures = 0;
    private static int whiteCaptures = 0;
    
    // 影响力衰减率
    private static final double INFLUENCE_DECAY = 0.75;  // 增大衰减率，提高精确度
    
    // 贴目数（中国规则通常为3.75目）
    private static final double KOMI = 3.75;
    
    /**
     * 设置KataGo的评估结果
     */
    public static void setKataGoEvaluation(double winrate, double lead, double[][] ownerships, double[][] policies) {
        whiteWinrate = winrate;
        whiteLead = lead;
        ownership = ownerships;
        policy = policies;
    }
    
    /**
     * 评估当前局面
     * @return 返回一个包含各种评估信息的Map
     */
    public static Map<String, Object> evaluatePosition() {
        // 初始化数据
        resetData();
        
        // 1. 检测死子
        detectDeadStones();
        
        // 2. 确定领地归属（使用KataGo的ownership）
        determineTerritoriesFromKataGo();
        
        // 3. 计算最终得分
        calculateScore();
        
        // 封装结果
        Map<String, Object> result = new HashMap<>();
        result.put("territoryMap", territoryMap);
        result.put("deadStones", deadStones);
        result.put("blackTerritory", blackTerritory);
        result.put("whiteTerritory", whiteTerritory);
        result.put("blackCaptures", blackCaptures);
        result.put("whiteCaptures", whiteCaptures);
        result.put("whiteWinrate", whiteWinrate);
        result.put("whiteLead", whiteLead);
        result.put("policy", policy);
        
        // 计算考虑贴目的得分差
        double scoreDiff = blackTerritory + blackCaptures - (whiteTerritory + whiteCaptures + KOMI);
        result.put("scoreDiff", scoreDiff);
        
        return result;
    }
    
    /**
     * 重置数据结构
     */
    private static void resetData() {
        territoryMap = new int[BOARD_SIZE + 2][BOARD_SIZE + 2];

        deadStones = new boolean[BOARD_SIZE + 2][BOARD_SIZE + 2];
        blackTerritory = 0;
        whiteTerritory = 0;
        blackCaptures = 0;
        whiteCaptures = 0;
    }
    
    /**
     * 检测死子
     * 使用KataGo的ownership来辅助判断死子
     */
    private static void detectDeadStones() {
        // 检查黑子棋串
        for (StoneString string : Board.blackString) {
            if (isLikelyDeadByKataGo(string, BLACK)) {
                markStringAsDead(string);
                whiteCaptures += string.getStones().size();
            }
        }
        
        // 检查白子棋串
        for (StoneString string : Board.whiteString) {
            if (isLikelyDeadByKataGo(string, WHITE)) {
                markStringAsDead(string);
                blackCaptures += string.getStones().size();
            }
        }
    }
    
    /**
     * 使用KataGo的ownership来判断棋串是否已死
     */
    private static boolean isLikelyDeadByKataGo(StoneString string, int color) {
        double totalOwnership = 0;
        int stoneCount = 0;
        
        // 计算棋串所有棋子位置的ownership平均值
        for (Stone stone : string.getStones()) {
            int i = stone.getIndex().getJ();
            int j = stone.getIndex().getI();
            totalOwnership += ownership[i][j];
            stoneCount++;
        }
        
        double avgOwnership = totalOwnership / stoneCount;
        
        // 如果是黑子，ownership应该为负；如果是白子，ownership应该为正
        // 如果与预期相反，说明这个棋串可能已死
        return (color == BLACK && avgOwnership > 0.5) || (color == WHITE && avgOwnership < -0.5);
    }
    
    /**
     * 标记一个棋串为死子
     */
    private static void markStringAsDead(StoneString string) {
        for (Stone stone : string.getStones()) {
            int i = stone.getIndex().getJ();
            int j = stone.getIndex().getI();
            deadStones[i][j] = true;
        }
    }
    
    /**
     * 使用KataGo的ownership来确定领地归属
     */
    private static void determineTerritoriesFromKataGo() {
        for (int i = 1; i <= BOARD_SIZE; i++) {
            for (int j = 1; j <= BOARD_SIZE; j++) {
                if (Board.state[i][j] == EMPTY) {
                    // 将KataGo的ownership值（-1到1）转换为我们的领地强度值（-100到100）
                    int strength = (int)(ownership[i][j] * 100);
                    territoryMap[i][j] = strength;
                    
                    // 统计领地数量
                    if (strength < 0) {
                        blackTerritory++;
                    } else if (strength > 0) {
                        whiteTerritory++;
                    }
                }
            }
        }
    }
    
    /**
     * 计算最终得分
     * 包括：领地 + 提子数 + 贴目
     */
    private static void calculateScore() {
        // 已经在determineTerritoriesFromKataGo中计算领地
        // 已经在detectDeadStones中计算提子数
        // 贴目会在返回结果时考虑
    }
    
    /**
     * 获取适合显示的领地颜色
     */
    public static Color getTerritoryColor(int i, int j) {
        // 如果有棋子，不显示领地颜色
        if (Board.state[i][j] != EMPTY) {
            if (deadStones[i][j]) {
                // 死子用红色标记
                return new Color(255, 0, 0, 100);
            }
            return null;
        }
        
        int value = territoryMap[i][j];
        if (value < 0) {
            // 黑方领地，使用半透明黑色
            int alpha = Math.min(180, 50 + Math.abs(value));
            return new Color(0, 0, 0, alpha);
        } else if (value > 0) {
            // 白方领地，使用半透明蓝色
            int alpha = Math.min(180, 50 + Math.abs(value));
            return new Color(30, 144, 255, alpha);
        }
        
        // 中立区域不显示颜色
        return null;
    }
    
    /**
     * 获取KataGo建议的最佳落子点
     * @return 返回最佳落子点的位置
     */
    public static Position getBestMove() {
        // 找到policy值最大的空点
        double maxPolicy = -1.0;
        Position bestMove = null;
        
        System.out.println("开始寻找最佳落子点...");
        
        for (int i = 1; i <= BOARD_SIZE; i++) {
            for (int j = 1; j <= BOARD_SIZE; j++) {
                // 只在空点中寻找
                if (Board.state[i][j] == EMPTY) {
                    // 检查policy值是否有效
                    if (Double.isNaN(policy[i][j]) || Double.isInfinite(policy[i][j])) {
                        continue;
                    }
                    
                    if (policy[i][j] > maxPolicy) {
                        maxPolicy = policy[i][j];
                        bestMove = new Position(j, i);
                    }
                }
            }
        }
        
        if (bestMove != null) {
            System.out.println("找到最佳落子点：(" + bestMove.getI() + "," + bestMove.getJ() + ")，概率：" + maxPolicy);
        } else {
            System.out.println("未找到有效的最佳落子点");
        }
        
        return bestMove;
    }
    
    /**
     * 判断位置是否在有效范围内
     */
    private static boolean isValidPosition(Position pos) {
        int i = pos.getI();
        int j = pos.getJ();
        return i >= 1 && i <= BOARD_SIZE && j >= 1 && j <= BOARD_SIZE;
    }
} 