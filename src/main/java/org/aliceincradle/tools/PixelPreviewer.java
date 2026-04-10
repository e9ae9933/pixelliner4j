package org.aliceincradle.tools;

import com.formdev.flatlaf.FlatDarkLaf;
import com.madgag.gif.fmsware.AnimatedGifEncoder;
import org.aliceincradle.pxlsloader.PxlCharacter;
import org.aliceincradle.pxlsloader.PxlPose;
import org.aliceincradle.pxlsloader.PxlSequence;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public class PixelPreviewer extends JPanel implements ActionListener {
    
    private final PxlCharacter chara;
    private final File workingDirectory;
    
    // --- 实例变量 ---
    // 使用原生的 Timer 替代野生的 TickRecorder 和死循环
    private final Timer gameLoopTimer = new Timer(16, this); // ~60fps
    private final Set<Integer> pressedKeys = new HashSet<>();
    private FrameAnimator animator;
    
    private int poseId = 0;
    private int sequenceId = 0;
    private PxlPose currentPose;
    private PxlSequence currentSequence;
    
    private boolean paused = false;
    private boolean debugBoxes = false;
    private String copyStateMsg = "";
    private int totalTicks = 0;
    private int mouseX, mouseY;
    private boolean mouseClicking;
    
    public PixelPreviewer(PxlCharacter chara, File workingDirectory) {
        this.chara = chara;
        this.workingDirectory = workingDirectory;
        
        setPreferredSize(new Dimension(800, 600));
        setBackground(new Color(43, 45, 48));
        setFocusable(true);
        requestFocusInWindow();
        
        loadSequence();
        setupInputListeners();
    }
    
    public static void main(String[] args) {
        // 1. 启用极其现代的暗色系 UI 外观
        FlatDarkLaf.setup();
        
        // 2. 严格确保 UI 的创建在 AWT 事件分发线程 (EDT) 中进行
        SwingUtilities.invokeLater(PixelPreviewer::createAndShowGUI);
    }
    
    private static void createAndShowGUI() {
        JFileChooser chooser = new JFileChooser();
        String userDir = System.getProperty("user.dir");
        if (userDir != null) {
            chooser.setCurrentDirectory(new File(userDir));
        }
        chooser.setDialogTitle("请选择一个 PixelLiner 文件 (.pxl / .pxls)");
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                String name = f.getName().toLowerCase();
                return f.isDirectory() || name.endsWith(".pxl") || name.endsWith(".pxls");
            }
            
            @Override
            public String getDescription() {
                return ".pxl, .pxls";
            }
        });
        
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            System.exit(0);
        }
        
        try {
            byte[] bytes = Files.readAllBytes(chooser.getSelectedFile().toPath());
            PxlCharacter chara = new PxlCharacter(bytes); // 依赖你的 PxlsLoader 纯数据解析
            
            JFrame frame = new JFrame("Alice In Cradle - 动画预览器");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            PixelPreviewer panel = new PixelPreviewer(chara, chooser.getSelectedFile()
                .getParentFile());
            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            
            panel.startGameLoop();
        } catch (Exception e) {
            showErrorDialog(e);
            System.exit(1);
        }
    }
    
    private static void showErrorDialog(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        JOptionPane.showMessageDialog(null, sw.toString(), "致命错误", JOptionPane.ERROR_MESSAGE);
    }
    
    private void loadSequence() {
        currentPose = chara.getPoseList().get(poseId);
        currentSequence = currentPose.getSequence(sequenceId);
        animator = new FrameAnimator(chara, currentSequence);
    }
    
    public void startGameLoop() {
        gameLoopTimer.start();
    }
    
    // --- 核心游戏主循环 (EDT 驱动) ---
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!paused) {
            animator.step();
        }
        totalTicks++;
        repaint(); // 触发 AWT 原生的双缓冲重绘
    }
    
    // --- 原生渲染管线 ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // 关键优化：关闭双线性插值，保持极其锐利的像素颗粒感
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                             RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // 渲染动画本体 (调用我们刚刚完善的 FrameAnimator)
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        animator.renderTo(g2d, cx, cy, 0, 1.0, 1.0, 1.0, debugBoxes);
        
        // 绘制 UI 文本信息
        drawHUD(g2d);
    }
    
    private void drawHUD(Graphics2D g2d) {
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        int y = 30;
        g2d.drawString("← / → : 切换序列 | D : 碰撞框 | Space : 暂停 | Enter : 逐帧 | R : 重置", 20, y);
        g2d.drawString("Ctrl+C : 复制姿势名 | Ctrl+S : 导出为 GIF", 20, y += 25);
        y += 30;
        
        g2d.drawString(String.format("Pose: %d / %d",
                                     poseId + 1, chara.getPoseList().size()), 20, y);
        g2d.drawString(String.format("Sequence: %d / %d",
                                     sequenceId + 1, currentPose.getSequenceCount()), 20, y += 25);
        g2d.drawString(animator.toString(), 20, y += 25);
        
        if (paused) {
            g2d.setColor(new Color(255, 200, 0));
            g2d.drawString("▶ 已暂停", 20, y += 25);
        }
        
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawString(String.format("Mouse: (%d, %d) %s", mouseX, mouseY,
                                     mouseClicking ? "[Down]" : ""), 20, y += 35);
        
        if (!copyStateMsg.isEmpty()) {
            g2d.setColor(copyStateMsg.contains("失败") ? new Color(255, 100, 100) :
                         new Color(100, 255, 100));
            g2d.drawString(copyStateMsg, 20, y += 25);
        }
    }
    
    // --- 极简输入处理 ---
    private void setupInputListeners() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                mouseClicking = true;
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                mouseClicking = false;
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                // 拦截长按导致的连发，只响应首次按下
                if (pressedKeys.add(code)) {
                    handleFirstPress(e);
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                pressedKeys.remove(e.getKeyCode());
            }
        });
    }
    
    private void handleFirstPress(KeyEvent e) {
        int code = e.getKeyCode();
        boolean ctrl = e.isControlDown();
        
        if (code == KeyEvent.VK_LEFT) {
            sequenceId--;
            if (sequenceId < 0) {
                poseId = (poseId - 1 + chara.getPoseList().size()) % chara.getPoseList().size();
                currentPose = chara.getPoseList().get(poseId);
                sequenceId = currentPose.getSequenceCount() - 1;
            }
            loadSequence();
        } else if (code == KeyEvent.VK_RIGHT) {
            sequenceId++;
            if (sequenceId >= currentPose.getSequenceCount()) {
                poseId = (poseId + 1) % chara.getPoseList().size();
                currentPose = chara.getPoseList().get(poseId);
                sequenceId = 0;
            }
            loadSequence();
        } else if (code == KeyEvent.VK_SPACE) {
            paused = !paused;
        } else if (code == KeyEvent.VK_D) {
            debugBoxes = !debugBoxes;
        } else if (code == KeyEvent.VK_R) {
            animator.reset();
        } else if (code == KeyEvent.VK_ENTER) {
            animator.stepFrame();
        } else if (ctrl && code == KeyEvent.VK_C) {
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(currentPose.getTitle()), null);
                copyStateMsg = "已复制: " + currentPose.getTitle();
            } catch (Exception ex) {
                copyStateMsg = "复制失败";
            }
        } else if (ctrl && code == KeyEvent.VK_S) {
            exportGifAsync();
        }
    }
    
    // --- 异步后台 GIF 导出 (工具绝不假死) ---
    private void exportGifAsync() {
        if (currentSequence == null || currentSequence.getFrameCount() == 0)
            return;
        
        // 冻结操作界面
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        copyStateMsg = "正在导出 GIF，请稍候...";
        
        // 提取导出所需的数据镜像，脱离对 UI 组件的并发依赖
        PxlPose expPose = currentPose;
        PxlSequence expSeq = currentSequence;
        int expPoseId = poseId;
        int expSeqId = sequenceId;
        
        new SwingWorker<File, Void>() {
            @Override
            protected File doInBackground() throws Exception {
                // 后台专属的克隆状态机
                FrameAnimator exportAnimator = new FrameAnimator(chara, expSeq);
                
                // TODO: 如果新版 PxlPose 有获取宽高的方法，请替换这里。目前默认 800x800
                int canvasW = 800;
                int canvasH = 800;
                int frameCount = expSeq.getFrameCount();
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                AnimatedGifEncoder encoder = new AnimatedGifEncoder();
                encoder.start(baos);
                encoder.setRepeat(0); // 无限循环
                
                for (int i = 0; i < frameCount; i++) {
                    BufferedImage frameImage = new BufferedImage(canvasW, canvasH,
                                                                 BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = frameImage.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                         RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    
                    // 注意：此处需要背景透明，若 GIF 需要底色可在此 fillRect
                    exportAnimator.renderTo(g2d, canvasW / 2.0, canvasH / 2.0, 0);
                    g2d.dispose();
                    
                    // 设置当前帧的停留时间 (crf60 是 1/60 秒，换算为毫秒)
                    int crf = expSeq.getFrame(i).getCrf60();
                    encoder.setDelay((int) (crf * (1000.0 / 60.0)));
                    encoder.addFrame(frameImage);
                    
                    exportAnimator.stepFrame();
                }
                encoder.finish();
                
                String fileName = String.format("%s_p%d_s%d.gif", expPose.getTitle()
                    .replaceAll("[\\\\/:*?\"<>|]", "_"), expPoseId, expSeqId);
                File outFile = new File(workingDirectory, fileName);
                Files.write(outFile.toPath(), baos.toByteArray());
                return outFile;
            }
            
            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    File result = get();
                    copyStateMsg = "GIF 导出成功: " + result.getName();
                } catch (Exception e) {
                    showErrorDialog(e);
                    copyStateMsg = "GIF 导出失败！";
                }
            }
        }.execute();
    }
}