package com.monki.core;

import com.monki.entity.Position;
import com.monki.entity.Stone;

import java.io.Serializable;
import java.util.*;

public class StoneString implements Serializable {
    private static final long serialVersionUID = 3L;
    //private enum player{white,black};//棋串属于哪一方
    private int liberty;//气的数量
    private Set<Stone> stones=new HashSet<>();//当前棋串的棋子
    private Set<Position> libertyPos=new HashSet<>();//当前气的位置
    public StoneString(){
    }

    public Set<Stone> getStones() {
        return stones;
    }
    
    /**
     * 获取当前棋串的气位置集合
     * @return 气位置的集合
     */
    public Set<Position> getLibertyPos() {
        return libertyPos;
    }

    public void addStones(Set<Stone> stones) {

        this.stones.addAll(stones);

        liberty=getLiberty();
    }

    public void addStone(Stone stone){

        stones.add(stone);

        liberty=getLiberty();
    }

    //依赖于棋串的棋子的气，要调用此方法，添加进来的棋子必须要已经添加了气的坐标
    public int getLiberty() {
        Set<Position> libertyOfString = new HashSet<>();
        libertyOfString.addAll(libertyPos);
        Set<Position> fallonIndex = new HashSet<>();
        for (Stone stone : stones) {
            Position index = stone.getIndex();
            index = new Position(index.getJ(),index.getI());
            fallonIndex.add(index);
        }

        //更新棋串气的时候检查棋子的气，重新保存棋子状态
        stones.forEach(stone -> {
            stone.checkLiberty();
            Board.stones[stone.getIndex().getJ()][stone.getIndex().getI()] = stone;
            libertyPos.addAll(stone.getLiberty());
        });
        int sizeOFLiberty=libertyPos.size();
        libertyOfString.retainAll(fallonIndex);
        //利用集合去重
        return sizeOFLiberty-libertyOfString.size();
    }
    //敌方落子时，移除当前气坐标中对方落子的位置
    public void updateLiberty(Position index){
        Position target = new Position(index.getJ(), index.getI());
        libertyPos.removeIf(libertyPo -> libertyPo.equals(target));
        this.liberty=getLiberty();//TODO:获取气的时候重置了libertyPos
        //重置棋盘状态
        //Board.stones[index.getJ()][index.getI()] = stone;

    }

    public String getLibertyString() {
        String s="";
        for (Stone stone : stones) {
            s+=stone.getLiberty();
        }
        return s;
    }

    public String toString() {
        return "串{" +
                "棋子数=" + stones.size() +
                "气=" + liberty +
                "气坐标" +getLibertyString() +
                '}';
    }
}
