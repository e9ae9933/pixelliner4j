package org.aliceincradle.tools;

import org.aliceincradle.pxlsloader.PxlCharacter;
import org.aliceincradle.pxlsloader.Settings;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PxlsKiller 重写版
 * 目标：将 TextAsset_*.pxls 及其关联的 PNG 纹理合并为单文件 .pxl
 * 特点：使用 Java 原生 Swing UI 和标准 I/O，移除外部图形库依赖
 */
public class PxlsKiller extends JFrame {
    private final JTextArea logArea;
    private final JProgressBar progressBar;
    private final JButton selectButton;
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    
    public PxlsKiller() {
        setTitle("PxlsKiller - 像素资源合并工具");
        setSize(800, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // 日志显示区
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(logArea), BorderLayout.CENTER);
        
        // 底部控制面板
        JPanel bottomPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        bottomPanel.add(progressBar, BorderLayout.NORTH);
        
        selectButton = new JButton("选择 AliceInCradle_Data 文件夹并开始合并");
        selectButton.addActionListener(e -> selectAndProcess());
        bottomPanel.add(selectButton, BorderLayout.SOUTH);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        appendLog("就绪。请选择包含 .pxls 文件的目录。");
    }
    
    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    private void selectAndProcess() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path rootPath = chooser.getSelectedFile().toPath();
            selectButton.setEnabled(false);
            processDirectory(rootPath);
        }
    }
    
    private void processDirectory(Path rootPath) {
        appendLog("正在扫描目录: " + rootPath);
        List<Path> pxlsPaths = new ArrayList<>();
        
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.getFileName().toString().endsWith(".pxls")) {
                        pxlsPaths.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            appendLog("扫描失败: " + e.getMessage());
            selectButton.setEnabled(true);
            return;
        }
        
        int total = pxlsPaths.size();
        if (total == 0) {
            appendLog("未找到任何 .pxls 文件。");
            selectButton.setEnabled(true);
            return;
        }
        
        appendLog("找到 " + total + " 个文件，开始处理...");
        progressBar.setMaximum(total);
        progressBar.setValue(0);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Path pxlsPath : pxlsPaths) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    processSingleFile(pxlsPath);
                    successCount.incrementAndGet();
                    appendLog("[完成] " + pxlsPath.getFileName());
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    appendLog("[错误] " + pxlsPath.getFileName() + ": " + e.getMessage());
                } finally {
                    SwingUtilities.invokeLater(() -> progressBar.setValue(progressBar.getValue() + 1));
                }
            }, executor));
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            appendLog(String.format("\n任务完成！成功: %d, 失败: %d", successCount.get(), failCount.get()));
            SwingUtilities.invokeLater(() -> selectButton.setEnabled(true));
        });
    }
    
    private void processSingleFile(Path pxlsPath) throws Exception {
        Path dir = pxlsPath.getParent();
        String fileName = pxlsPath.getFileName().toString();
        
        // 正则解析 TextAsset_(.*).pxls
        Pattern pattern = Pattern.compile("TextAsset_(.*)\\.pxls");
        Matcher matcher = pattern.matcher(fileName);
        if (!matcher.find()) {
            throw new Exception("文件名格式不符合 TextAsset_*.pxls");
        }
        String assetName = matcher.group(1);
        
        // 定义图像读取逻辑
        IntFunction<byte[]> pngReader = id -> {
            try {
                Path pngPath = dir.resolve(String.format("Texture_%s.pxls.texture_%d.png", assetName, id));
                return Files.readAllBytes(pngPath);
            } catch (IOException e) {
                throw new RuntimeException("无法读取贴图: " + e.getMessage());
            }
        };
        
        Settings settings = new Settings();
        settings.loadFromPngFunction = pngReader;
        
        // 加载并序列化为 .pxl
        PxlCharacter character = new PxlCharacter(Files.readAllBytes(pxlsPath), settings);
        byte[] output = character.serializeToPxlsFile();
        
        Path outputFile = dir.resolve("TextAsset_" + assetName + ".pxl");
        Files.write(outputFile, output);
    }
    
    public static void main(String[] args) {
        // 设置系统外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        
        SwingUtilities.invokeLater(() -> {
            PxlsKiller app = new PxlsKiller();
            app.setLocationRelativeTo(null);
            app.setVisible(true);
        });
    }
}