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
 * 实现类似野狐围棋的形势判断算法
 */
public class PositionEvaluator {
    // 棋盘状态常量
    private static final int EMPTY = 0;
    private static final int BLACK = -1;
    private static final int WHITE = 1;
    
    // 棋盘大小
    private static final int BOARD_SIZE = 19;
    
    // 影响半径，用于计算领地和影响力
    private static final int INFLUENCE_RADIUS = 5;  // 增加影响半径，提高远程影响
    
    // 每个点的领地属性：-100到100之间的值，负值表示黑方领地，正值表示白方领地，绝对值表示强度
    private static int[][] territoryMap = new int[BOARD_SIZE + 2][BOARD_SIZE + 2];
    
    // 每个点的影响力：-100到100之间的值，负值表示黑方影响，正值表示白方影响
    private static int[][] influenceMap = new int[BOARD_SIZE + 2][BOARD_SIZE + 2];
    
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
     * 评估当前局面
     * @return 返回一个包含各种评估信息的Map
     */
    public static Map<String, Object> evaluatePosition() {
        // 初始化数据
        resetData();
        
        // 1. 检测死子
        detectDeadStones();
        
        // 2. 计算影响力
        calculateInfluence();
        
        // 3. 确定领地归属
        determineTerritories();
        
        // 4. 计算最终得分
        calculateScore();
        
        // 封装结果
        Map<String, Object> result = new HashMap<>();
        result.put("territoryMap", territoryMap);
        result.put("influenceMap", influenceMap);
        result.put("deadStones", deadStones);
        result.put("blackTerritory", blackTerritory);
        result.put("whiteTerritory", whiteTerritory);
        result.put("blackCaptures", blackCaptures);
        result.put("whiteCaptures", whiteCaptures);
        
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
        influenceMap = new int[BOARD_SIZE + 2][BOARD_SIZE + 2];
        deadStones = new boolean[BOARD_SIZE + 2][BOARD_SIZE + 2];
        blackTerritory = 0;
        whiteTerritory = 0;
        blackCaptures = 0;
        whiteCaptures = 0;
    }
    
    /**
     * 检测死子
     * 判断条件：
     * 1. 棋串气数较少（<=2）
     * 2. 周围敌方棋子较多
     * 3. 周围存在强敌方影响力
     * 4. 考虑棋串大小和形状
     */
    private static void detectDeadStones() {
        // 检查黑子棋串
        for (StoneString string : Board.blackString) {
            if (isLikelyDead(string, BLACK)) {
                markStringAsDead(string);
                whiteCaptures += string.getStones().size();
            }
        }
        
        // 检查白子棋串
        for (StoneString string : Board.whiteString) {
            if (isLikelyDead(string, WHITE)) {
                markStringAsDead(string);
                blackCaptures += string.getStones().size();
            }
        }
    }
    
    /**
     * 判断一个棋串是否可能已死
     */
    private static boolean isLikelyDead(StoneString string, int color) {
        // 如果气数大于3，通常不会是死棋
        if (string.getLiberty() > 3) {
            return false;
        }
        
        // 大棋串不容易被判为死子
        if (string.getStones().size() > 15) {
            return false;
        }
        
        // 计算周围敌方棋子的数量和友方棋子数量
        int enemyStones = 0;
        int friendlyStones = 0;
        Set<Position> libertyPositions = new HashSet<>(string.getLibertyPos());
        
        for (Stone stone : string.getStones()) {
            int i = stone.getIndex().getJ();
            int j = stone.getIndex().getI();
            
            // 检查周围四个方向
            Position[] neighbors = new Position[] {
                new Position(i-1, j), // 上
                new Position(i+1, j), // 下
                new Position(i, j-1), // 左
                new Position(i, j+1)  // 右
            };
            
            for (Position pos : neighbors) {
                if (isValidPosition(pos)) {
                    int stoneColor = Board.state[pos.getI()][pos.getJ()];
                    if (stoneColor == -color) { // 敌方棋子
                        enemyStones++;
                    } else if (stoneColor == color) { // 友方棋子
                        friendlyStones++;
                    }
                }
            }
        }
        
        // 气数少且周围敌方棋子数量明显多于友方棋子，可能是死棋
        if (string.getLiberty() <= 1 && enemyStones > friendlyStones + 2) {
            return true;
        }
        
        // 特殊情况：如果是眼形，但被敌方完全包围
        if (string.getLiberty() == 1 && isEyeShaped(string, color) && enemyStones >= 3) {
            return true;
        }
        
        // 检查气的位置是否都被敌方控制
        boolean allLibertiesControlled = true;
        for (Position liberty : libertyPositions) {
            // 检查气位置周围的影响力
            int surroundingInfluence = checkSurroundingInfluence(liberty, -color);
            if (surroundingInfluence < 3) { // 如果气位置周围的敌方影响力不够强
                allLibertiesControlled = false;
                break;
            }
        }
        
        return string.getLiberty() <= 2 && allLibertiesControlled && enemyStones > friendlyStones;
    }
    
