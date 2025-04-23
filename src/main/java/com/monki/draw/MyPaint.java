package com.monki.draw;

import com.monki.util.Calculator;
import com.monki.util.Config;
import com.monki.entity.Position;
import com.monki.entity.Stone;

import java.awt.*;

public class MyPaint {
    public static final int X= Config.X;//棋盘左上角顶点x坐标
    public static final int Y=Config.Y;//棋盘左上角顶点y坐标
    public static final int LENGTH=Config.LENGTH;//棋盘宽度
    public static final int PATH=Config.PATH;//围棋路数
    public static final int SPACE=Config.SPACE;

    public void drawIndex(Graphics g) {
        char[] alphabet={'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S'};
        g.setColor(Color.BLACK);
        g.setFont(new Font("Default", Font.PLAIN, 15));
        for (int i = 0; i < 19; i++) {
            g.drawString("" + alphabet[i], Config.X -Config.SPACE/8+ Config.SPACE * i, Config.Y-Config.SPACE/8);
            g.drawString("" + (i + 1), Config.X - Config.SPACE/2, Config.Y + Config.SPACE / 8 + Config.SPACE * i);
        }
    }
    public void drawLines(Graphics g) {
        //棋盘绘制
        //g.setColor(new Color(242, 210, 17));
        g.setColor(Color.WHITE);
        g.fillRect(X-SPACE/2,Y-SPACE/2,LENGTH+SPACE,LENGTH+SPACE);
        g.setColor(Color.BLUE);
        //g.drawLine(X,Y+SPACE,X+LENGTH,Y+SPACE);
        //g.drawLine(X+SPACE,Y,X+SPACE,Y+LENGTH);

        for(int i=0;i<PATH;i++){
            g.drawLine(X,Y+SPACE*i,X+LENGTH,Y+SPACE*i);
            g.drawLine(X+SPACE*i,Y,X+SPACE*i,Y+LENGTH);

        }
        g.setColor(Color.BLACK);
    }
    public void drawStars(Graphics g) {

        //星位绘制
        Position position = Calculator.getCoordinateViaIndex(4, 4);
        g.fillOval(position.getI()-SPACE/10,position.getJ()-SPACE/10,SPACE/5,SPACE/5);
        position = Calculator.getCoordinateViaIndex(10, 4);
        g.fillOval(position.getI()-SPACE/10,position.getJ()-SPACE/10,SPACE/5,SPACE/5);
        position = Calculator.getCoordinateViaIndex(16, 4);
        g.fillOval(position.getI()-SPACE/10,position.getJ()-SPACE/10,SPACE/5,SPACE/5);
        position = Calculator.getCoordinateViaIndex(4, 10);
        g.fillOval(position.getI()-SPACE/10,position.getJ()-SPACE/10,SPACE/5,SPACE/5);
        position = Calculator.getCoordinateViaIndex(10, 10);
        g.fillOval(position.getI()-SPACE/10,position.getJ()-SPACE/10,SPACE/5,SPACE/5);
        position = Calculator.getCoordinateViaIndex(16, 10);
        g.fillOval(position.getI()-SPACE/10,position.getJ()-SPACE/10,SPACE/5,SPACE/5);
        position = Calculator.getCoordinateViaIndex(4, 16);
        g.fillOval(position.getI()-SPACE/10,position.getJ()-SPACE/10,SPACE/5,SPACE/5);
        position = Calculator.getCoordinateViaIndex(10, 16);
        g.fillOval(position.getI()-SPACE/10,position.getJ()-SPACE/10,SPACE/5,SPACE/5);
        position = Calculator.getCoordinateViaIndex(16, 16);
        g.fillOval(position.getI()-SPACE/10,position.getJ()-SPACE/10,SPACE/5,SPACE/5);
    }
    public void drawStone(Graphics2D g, Stone stone) {
        //圆形绘制设置抗锯齿渲染
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color[] whiteGradient = {Color.WHITE, Color.LIGHT_GRAY};
        Color[] blackGradient = {Color.LIGHT_GRAY,Color.BLACK};
        //设置渐变画笔
        RadialGradientPaint p = new RadialGradientPaint(
                stone.getCoordinate().getI()- (float) SPACE /6,stone.getCoordinate().getJ()- (float) SPACE /6 , (float) SPACE /3 ,
                new float[]{0.0f, 1.0f},
                stone.getColor().equals(Color.BLACK)?blackGradient:whiteGradient,
                RadialGradientPaint.CycleMethod.NO_CYCLE

        );
        g.setPaint(p);
        g.fillOval(stone.getCoordinate().getI()-Config.SPACE/2,stone.getCoordinate().getJ()-Config.SPACE/2,SPACE,SPACE);

    }
    public void drawLiberty(Graphics g, Position index) {
        //g.setColor(stone.getColor());
        index=Calculator.getCoordinateViaIndex(index.getJ(), index.getI());
        g.fillOval(index.getI()-Config.SPACE/4,index.getJ()-Config.SPACE/4,SPACE/2,SPACE/2);

    }


}
