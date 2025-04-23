package com.monki.entity;

import com.monki.core.Board;
import com.monki.core.StoneString;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import java.io.Serializable;
import java.util.Set;

public class Stone implements Serializable {
    private static final long serialVersionUID = 1L;
    private int count;//手数
    private Color color;//棋子颜色
    private Position coordinate;//棋子坐标
    private Position index;
    private StoneString myString;//
    private Set<Position> liberty=new HashSet<>();//保存气的坐标
    private Boolean isRemoved;


    public Stone(int count, Color color, Position coordinate, Position index) {
        this.count = count;
        this.color = color;
        this.coordinate = coordinate;
        this.index = index;
        this.isRemoved=false;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Position getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Position coordinate) {
        this.coordinate = coordinate;
    }

    public Position getIndex() {
        return index;
    }
    public void setIndex(Position index) {
        this.index = index;
    }

    public Set<Position> getLiberty() {
        return liberty;
    }

    public void setLiberty(Set<Position> liberty) {
        this.liberty = liberty;
    }

    public StoneString getMyString() {
        return myString;
    }
    public void setMyString(StoneString myString) {
        this.myString = myString;
    }

    public void setRemoved(Boolean removed) {
        this.isRemoved = removed;
    }

    public Boolean getRemoved() {
        return this.isRemoved;
    }
    private void addLiberity(Position index){
        liberty.add(index);
    }
    //TODO：检查前重置上次的气结果
    public void checkLiberty(){

        //更新气，不是只添加，只添加会造成下次检查气时保留上次的结果
        setLiberty(new HashSet<>());
        int i = index.getJ();
        int j = index.getI();
        Position up=new Position(i-1,j);
        Position down=new Position(i+1,j);
        Position left=new Position(i,j-1);
        Position right=new Position(i,j+1);
        if(checkBoardState(up,0)){
            addLiberity(up);
        }
        if (checkBoardState(down,0)){
            addLiberity(down);
        }
        if(checkBoardState(left,0)){
            addLiberity(left);
        }
        if (checkBoardState(right,0)){
            addLiberity(right);
        }

    }
    //index 在数组上的坐标 i 棋盘当前点的落子状态 -1黑 0空 1白
    private boolean checkBoardState(Position index, int state) {
        return isOnBoard(index)&&Board.state[index.getI()][index.getJ()]==state;
    }
    //判断当前位置是否在棋盘上
    private boolean isOnBoard(Position index) {
        return index.getI()>=1&&index.getI()<=19&&index.getJ()>=1&&index.getJ()<=19;
    }


    @Override
    public String toString() {
        String player =color.equals(Color.BLACK)?"黑":"白";
        return "棋子{" +count+
                ", 对局方=" + player+
                ", 图形学坐标=" + index +
                ", 属于的棋串=" + myString +
                ", \n剩余气数=" + liberty +
                ", 是否被提=" + isRemoved +
                '}'+"\n";
    }

}