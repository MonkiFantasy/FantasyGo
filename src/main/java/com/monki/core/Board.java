package com.monki.core;

import com.monki.draw.MyPanel;
import com.monki.entity.Position;
import com.monki.entity.Stone;
import com.monki.util.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Board {

    public static Stone[][] stones=new Stone[Config.PATH+1][Config.PATH+1];//存放棋盘上落得子
    public static int[][] state=new int[Config.PATH+1][Config.PATH+1];//棋盘上落子的状态 -1黑 1白 0空
    //public static int[][] liberty=new int[Config.PATH+1][Config.PATH+1];//棋盘上的棋子的气数
    public static List<StoneString> blackString=new ArrayList<>();
    public static List<StoneString> whiteString=new ArrayList<>();
    public static List<int[][]> history=new ArrayList<>();//棋盘历史状态

    //根据当前落子的坐标，判断上下左右的敌方棋串是否气为0,返回需要被移除的棋串
    public static List<StoneString> getStonesToRemove(Position index,int player) {
        int i = index.getJ();
        int j = index.getI();
        Position up=new Position(i-1,j);
        Position down=new Position(i+1,j);
        Position left=new Position(i,j-1);
        Position right=new Position(i,j+1);

        ArrayList<StoneString> stringList = new ArrayList<>();
        if(isOppositeStringDeath(up,player)){
            stringList.add(getStoneStringByIndex(up));
        }
        if (isOppositeStringDeath(down,player)){
            stringList.add(getStoneStringByIndex(down));
        }
        if(isOppositeStringDeath(left,player)){
            stringList.add(getStoneStringByIndex(left));
        }
        if (isOppositeStringDeath(right,player)){
            stringList.add(getStoneStringByIndex(right));
        }
            //stringlist=null;

        return stringList;
    }

    //判断当前位置的敌方棋串是否为死串
    public static Boolean isOppositeStringDeath(Position index,int player){
        return isOnBoard(index)&&state[index.getI()][index.getJ()]==-player
                &&stones[index.getI()][index.getJ()].getMyString().getLiberty()==0;
    }

    //获得当前棋子位置的棋串（当前位置要有棋子）
    public static StoneString getStoneStringByIndex(Position index){
        return stones[index.getI()][index.getJ()].getMyString();
    }
    //当前落子后连接棋串
    public static void connectString(Stone stone,int player){
        Position index=stone.getIndex();
        int i = index.getJ();
        int j = index.getI();
        Position up=new Position(i-1,j);
        Position down=new Position(i+1,j);
        Position left=new Position(i,j-1);
        Position right=new Position(i,j+1);

        //TODO：连接棋串，判断上下左右是否有当前方棋串，没有就把当前棋子作为一个子串，存入当前方StoneString的list
        StoneString mergedString = new StoneString();
        if(existStone(up,player)){
            StoneString u = getStoneStringByIndex(up);
            mergedString.addStone(stone);
            mergedString.addStones(u.getStones());
            boolean b = player == -1 ? blackString.remove(u) : whiteString.remove(u);
        }
        if (existStone(down,player)) {
            StoneString d = getStoneStringByIndex(down);
            mergedString.addStone(stone);
            mergedString.addStones(d.getStones());
            boolean b = player == -1 ? whiteString.remove(d) : blackString.remove(d);
        }
        if (existStone(left,player)) {
            StoneString l = getStoneStringByIndex(left);
            mergedString.addStone(stone);
            mergedString.addStones(l.getStones());
            boolean b = player == -1 ? whiteString.remove(l) : blackString.remove(l);
        }
        if (existStone(right,player)) {
            StoneString r = getStoneStringByIndex(right);
            mergedString.addStone(stone);
            mergedString.addStones(r.getStones());
            boolean b = player == -1 ? whiteString.remove(r) : blackString.remove(r);
        }
        mergedString.addStone(stone);


        //更新当前stone所在的棋串
        stones[i][j].setMyString(mergedString);
        MyPanel.fallOn.add(stone);
        //todo:更新当前串前面棋子所在的串
        Set<Stone> stonesOfString = mergedString.getStones();
        for (Stone stoneOfString : stonesOfString) {
            stoneOfString.setMyString(mergedString);
            MyPanel.fallOn.set(stoneOfString.getCount()-1,stoneOfString);
        }
        //MyPanel.fallOn
        //存入对应list
        if (player==-1){
            blackString.add(mergedString);
        } else if (player==1) {
            whiteString.add(mergedString);
        }

    }
    public static Boolean existStone(Position index,int player){
        return isOnBoard(index)&&state[index.getI()][index.getJ()]==player;
    }
    public static boolean isOnBoard(Position index) {
        return index.getI()>=1&&index.getI()<=19&&index.getJ()>=1&&index.getJ()<=19;
    }
  /*  public void checkAirOfStone(Stone stone){

    }
    public void checkAirOfStoneString(StoneString stoneString){

    }*/



}
