package com.monki.util;

import com.monki.entity.Position;

public class Calculator {

    //通过鼠标指针坐标获得围棋落子坐标
    public static Position getIndexViaMouse(int mouseX, int mouseY){
        //计算i
        int i = (mouseX-Config.X+Config.SPACE/2)/Config.SPACE+1;
        //计算j
        int j = (mouseY-Config.Y+Config.SPACE/2)/Config.SPACE+1;
        return new Position(i, j);
    }

    //根据交叉点索引获得实际坐标
    public static Position getCoordinateViaIndex(int i, int j){
        int posX = Config.X+(i-1)*Config.SPACE;
        int posY = Config.Y+(j-1)*Config.SPACE;
        return new Position(posX,posY);

    }

    // 辅助方法，检查鼠标是否在棋盘区域内
    public static boolean isInBoard(int x, int y) {

        return x >= Config.X - Config.SPACE / 2 && x <= Config.X + Config.LENGTH + Config.SPACE / 2

                && y >= Config.Y - Config.SPACE / 2 && y <= Config.Y + Config.LENGTH + Config.SPACE / 2;

    }
    public static boolean areArraysEqual(int[][] array1, int[][] array2) {
        if (array1 == null && array2 == null) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }

        for (int i = 0; i < array1.length; i++) {
            if (array1[i] == null && array2[i] == null) {
                continue;
            }
            if (array1[i] == null || array2[i] == null || array1[i].length != array2[i].length) {
                return false;
            }
            for (int j = 0; j < array1[i].length; j++) {
                if (array1[i][j] != array2[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }
    public static int[][] deepCopy(int[][] original) {
        if (original == null) {
            return null;
        }

        int rows = original.length;
        int[][] copy = new int[rows][];

        for (int i = 0; i < rows; i++) {
            if (original[i] != null) {
                int cols = original[i].length;
                copy[i] = new int[cols];
                System.arraycopy(original[i], 0, copy[i], 0, cols);
            }
        }

        return copy;
    }
    public static String getAlphaIndex(Position index) {
        for (int i = 1; i <= 19; i++) {
            for (int j = 1; j <= 19; j++) {
                if (index.getI() == i && index.getJ() == j) {
                    return (char) ('a' + i - 1) +String.valueOf((char)('a'+j-1));
                }
            }

        }
        return "empty";
    }

}
