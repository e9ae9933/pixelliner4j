package org.aliceincradle.tools;

import org.aliceincradle.pxlsloader.PxlCharacter;
import org.aliceincradle.pxlsloader.PxlFrame;
import org.aliceincradle.pxlsloader.PxlLayer;
import org.aliceincradle.pxlsloader.PxlSequence;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 现代版 AWT 动画状态机与渲染器。
 * 纯粹依赖 Java 原生 Graphics2D 与 Pxl POJO 数据模型。
 * 采用 Fail-Fast 策略，并在矩阵计算上追求与 Alice In Cradle 游戏本体 100% 的 Bug 级兼容。
 */
public class FrameAnimator {
    private final PxlCharacter character;
    private PxlSequence sequence;
    
    public int position;
    public int stepped;
    public int loopedCount;
    
    public FrameAnimator(PxlCharacter character, PxlSequence sequence) {
        this.character = character;
        this.sequence = sequence;
        this.position = 0;
        this.stepped = 0;
        this.loopedCount = 0;
    }
    
    // --- 核心渲染管线 ---
    
    public void renderTo(Graphics2D g2d, double x, double y, double theta) {
        renderTo(g2d, x, y, theta, 1.0, 1.0, 1.0, false);
    }
    
    public void renderTo(Graphics2D g2d, double x, double y, double theta, double scaleX,
                         double scaleY) {
        renderTo(g2d, x, y, theta, scaleX, scaleY, 1.0, false);
    }
    
    public void renderTo(Graphics2D g2d, double x, double y, double theta, double scaleX,
                         double scaleY, double alpha, boolean debugBoxes) {
        if (sequence == null || sequence.getFrameCount() == 0)
            return;
        
        PxlFrame frame = sequence.getFrame(position);
        
        // 备份外部传入的画笔状态（非常重要，用于恢复和画绝对坐标线段）
        AffineTransform originalTransform = g2d.getTransform();
        Composite originalComposite = g2d.getComposite();
        
        try {
            for (PxlLayer layer : frame.getLayerList()) {
                if (layer.getAlpha() <= 0)
                    continue;
                
                BufferedImage img = layer.getImage(); // 配合底层做 Fail-Fast
                
                // 1. 透明度：万分比换算，并加入防御性钳制 (Clamp) 防止奇葩数据炸毁画笔
                float finalAlpha = (float) (layer.getAlpha() / 10000.0f * alpha);
                finalAlpha = Math.max(0.0f, Math.min(1.0f, finalAlpha));
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, finalAlpha));
                
                // 2. 矩阵构建：必须从纯净的空矩阵 (Identity) 开始！
                // 否则 g2d.drawImage 会在底层造成双重相机偏移
                AffineTransform trans = new AffineTransform();
                
                // 完美复刻原版的 "散沙" 标量加法逻辑
                trans.translate(x + layer.getX(), y + layer.getY());
                trans.rotate(theta + layer.getRotR());
                trans.scale(layer.getZmx() * scaleX, layer.getZmy() * scaleY);
                trans.translate(-img.getWidth() / 2.0, -img.getHeight() / 2.0);
                
                // 3. 执行绘制：g2d 会在底层自动计算: originalTransform * trans
                g2d.drawImage(img, trans, null);
                
                // 4. [调试模式]：完美复刻你一年前的 4 顶点连线包围盒
                if (debugBoxes) {
                    g2d.setTransform(originalTransform); // 切回世界坐标系画线，保证线宽不受缩放影响
                    int w = img.getWidth();
                    int h = img.getHeight();
                    
                    Point2D[] p = {new Point2D.Double(0, 0), new Point2D.Double(w, 0),
                        new Point2D.Double(w, h), new Point2D.Double(0, h)};
                    Point2D[] dst = new Point2D[4];
                    trans.transform(p, 0, dst, 0, 4); // 用刚刚算好的图层局部矩阵转换顶点
                    
                    g2d.setColor(new Color(0x7f000000, true)); // 还原半透明黑色 0x7f000000
                    for (int i = 0; i < 4; i++) {
                        Point2D a = dst[i], b = dst[(i + 1) % 4];
                        g2d.drawLine((int) a.getX(), (int) a.getY(), (int) b.getX(),
                                     (int) b.getY());
                    }
                }
            }
            // 5. [调试模式]：追加复刻一年前的 "Sequence 级全局边界框"
            if (debugBoxes) {
                g2d.setTransform(originalTransform); // 确保在世界坐标系下绘制
                
                // 注意：旧代码里有个 sequence.getFa()，假设你现在 PxlSequence 里有 getWidth() 和 getHeight()
                int w = sequence.getWidth();
                int h = sequence.getHeight();
                double px = -w / 2.0;
                double py = -h / 2.0;
                
                AffineTransform seqTrans = new AffineTransform();
                seqTrans.translate(x + px, y + py);
                seqTrans.rotate(theta, -px, -py); // 绕左上角偏移点旋转，复刻你的旧逻辑
                
                Point2D[] p = {new Point2D.Double(0, 0), new Point2D.Double(w, 0),
                    new Point2D.Double(w, h), new Point2D.Double(0, h)};
                Point2D[] dst = new Point2D[4];
                seqTrans.transform(p, 0, dst, 0, 4);
                
                g2d.setColor(Color.BLACK); // 全局判定框用纯黑色
                for (int i = 0; i < 4; i++) {
                    Point2D a = dst[i], b = dst[(i + 1) % 4];
                    g2d.drawLine((int) a.getX(), (int) a.getY(), (int) b.getX(), (int) b.getY());
                }
            }
        } finally {
            // 闭环清理
            g2d.setTransform(originalTransform);
            g2d.setComposite(originalComposite);
        }
    }
    
    // --- 状态机控制流 ---
    
    public void step() {
        if (sequence == null || sequence.getFrameCount() == 0)
            return;
        
        stepped++;
        if (stepped >= sequence.getFrame(position).getCrf60()) {
            stepped = 0;
            position++;
            if (position >= sequence.getFrameCount()) {
                position = sequence.getLoopTo();
                loopedCount++;
            }
        }
    }
    
    public void stepFrame() {
        if (sequence == null || sequence.getFrameCount() == 0)
            return;
        
        stepped = 0;
        position++;
        if (position >= sequence.getFrameCount()) {
            position = sequence.getLoopTo();
            loopedCount++;
        }
    }
    
    public void changeFrame(String name) {
        stepped = 0;
        List<PxlFrame> fs = sequence.getFrameList();
        
        position = IntStream.range(0, fs.size())
            .filter(i -> fs.get(i).getName().equalsIgnoreCase(name)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Sequence 中不存在名为 '" + name + "' 的帧！"));
    }
    
    public void renderAndStep(Graphics2D g2d, int x, int y, double theta) {
        renderTo(g2d, x, y, theta);
        step();
    }
    
    public void reset() {
        position = 0;
        stepped = 0;
        loopedCount = 0;
    }
    
    public void setSequence(PxlSequence newSequence) {
        this.sequence = newSequence;
        reset();
    }
    
    @Override
    public String toString() {
        if (sequence == null)
            return "FrameAnimator{null}";
        return String.format("FrameAnimator{frameName='%s', pos=%d/%d, stepped=%d, loops=%d}",
                             sequence.getFrame(position)
            .getName(), position, sequence.getFrameCount(), stepped, loopedCount);
    }
}