    /**
     * 检查位置周围的影响力
     * @param pos 位置
     * @param color 检查颜色的影响力
     * @return 位置周围指定颜色影响力的强度
     */
    private static int checkSurroundingInfluence(Position pos, int color) {
        int i = pos.getI();
        int j = pos.getJ();
        int influenceCount = 0;
        
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                if (di == 0 && dj == 0) continue;
                
                int ni = i + di;
                int nj = j + dj;
                
                if (isValidPosition(new Position(ni, nj))) {
                    int influence = influenceMap[ni][nj];
                    if ((color == BLACK && influence < 0) || (color == WHITE && influence > 0)) {
                        influenceCount++;
                    }
                }
            }
        }
        
        return influenceCount;
    }
    
    /**
     * 判断是否为眼形状（一个空间被包围）
     */
    private static boolean isEyeShaped(StoneString string, int color) {
        if (string.getLiberty() != 1) {
            return false;
        }
        
        // 获取唯一的气的位置
        Position liberty = null;
        for (Position pos : string.getLibertyPos()) {
            liberty = pos;
            break;
        }
        
        if (liberty == null) {
            return false;
        }
        
        // 检查这个气的周围是否都是相同颜色的棋子
        int surroundCount = 0;
        int i = liberty.getI();
        int j = liberty.getJ();
        Position[] neighbors = new Position[] {
            new Position(i-1, j), // 上
            new Position(i+1, j), // 下
            new Position(i, j-1), // 左
            new Position(i, j+1)  // 右
        };
        
        for (Position pos : neighbors) {
            if (isValidPosition(pos) && Board.state[pos.getI()][pos.getJ()] == color) {
                surroundCount++;
            }
        }
        
        return surroundCount >= 3; // 至少3个方向被同色棋子包围，形成眼形
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
     * 计算影响力
     * 使用衰减模型：棋子对周围N格范围内有影响，随距离衰减
     */
    private static void calculateInfluence() {
        // 首先基于棋子位置计算初始影响力
        for (int i = 1; i <= BOARD_SIZE; i++) {
            for (int j = 1; j <= BOARD_SIZE; j++) {
                int state = Board.state[i][j];
                if (state != EMPTY && !deadStones[i][j]) {
                    // 如果不是死子，计算其影响力
                    spreadInfluence(i, j, state);
                }
            }
        }
        
        // 多次平滑处理，使影响力分布更加自然
        for (int k = 0; k < 3; k++) {
            smoothInfluence();
        }
    }
    
    /**
     * 从一个棋子位置扩散影响力
     */
    private static void spreadInfluence(int i, int j, int color) {
        // 中心点的影响力
        influenceMap[i][j] = color * 100;
        
        // 向四周扩散影响力，影响力随距离衰减
        for (int di = -INFLUENCE_RADIUS; di <= INFLUENCE_RADIUS; di++) {
            for (int dj = -INFLUENCE_RADIUS; dj <= INFLUENCE_RADIUS; dj++) {
                if (di == 0 && dj == 0) continue; // 跳过中心点
                
                int ni = i + di;
                int nj = j + dj;
                
                if (isValidPosition(new Position(ni, nj))) {
                    // 使用欧几里得距离计算，比曼哈顿距离更精确
                    double distance = Math.sqrt(di * di + dj * dj);
                    if (distance <= INFLUENCE_RADIUS) {
                        // 根据距离计算影响力
                        int influence = (int)(100 * Math.pow(INFLUENCE_DECAY, distance));
                        
                        // 考虑棋盘边缘效应
                        if (isNearEdge(ni, nj)) {
                            influence = (int)(influence * 1.2); // 边缘位置增强影响
                        }
                        
                        influenceMap[ni][nj] += color * influence;
                        
                        // 限制影响力在-100到100之间
                        influenceMap[ni][nj] = Math.max(-100, Math.min(100, influenceMap[ni][nj]));
                    }
                }
            }
        }
    }
    
    /**
     * 判断一个位置是否靠近棋盘边缘
     */
    private static boolean isNearEdge(int i, int j) {
        return i <= 2 || i >= BOARD_SIZE - 1 || j <= 2 || j >= BOARD_SIZE - 1;
    }
    
    /**
     * 平滑影响力，调整黑白双方的影响对抗
     */
    private static void smoothInfluence() {
        int[][] tempMap = new int[BOARD_SIZE + 2][BOARD_SIZE + 2];
        
        // 复制当前影响力
        for (int i = 0; i <= BOARD_SIZE + 1; i++) {
            System.arraycopy(influenceMap[i], 0, tempMap[i], 0, BOARD_SIZE + 2);
        }
        
        // 平滑处理 - 每个点受周围点的影响
        for (int i = 1; i <= BOARD_SIZE; i++) {
            for (int j = 1; j <= BOARD_SIZE; j++) {
                if (Board.state[i][j] != EMPTY) continue; // 跳过有棋子的位置
                
                // 计算周围八个方向的加权平均影响力
                int sum = 0;
                int count = 0;
                
                for (int di = -1; di <= 1; di++) {
                    for (int dj = -1; dj <= 1; dj++) {
                        if (di == 0 && dj == 0) continue;
                        
                        int ni = i + di;
                        int nj = j + dj;
                        
                        if (isValidPosition(new Position(ni, nj))) {
                            // 直接相邻的点权重更高
                            int weight = (Math.abs(di) + Math.abs(dj) == 1) ? 3 : 1;
                            sum += tempMap[ni][nj] * weight;
                            count += weight;
                        }
                    }
                }
                
                if (count > 0) {
                    // 原影响力权重0.6，周围加权平均影响力权重0.4
                    influenceMap[i][j] = (int)(0.6 * tempMap[i][j] + 0.4 * (sum / count));
                }
            }
        }
    }
    
    /**
     * 确定领地归属
     * 根据影响力计算每个空点的归属
     */
    private static void determineTerritories() {
        // 使用洪水填充算法确定连通区域
        boolean[][] visited = new boolean[BOARD_SIZE + 2][BOARD_SIZE + 2];
        
        for (int i = 1; i <= BOARD_SIZE; i++) {
            for (int j = 1; j <= BOARD_SIZE; j++) {
                if (Board.state[i][j] == EMPTY && !visited[i][j]) {
                    // 找到一个未访问的空点，开始填充
                    floodFillTerritory(i, j, visited);
                }
            }
        }
    }
    
    /**
     * 使用洪水填充算法确定连通的领地区域
     */
    private static void floodFillTerritory(int i, int j, boolean[][] visited) {
        Queue<Position> queue = new LinkedList<>();
        java.util.List<Position> region = new ArrayList<>();
        
        queue.add(new Position(i, j));
        visited[i][j] = true;
        
        // 累计区域内的影响力
        int totalInfluence = 0;
        
        // 检查区域边界的围棋棋子
        Map<Integer, Integer> boundaryStonesCount = new HashMap<>();
        boundaryStonesCount.put(BLACK, 0);
        boundaryStonesCount.put(WHITE, 0);
        
        while (!queue.isEmpty()) {
            Position pos = queue.poll();
            region.add(pos);
            totalInfluence += influenceMap[pos.getI()][pos.getJ()];
            
            // 检查四个方向
            Position[] neighbors = new Position[] {
                new Position(pos.getI()-1, pos.getJ()), // 上
                new Position(pos.getI()+1, pos.getJ()), // 下
                new Position(pos.getI(), pos.getJ()-1), // 左
                new Position(pos.getI(), pos.getJ()+1)  // 右
            };
            
            for (Position next : neighbors) {
                if (isValidPosition(next)) {
                    int state = Board.state[next.getI()][next.getJ()];
                    if (state == EMPTY && !visited[next.getI()][next.getJ()]) {
                        queue.add(next);
                        visited[next.getI()][next.getJ()] = true;
                    } else if (state != EMPTY && !deadStones[next.getI()][next.getJ()]) {
                        // 统计区域边界的黑白棋子数量
                        boundaryStonesCount.put(state, boundaryStonesCount.get(state) + 1);
                    }
                }
            }
        }
        
        // 确定区域归属：综合考虑影响力和边界棋子
        int averageInfluence = region.size() > 0 ? totalInfluence / region.size() : 0;
        
        // 边界判断：如果一方棋子完全包围此区域，则属于该方
        int blackBoundary = boundaryStonesCount.get(BLACK);
        int whiteBoundary = boundaryStonesCount.get(WHITE);
        
        int territoryOwner;
        int territoryStrength;
        
        if (blackBoundary > 0 && whiteBoundary == 0) {
            // 完全被黑棋包围
            territoryOwner = BLACK;
            territoryStrength = 80 + Math.min(20, blackBoundary);
        } else if (whiteBoundary > 0 && blackBoundary == 0) {
            // 完全被白棋包围
            territoryOwner = WHITE;
            territoryStrength = 80 + Math.min(20, whiteBoundary);
        } else {
            // 根据影响力判断
            territoryOwner = averageInfluence < 0 ? BLACK : (averageInfluence > 0 ? WHITE : EMPTY);
            territoryStrength = Math.abs(averageInfluence);
            
            // 边界棋子数量也会影响领地强度
            if (territoryOwner == BLACK && blackBoundary > whiteBoundary) {
                territoryStrength = Math.min(100, territoryStrength + (blackBoundary - whiteBoundary) * 5);
            } else if (territoryOwner == WHITE && whiteBoundary > blackBoundary) {
                territoryStrength = Math.min(100, territoryStrength + (whiteBoundary - blackBoundary) * 5);
            }
        }
        
        // 标记领地
        for (Position pos : region) {
            territoryMap[pos.getI()][pos.getJ()] = territoryOwner * territoryStrength;
            
            // 统计领地数量
            if (territoryOwner == BLACK) {
                blackTerritory++;
            } else if (territoryOwner == WHITE) {
                whiteTerritory++;
            }
        }
    }
    
    /**
     * 计算最终得分
     * 包括：领地 + 提子数 + 贴目
     */
    private static void calculateScore() {
        // 已经在determineTerritories中计算领地
        // 已经在detectDeadStones中计算提子数
        // 贴目会在返回结果时考虑
    }
    
    /**
     * 判断位置是否在有效范围内
     */
    private static boolean isValidPosition(Position pos) {
        int i = pos.getI();
        int j = pos.getJ();
        return i >= 1 && i <= BOARD_SIZE && j >= 1 && j <= BOARD_SIZE;
    }
    
    /**
     * 获取适合显示的领地颜色
     * @param i 行索引
     * @param j 列索引
     * @return 用于显示的颜色，null表示无需显示
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
            return new Color(30, 144, 255, alpha);  // 使用更好看的道奇蓝
        }
        
        // 中立区域不显示颜色
        return null;
    }
} 