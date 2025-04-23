package com.monki.entity;

import java.awt.*;
import java.io.Serializable;
import java.util.Objects;

public class Position implements Serializable {
    private static final long serialVersionUID = 2L;
    private int i;
    private int j;
    private Color color;
    public Position(int i, int j) {
        this.i = i;
        this.j = j;
    }

    public Position(int i, int j, Color color) {
        this.i = i;
        this.j = j;
        this.color = color;
    }
    public int getI() {
        return i;
    }

    public int getJ() {
        return j;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return i == position.i && j == position.j;
    }

    @Override
    public int hashCode() {
        return Objects.hash(i, j); // 使用 java.util.Objects 的 hash 方法
    }
    @Override
    public String toString() {
        return "(" + i + ", " + j+")";
    }
}
