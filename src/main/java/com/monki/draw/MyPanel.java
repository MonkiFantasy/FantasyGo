package com.monki.draw;

import com.monki.core.Board;
import com.monki.core.StoneString;
import com.monki.socket.StoneClient;
import com.monki.socket.StoneServer;
import com.monki.util.FileSaver;
import com.monki.util.Calculator;
import com.monki.util.Config;
import com.monki.entity.Position;
import com.monki.entity.Stone;
import com.monki.util.MyLogger;
import com.monki.socket.GoClient;
import com.monki.util.KataGoRunner;
import com.monki.util.KataGoEvaluator;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.BasicStroke;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.io.File;

public class MyPanel extends JPanel {
    public static final int X = Config.X;//棋盘左上角顶点x坐标
    public static final int Y = Config.Y;//棋盘左上角顶点y坐标
    public static final int LENGTH = Config.LENGTH;//棋盘宽度
    //public static final int PATH=Config.PATH;//围棋路数
    public static final int SPACE = Config.SPACE;
    public static MyPaint myPaint = new MyPaint();
    public static Position mouseOn = null;//鼠标指针距离最近交叉点坐标
    public static List<Stone> fallOn = new ArrayList<>();//已落子的信息
    public static int turn = -1;//-1黑 1白
    public static int count = 1;//对弈手数
    public static Stone lastStone = null; // 记录最后一颗落子，用于焦点绘制
    //Logger logger =Logger.getLogger("panel");
    //private Board board= new Board();
    //private static Stone[][] stones=Board.stones;//存放棋盘上落得子
    private JButton menu;
    private  JButton musicPlayer;
    private JButton saveSGF;
    private JButton situationJudgment; // 新增形势判断按钮
    private JPanel textPanel;
    private static JTextArea text;
    private Clip clip;
    private JFrame myFrame;
    private boolean showSituation = false; // 控制是否显示形势判断
    private KataGoRunner kataGoRunner;
    private String lastSgfPath;
    private File tempDir;

