package com.monki.test;

import com.monki.entity.Position;

import java.util.HashSet;

public class CollectionTest {
    public static void main(String[] args) {
        //重写hashcode方法
        HashSet<Position> set1 = new HashSet<>();
        HashSet<Position> set2 = new HashSet<>();
        set1.add(new Position(1,1));
        set1.add(new Position(1,1));
        set1.add(new Position(1,3));
        set2.add(new Position(1,2));
        set2.add(new Position(2,3));
        //set1.retainAll(set2);
        System.out.println(set1);

    }
}
