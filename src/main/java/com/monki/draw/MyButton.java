package com.monki.draw;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class MyButton extends JButton {

    public MyButton(String text){

        setText(text);
        //setOpaque(true);
        //setContentAreaFilled(false);
        //setBorder(BorderFactory.createEmptyBorder());
        setFocusPainted(false);
        setFont(new Font("SimHei", Font.PLAIN, 25));
        //setOpaque(true);
    }

    @Override
    public void paintComponent(Graphics g) {

        try {
            BufferedImage image = ImageIO.read(getClass().getResource("/img/img_button.png"));
            //g.drawRoundRect();
            g.drawImage(image,0,0,getWidth(),getHeight(),this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // 绘制文本（如果需要）
        if (getText() != null && !getText().isEmpty()) {
            g.setColor(getForeground()); // 使用按钮的前景色来绘制文本
            FontMetrics fm = getFontMetrics(getFont());
            int x = (getWidth() - fm.stringWidth(getText())) / 2; // 文本水平居中
            int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent(); // 文本垂直居中
            g.setColor(new Color(131, 105, 195));
            g.drawString(getText(), x, y);
        }
        //super.paintComponent(g);

    }

    @Override
    protected void paintBorder(Graphics g) {
        g.setColor(new Color(131, 105, 195));
        g.drawRect(0,0,getWidth()-1,getHeight()-1);
    }
}