    public MyPanel(JFrame frame) {
        myFrame = frame;
        initMusic();
        initPanel();
        initListener();
        
        // 创建临时目录
        tempDir = new File(System.getProperty("user.home"), "fantasy_go_temp");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        
        // 初始化KataGo运行器
        kataGoRunner = new KataGoRunner();
        try {
            kataGoRunner.startKataGo();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("KataGo启动失败：" + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "KataGo启动失败：" + e.getMessage(), 
                "错误", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void finalize() {
        if (kataGoRunner != null) {
            kataGoRunner.stopKataGo();
        }
        // 清理临时文件
        if (tempDir != null && tempDir.exists()) {
            for (File file : tempDir.listFiles()) {
                file.delete();
            }
            tempDir.delete();
        }
    }

    @Override
    public void paintComponent(Graphics gh) {
        super.paintComponent(gh);
        Graphics2D g = (Graphics2D) gh;
        //background
        try {
            Image image = ImageIO.read(getClass().getResource("/img/img.png"));
            g.drawImage(image,0,0,getWidth(),getHeight(),this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //棋盘绘制
        myPaint.drawLines(g);
        myPaint.drawIndex(g);
        myPaint.drawStars(g);
        
        // 如果启用了形势判断，则绘制形势
        if (showSituation) {
            // 绘制领地和死子标记
            for (int i = 1; i <= Config.PATH; i++) {
                for (int j = 1; j <= Config.PATH; j++) {
                    Position position = Calculator.getCoordinateViaIndex(j, i);
                    Color territoryColor = com.monki.util.PositionEvaluator.getTerritoryColor(i, j);
                    if (territoryColor != null) {
                        g.setColor(territoryColor);
                        g.fillRect(position.getI() - Config.SPACE/4, position.getJ() - Config.SPACE/4, 
                                  Config.SPACE/2, Config.SPACE/2);
                    }
                }
            }
            
            // 绘制最佳落子点
            Position bestMove = com.monki.util.PositionEvaluator.getBestMove();
            if (bestMove != null) {
                Position bestMoveCoord = Calculator.getCoordinateViaIndex(bestMove.getI(), bestMove.getJ());
                g.setColor(new Color(0, 255, 0, 180)); // 半透明绿色
                g.setStroke(new BasicStroke(3.0f)); // 加粗线条
                g.drawOval(bestMoveCoord.getI() - Config.SPACE/3, bestMoveCoord.getJ() - Config.SPACE/3, 
                          2*Config.SPACE/3, 2*Config.SPACE/3);
                
                // 保存当前画笔颜色和线型
                Stroke oldStroke = ((Graphics2D)g).getStroke();
                
                // 恢复默认线型
                g.setStroke(oldStroke);
            }
        }
        
        //实现落子提示效果
        if (mouseOn != null) {
            g.setColor(Color.RED);
            g.fillRect(mouseOn.getI() - Config.SPACE / 4, mouseOn.getJ() - Config.SPACE / 4, SPACE/2, SPACE/2);
        }
        //落子实现
        if (!fallOn.isEmpty()) {
            //g.setColor(Color.BLACK);
            //Boolean isBlack = true;
            for (Stone stone : fallOn) {
                if (!stone.getRemoved()) {
                    myPaint.drawStone(g, stone);
                    
                    // 修改落子焦点绘制逻辑，解决网络模式下的焦点显示问题
                    // 焦点显示在最后一颗棋子上，无论是本地落下的还是从网络接收的
                    if (lastStone != null && stone.getCount() == lastStone.getCount()) {
                        int i = stone.getCoordinate().getI();
                        int j = stone.getCoordinate().getJ();
                        g.setColor(stone.getColor().equals(Color.WHITE) ? Color.BLACK : Color.WHITE);
                        g.fillPolygon(new int[]{i, i, i+Config.SPACE/2}, new int[]{j, j+Config.SPACE/2, j}, 3);
                    }
                }
            }
        }
    }

    private class MyMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            //鼠标按下落子
            if (Calculator.isInBoard(e.getX(), e.getY())) {
                Position index = Calculator.getIndexViaMouse(e.getX(), e.getY());
                Position coordinate = Calculator.getCoordinateViaIndex(index.getI(), index.getJ());
                Stone stone = new Stone(count, turn == -1 ? Color.BLACK : Color.WHITE, coordinate, index);
                
                //本地对弈模式
                if(Config.MODE == 0){
                    //判断落子是否合法(重复落子，自杀，全局同形）
                    if (isValidStone(index, stone)) {
                        //落子合法，执行落子逻辑
                        updateStone(stone);
                        repaint();
                    }
                }
                //网络对弈模式 - 使用新的GoClient
                else if(Config.MODE == 1){
                    // 获取GoClient实例
                    GoClient goClient = MyFrame.connectPanel.getGoClient();
                    
                    // 检查棋子是否合法
                    if (isValidStone(index, stone)) {
                        // 更新本地棋盘状态
                        updateStone(stone);
                        
                        // 发送落子消息到服务器
                        goClient.sendMove(stone);
                        
                        // 重绘棋盘
                        repaint();
                    }
                }

                MyLogger.log("count" + count, this.getClass());

                //重绘当前落下棋子区域
                repaint(coordinate.getI() - Config.SPACE / 2, coordinate.getJ() - Config.SPACE / 2, SPACE * 2, SPACE * 2);
                
                //日志输出
                showBoardState();
                showFallon();
            }
        }
    }

    public static void updateStone(Stone stone) {
        try {
            Position index = stone.getIndex();
            boolean debug = false; // 控制详细日志输出
            
            // 快速验证石头有效性
            if (index.getI() < 0 || index.getI() > Config.PATH || 
                index.getJ() < 0 || index.getJ() > Config.PATH) {
                System.out.println("无效的棋子位置: " + index.getI() + "," + index.getJ());
                return;
            }
            
            // 设置当前回合 - 根据棋子颜色设置
            int stoneTurn = stone.getColor().equals(Color.BLACK) ? -1 : 1;
            
            // 1. 更新棋盘状态 - 优先处理核心逻辑
            Board.stones[index.getJ()][index.getI()] = stone;
            Board.state[index.getJ()][index.getI()] = stoneTurn;
            
            // 2. 更新气相关信息 - 对石头检查气
            stone.checkLiberty();
            
            // 3. 更新敌方棋串的气坐标 - 批量处理
            List<StoneString> opponentStrings = (stoneTurn == -1) ? Board.whiteString : Board.blackString;
            for (StoneString stoneString : opponentStrings) {
                stoneString.updateLiberty(index);
            }
            
            // 4. 处理提子
            removeOppositeDeathString(index, stoneTurn);
            
            // 5. 再次更新当前棋子气数
            stone.checkLiberty();
            
            // 6. 连接棋串和更新历史
            Board.connectString(stone, stoneTurn);
            Board.history.add(Calculator.deepCopy(Board.state));
            
            // 7. 更新UI文本
            if(text != null) {
                text.setText("请"+(stoneTurn==-1?"白":"黑")+"方落子 当前手数："+(stone.getCount()+1));
            }
            
            // 8. 更新回合和手数
            if (Config.MODE == 0) {
                // 本地模式下切换回合并增加手数
                turn = -turn;
                count++;
            } else {
                // 网络模式下根据当前棋子更新
                turn = -stoneTurn;
                count = stone.getCount() + 1;
            }
            
            // 9. 添加到落子历史
            if (fallOn.size() < stone.getCount()) {
                // 填充缺失的石头
                while (fallOn.size() < stone.getCount() - 1) {
                    fallOn.add(null);
                }
                fallOn.add(stone);
            } else if (fallOn.size() >= stone.getCount()) {
                // 更新现有石头
                while (fallOn.size() <= stone.getCount() - 1) {
                    fallOn.add(null);
                }
                fallOn.set(stone.getCount() - 1, stone);
            }
            
            // 10. 更新最后一颗落子引用，用于焦点绘制
            lastStone = stone;
            
        } catch (Exception e) {
            System.out.println("棋盘更新出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理落子后可能产生的提子操作
     * 
     * @param index 当前落子的位置
     * @param player 当前落子的玩家（-1表示黑，1表示白）
     */
    private static void removeOppositeDeathString(Position index, int player) {
        // 获取落子后气为0的对方棋串列表
        List<StoneString> deadStrings = Board.getStonesToRemove(index, player);
        
        if (!deadStrings.isEmpty()) {
            // 遍历每个应该被提子的棋串
            for (StoneString deadString : deadStrings) {
                Set<Stone> stones = deadString.getStones();
                
                // 处理棋串中的每个棋子
                for (Stone stone : stones) {
                    // 标记棋子为已提走
                    stone.setRemoved(true);
                    
                    // 更新UI记录中的棋子状态
                    fallOn.set(stone.getCount() - 1, stone);
                    System.out.println(stone);
                    
                    // 清除棋盘数据结构中的记录
                    Board.state[stone.getIndex().getJ()][stone.getIndex().getI()] = 0;
                    Board.stones[stone.getIndex().getJ()][stone.getIndex().getI()] = null;
                }
                
                // 从棋盘的棋串集合中移除该死亡棋串
                if (player == -1) {
                    Board.whiteString.remove(deadString); // 黑方落子，移除白方棋串
                } else if (player == 1) {
                    Board.blackString.remove(deadString); // 白方落子，移除黑方棋串
                }
            }
        }
    }

    private void showFallon() {
        StringBuilder msg = new StringBuilder();
        for (Stone stone1 : fallOn) {
            msg.append(stone1);
        }
        MyLogger.log(msg.toString(), this.getClass());
    }

    private void showBoardState() {
        MyLogger.log("Board.state[][]", this.getClass());
        StringBuilder boardState = new StringBuilder();
        for (int i = 1; i <= 19; i++) {
            for (int j = 1; j <= 19; j++) {
                boardState.append(Board.state[i][j]).append(" ");
                //System.out.println(Board.stones[i][j]);
            }
            boardState.append("\n");
        }
        MyLogger.log(boardState.toString(), this.getClass());
    }

    private boolean isValidStone(Position index, Stone stone) {
        System.out.println("\n===== 验证落子 =====");
        System.out.println("落子位置: (" + index.getI() + "," + index.getJ() + ")");
        System.out.println("落子颜色: " + (stone.getColor().equals(Color.BLACK) ? "黑" : "白"));
        System.out.println("当前轮次: " + (turn == -1 ? "黑方" : "白方"));
        System.out.println("当前模式: " + (Config.MODE == 0 ? "本地模式" : "网络模式"));
        
        if (Config.MODE == 1) {
            System.out.println("我的角色: " + (Config.SERVER ? "创建者(黑)" : "加入者(白)"));
            System.out.println("是否我的回合: " + ((turn == -1 && Config.SERVER) || (turn == 1 && !Config.SERVER)));
            
            // 在网络模式下验证是否是当前玩家的回合
            if((stone.getColor().equals(Color.WHITE) && Config.SERVER) || 
               (stone.getColor().equals(Color.BLACK) && !Config.SERVER)) {
                MyLogger.log("不是你的回合", this.getClass());
                System.out.println("不是你的回合 - 角色与棋子颜色不匹配");
                new WarningDialog("不是你的回合，请等待对方落子");
                return false;
            }
        }
        
        // 当前坐标已落子
        if (Board.state[index.getJ()][index.getI()] != 0) {
            MyLogger.log("当前坐标已落子，请到别处落子", this.getClass());
            System.out.println("验证失败 - 位置已有棋子");
            new WarningDialog("当前坐标已落子，请到别处落子");
            return false;
        }
        
        // 判断是否自杀
        if (isSuicide(index, turn, stone)) {
            MyLogger.log("你不能自杀", this.getClass());
            System.out.println("验证失败 - 自杀规则");
            new WarningDialog("你不能自杀，请到别处落子");
            return false;
        }
        
        // 判断是否全局同形
        if (isAppeared(index)) {
            MyLogger.log("违反了禁全局同形规则，请在别处落子", this.getClass());
            System.out.println("验证失败 - 全局同形规则");
            new WarningDialog("违反了禁全局同形规则，\n请到别处落子");
            return false;
        }
        
        // 落子有效，说明棋局未终止
        Config.GAMESTATUS = 1;
        System.out.println("验证通过 - 落子有效");
        System.out.println("===== 验证结束 =====\n");
        return true;
    }

    private boolean isAppeared(Position index) {
        int[][] clone = Calculator.deepCopy(Board.state);
        int i = index.getJ();
        int j = index.getI();
        
        //真实棋盘落子（后面恢复）
        Board.state[i][j] = turn;
        //模拟棋盘落子
        clone[i][j] = turn;
        
        //落子后更新敌方棋串的气坐标
        if (turn == -1) {
            for (StoneString stoneString : Board.whiteString) {
                stoneString.updateLiberty(index);
            }
        } else if (turn == 1) {
            for (StoneString stoneString : Board.blackString) {
                stoneString.updateLiberty(index);
            }
        }
        
        //落子后模拟提子
        List<StoneString> stoneStrings = Board.getStonesToRemove(index, turn);
        if (!stoneStrings.isEmpty()) {
            //找到被提的棋串，设置模拟棋盘中被提掉的棋子
            stoneStrings.forEach(stoneString -> {
                Set<Stone> stones = stoneString.getStones();
                stones.forEach(s -> {
                    clone[s.getIndex().getJ()][s.getIndex().getI()] = 0;
                });
            });
        }
        
        //恢复真实棋盘状态
        Board.state[i][j] = 0;
        
        //检查是否违反全局同形
        for (int[][] ints : Board.history) {
            if(Calculator.areArraysEqual(clone, ints)){
                return true;
            }
        }
        return false;
    }

    //修改自杀逻辑，先判断是否能提子，如果能则不算自杀，判断落子后当前棋串的气是否为0，为0则算自杀
    private boolean isSuicide(Position index, int player, Stone stone) {
        int[][] clone = Calculator.deepCopy(Board.state);
        int i = index.getJ();
        int j = index.getI();
        Position up = new Position(i-1, j);
        Position down = new Position(i+1, j);
        Position left = new Position(i, j-1);
        Position right = new Position(i, j+1);
        
        //真实棋盘落子（后面恢复）
        Board.state[i][j] = turn;
        
        //落子后更新敌方棋串的气坐标
        if (turn == -1) {
            for (StoneString stoneString : Board.whiteString) {
                stoneString.updateLiberty(index);
            }
        } else if (turn == 1) {
            for (StoneString stoneString : Board.blackString) {
                stoneString.updateLiberty(index);
            }
        }
        
        //落子后模拟提子
        List<StoneString> stoneStrings = Board.getStonesToRemove(index, turn);
        
        //恢复棋盘状态
        Board.state[i][j] = 0;
        
        if (!stoneStrings.isEmpty()) {
            //能提子，不算自杀
            return false;
        } else {
            //连接棋串，判断上下左右是否有当前方棋串
            StoneString mergedString = new StoneString();
            if(Board.existStone(up, player)){
                StoneString u = Board.getStoneStringByIndex(up);
                mergedString.addStone(stone);
                mergedString.addStones(u.getStones());
            }
            if (Board.existStone(down, player)) {
                StoneString d = Board.getStoneStringByIndex(down);
                mergedString.addStone(stone);
                mergedString.addStones(d.getStones());
            }
            if (Board.existStone(left, player)) {
                StoneString l = Board.getStoneStringByIndex(left);
                mergedString.addStone(stone);
                mergedString.addStones(l.getStones());
            }
            if (Board.existStone(right, player)) {
                StoneString r = Board.getStoneStringByIndex(right);
                mergedString.addStone(stone);
                mergedString.addStones(r.getStones());
            }
            mergedString.addStone(stone);
            
            // 如果气数为0，则为自杀
            return mergedString.getLiberty() == 0;
        }
    }

    private void initPanel() {
        setLayout(null);
        setBounds(0, 0, 1920, 1080);
        setBackground(Color.gray);
        
        // 计算基础位置 - 棋盘右边缘
        int rightEdgePosition = X + LENGTH + SPACE;
        
        // 设置主菜单按钮位置
        menu = new MyButton("主菜单");
        menu.setBounds(rightEdgePosition, Y, SPACE * 5, (int) (SPACE*1.2));
        
        // 设置文本面板位置
        textPanel = new BackgroundPanel("/img/img_1.png");
        textPanel.setToolTipText("15351");
        textPanel.setBorder(BorderFactory.createLineBorder(Color.blue));
        textPanel.setBounds(rightEdgePosition, Y+SPACE*4, SPACE * 10, SPACE*12);
        
        // 设置文本区域
        text = new JTextArea();
        text.setBackground(new Color(0, 0, 0, 0));
        text.setText("执黑先行");
        text.setFont(new Font("宋体", Font.BOLD, 20));
        
        // 设置音乐播放按钮位置
        musicPlayer = new MyButton("背景音乐：关");
        musicPlayer.setBounds(rightEdgePosition, Y+SPACE*2, SPACE * 5, (int) (SPACE*1.2));
        
        // 设置保存棋谱按钮位置
        saveSGF = new MyButton("保存棋谱");
        saveSGF.setBounds(rightEdgePosition + SPACE*6, Y, SPACE * 5, (int)(SPACE*1.2));
        
        // 设置形势判断按钮位置
        situationJudgment = new MyButton("形势判断");
        situationJudgment.setBounds(rightEdgePosition + SPACE*6, Y+SPACE*2, SPACE * 5, (int)(SPACE*1.2));
        
        // 添加组件
        setDoubleBuffered(true);
        add(menu);
        add(musicPlayer);
        add(saveSGF);
        add(situationJudgment); // 添加形势判断按钮
        textPanel.add(text);
        add(textPanel);
        setVisible(true);
    }
    public void initMusic(){
        try {
            System.out.println("当前工作目录：" + System.getProperty("user.dir"));
            BufferedInputStream inputStream = new BufferedInputStream(this.getClass().getResourceAsStream("/music/Go.wav"));
            if (inputStream == null) {
                System.err.println("警告：找不到音频文件，将禁用音乐功能");
                return;
            }
            
            AudioInputStream bgm = AudioSystem.getAudioInputStream(inputStream);
            clip = AudioSystem.getClip();
            clip.open(bgm);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
            ex.printStackTrace();
            System.err.println("警告：初始化音频失败，将禁用音乐功能");
            // 禁用音乐播放按钮
            if (musicPlayer != null) {
                musicPlayer.setEnabled(false);
                musicPlayer.setToolTipText("音频功能不可用");
            }
        }
    }
    private void initListener() {

        //鼠标点击监听（落子监听）
        addMouseListener(new MyMouseListener());
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                //鼠标移动落子位置高亮提示
                if (Calculator.isInBoard(e.getX(), e.getY())) {
                    mouseOn = Calculator.getIndexViaMouse(e.getX(), e.getY());
                    mouseOn = Calculator.getCoordinateViaIndex(mouseOn.getI(), mouseOn.getJ());
                    repaint();
                }
            }
        });
        //弹出连接对话框
        menu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //修改：使用确认对话框询问是否退出并清空棋盘状态
                if(Config.GAMESTATUS==1){
                    new ConfirmDialog("棋局尚未结束，确定要退出吗？", event -> {
                        clearBoardState();
                        // 返回主菜单，窗口尺寸恢复为500x500
                        ((MyFrame)myFrame).switchPanel(MyFrame.myPanel, MyFrame.startPanel, 500, 500);
                    });
                } else {
                    // 返回主菜单，窗口尺寸恢复为500x500
                    ((MyFrame)myFrame).switchPanel(MyFrame.myPanel, MyFrame.startPanel, 500, 500);
                }
            }
        });
        musicPlayer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JButton source = (JButton) e.getSource();
                if (clip == null) {
                    source.setToolTipText("音频功能不可用");
                    return;
                }
                
                if (source.getText().equals("背景音乐：开")) {
                    clip.stop();
                    source.setToolTipText("点击播放");
                    source.setText("背景音乐：关");
                } else if (source.getText().equals("背景音乐：关")) {
                    clip.start();
                    source.setText("背景音乐：开");
                    source.setToolTipText("点击暂停");
                }
            }
        });
        saveSGF.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                StringBuilder sb = new StringBuilder();
                sb.append("(;GM[1]FF[4]CA[UTF-8]SZ[19];\n");
                for (Stone stone : fallOn) {
                    if (stone.getColor().equals(Color.BLACK)) {
                        sb.append("B[");
                        //sb.append(Calculator.getCoordinateViaIndex(stone.getIndex().getI(), stone.getIndex().getJ()));
                        sb.append(Calculator.getAlphaIndex(stone.getIndex()));
                        sb.append("]");
                    } else if (stone.getColor().equals(Color.WHITE)) {
                        sb.append("W[");
                        sb.append(Calculator.getAlphaIndex(stone.getIndex()));
                        sb.append("]");
                        //sb.append(Calculator.getCoordinateViaIndex(stone.getIndex().getI(), stone.getIndex().getJ()));
                    }
                    if(stone.getCount()!=fallOn.size()){
                       sb.append(";");
                    }
                }
                sb.append(")");
                try {
                    FileSaver.save(sb.toString());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }


            }
        });
        
        // 添加形势判断按钮的事件监听器
        situationJudgment.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 切换形势判断显示状态
                showSituation = !showSituation;
                
                if (showSituation) {
                    // 使用KataGo分析当前局面
                    analyzeWithKataGo();
                    
                    // 获取形势评估结果
                    Map<String, Object> result = com.monki.util.PositionEvaluator.evaluatePosition();
                    
                    // 显示评估结果
                    double scoreDiff = (double) result.get("scoreDiff");
                    int blackTerritory = (int) result.get("blackTerritory");
                    int whiteTerritory = (int) result.get("whiteTerritory");
                    int blackCaptures = (int) result.get("blackCaptures");
                    int whiteCaptures = (int) result.get("whiteCaptures");
                    double whiteWinrate = (double) result.get("whiteWinrate");
                    double whiteLead = (double) result.get("whiteLead");
                    
                    // 更新文本显示
                    text.setText(String.format(
                        "KataGo评估：\n" +
                        "胜率：白%.1f%% 黑%.1f%%\n" +
                        "目差：%.1f目\n\n" +
                        "黑方领地：%d 目\n白方领地：%d 目\n" +
                        "黑方提子：%d 子\n白方提子：%d 子\n" +
                        "得分差：%.2f 目\n%s",
                        whiteWinrate * 100, (1 - whiteWinrate) * 100,
                        whiteLead,
                        blackTerritory, whiteTerritory, 
                        blackCaptures, whiteCaptures, 
                        scoreDiff,
                        scoreDiff > 0 ? "黑方领先" : "白方领先"
                    ));
                    
                    situationJudgment.setText("关闭形势判断");
                } else {
                    // 恢复原始显示
                    text.setText("请" + (turn == -1 ? "黑" : "白") + "方落子 当前手数：" + count);
                    situationJudgment.setText("形势判断");
                }
                
                // 重绘棋盘以显示或隐藏形势判断
                repaint();
            }
        });
    }
    
    /**
     * 清空棋盘状态
     * 重置所有与棋局相关的变量和集合
     */
    private void clearBoardState() {
        // 清空已落子信息
        fallOn.clear();
        
        // 重置轮次和手数
        turn = -1; // 重置为黑方先行
        count = 1; // 重置手数
        
        // 重置最后落子引用
        lastStone = null;
        
        // 清空棋盘状态
        for (int i = 0; i <= Config.PATH; i++) {
            for (int j = 0; j <= Config.PATH; j++) {
                Board.state[i][j] = 0;
                Board.stones[i][j] = null;
            }
        }
        
        // 清空棋串
        Board.blackString.clear();
        Board.whiteString.clear();
        
        // 清空历史记录
        Board.history.clear();
        
        // 重置鼠标位置
        mouseOn = null;
        
        // 重置游戏状态
        Config.GAMESTATUS = 0;
        
        // 更新文本区域
        text.setText("执黑先行");
        
        // 重绘棋盘
        repaint();
    }

    private void analyzeWithKataGo() {
        try {
            // 生成临时SGF文件
            StringBuilder sb = new StringBuilder();
            sb.append("(;GM[1]FF[4]CA[UTF-8]SZ[19];\n");
            for (Stone stone : fallOn) {
                if (!stone.getRemoved()) {  // 只保存未被提走的棋子
                    if (stone.getColor().equals(Color.BLACK)) {
                        sb.append("B[");
                        sb.append(Calculator.getAlphaIndex(stone.getIndex()));
                        sb.append("]");
                    } else if (stone.getColor().equals(Color.WHITE)) {
                        sb.append("W[");
                        sb.append(Calculator.getAlphaIndex(stone.getIndex()));
                        sb.append("]");
                    }
                    if(stone.getCount() != fallOn.size()) {
                        sb.append(";");
                    }
                }
            }
            sb.append(")");

            // 保存SGF文件
            lastSgfPath = new File(tempDir, "temp_analysis.sgf").getAbsolutePath();
            try (FileWriter writer = new FileWriter(lastSgfPath)) {
                writer.write(sb.toString());
            }

            // 使用KataGo分析
            String analysis = kataGoRunner.analyzeSgf(lastSgfPath);
            if (analysis != null) {
                // 处理分析结果
                KataGoEvaluator.processKataGoOutput(analysis);
            } else {
                throw new Exception("KataGo返回空结果");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("KataGo分析失败：" + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "KataGo分析失败：" + e.getMessage(), 
                "错误", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
