package com.monki.test;

import java.util.Arrays;

public class GoBoardEvaluation {

    // 定义棋盘大小，例如9x9
    private static final int BOARD_SIZE = 19;
    private static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1} // 上下左右四个方向
    };

    // 棋盘二维数组，0表示空，1表示黑子，2表示白子
    private int[][] board = new int[BOARD_SIZE][BOARD_SIZE];

    // 膨胀操作
    private void dilate() {
        int[][] newBoard = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != 0) {
                    newBoard[i][j] = board[i][j]; // 保留原有棋子
                } else {
                    for (int[] direction : DIRECTIONS) {
                        int ni = i + direction[0];
                        int nj = j + direction[1];
                        if (ni >= 0 && ni < BOARD_SIZE && nj >= 0 && nj < BOARD_SIZE && board[ni][nj] != 0) {
                            newBoard[i][j] = board[ni][nj]; // 膨胀：将相邻的棋子颜色扩展到当前位置
                            break;
                        }
                    }
                }
            }
        }
        board = newBoard; // 更新棋盘
    }

    // 腐蚀操作
    private void erode() {
        int[][] newBoard = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                boolean isSurrounded = true;
                for (int[] direction : DIRECTIONS) {
                    int ni = i + direction[0];
                    int nj = j + direction[1];
                    if (ni >= 0 && ni < BOARD_SIZE && nj >= 0 && nj < BOARD_SIZE && board[ni][nj] == 0) {
                        isSurrounded = false; // 如果有一个相邻位置是空的，则不被完全包围
                        break;
                    }
                }
                if (isSurrounded) {
                    newBoard[i][j] = board[i][j]; // 保留被完全包围的棋子
                }
            }
        }
        // 将所有未被完全包围的位置设为0（即腐蚀掉）
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (newBoard[i][j] == 0) {
                    board[i][j] = 0;
                }
            }
        }
    }

    // 简单的形势判断：计算黑子和白子的数量（膨胀和腐蚀后）
    private void evaluateSituation() {
        int blackCount = 0;
        int whiteCount = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == 1) {
                    blackCount++;
                } else if (board[i][j] == 2) {
                    whiteCount++;
                }
            }
        }
        System.out.println("Black count: " + blackCount);
        System.out.println("White count: " + whiteCount);
        // 可以根据黑子和白子的数量进行简单的形势判断，例如哪一方棋子更多
    }

    // 打印棋盘
    private void printBoard() {
        for (int[] row : board) {
            System.out.println(Arrays.toString(row));
        }
    }

    public static void main(String[] args) {
        GoBoardEvaluation goBoard = new GoBoardEvaluation();
        // 初始化棋盘，例如放置一些黑子和白子
        goBoard.board[2][2] = 2;
        goBoard.board[3][3] = 1; // 黑子
        goBoard.board[15][15] = 1;
        goBoard.board[3][15] = 2; // 白子
        goBoard.board[15][6] = 2;
        goBoard.board[15][3] = 2;

        System.out.println("Original board:");
        goBoard.printBoard();

        // 进行膨胀操作
        goBoard.dilate();
        goBoard.dilate();
        goBoard.dilate();
        //goBoard.dilate();
        //goBoard.dilate();
        //goBoard.dilate();
        //goBoard.dilate();

        System.out.println("After dilation:");
        goBoard.printBoard();

        // 进行腐蚀操作
        goBoard.erode();
        goBoard.erode();
        //goBoard.erode();
        //goBoard.erode();
        //goBoard.erode();
        //goBoard.erode();
        //goBoard.erode();
        //goBoard.erode();
        //goBoard.erode();
        //goBoard.erode();


        System.out.println("After erosion:");
        goBoard.printBoard();

        // 进行形势判断
        goBoard.evaluateSituation();
    }
}
