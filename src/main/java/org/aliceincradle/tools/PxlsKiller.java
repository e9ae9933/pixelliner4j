package org.aliceincradle.tools;

import org.aliceincradle.pxlsloader.PxlCharacter;
import org.aliceincradle.pxlsloader.Settings;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PxlsKiller 资源合并工具 (防 OOM 自定义并发版)
 * 特点：原生 Swing UI、实时内存监控、自定义线程池、智能贴图寻址（无视前缀）
 */
public class PxlsKiller extends JFrame {
    private final JTextArea logArea;
    private final JProgressBar progressBar;
    private final JButton selectButton;
    private final JSpinner threadSpinner;
    private final JLabel memoryLabel;
    
    public PxlsKiller() {
        setTitle("PxlsKiller - 像素资源合并工具 (内存监控版)");
        setSize(850, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // 顶部控制面板 (线程设置 + 内存监控)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JPanel threadPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        threadPanel.add(new JLabel("并发线程数: "));
        // 默认 CPU 核心数，范围 1~128
        threadSpinner = new JSpinner(new SpinnerNumberModel(Runtime.getRuntime()
                                                                .availableProcessors(), 1, 128, 1));
        threadPanel.add(threadSpinner);
        topPanel.add(threadPanel, BorderLayout.WEST);
        
        memoryLabel = new JLabel("内存: 0 MB / 0 MB");
        memoryLabel.setForeground(new Color(150, 0, 0));
        topPanel.add(memoryLabel, BorderLayout.EAST);
        
        add(topPanel, BorderLayout.NORTH);
        
        // 日志显示区
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(0, 200, 0));
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(logArea), BorderLayout.CENTER);
        
        // 底部控制面板
        JPanel bottomPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        bottomPanel.add(progressBar, BorderLayout.NORTH);
        
        selectButton = new JButton("选择目录并开始合并");
        selectButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        selectButton.setPreferredSize(new Dimension(0, 40));
        selectButton.addActionListener(e -> selectAndProcess());
        bottomPanel.add(selectButton, BorderLayout.SOUTH);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        // 启动内存监控定时器 (每 500ms 刷新一次)
        startMemoryMonitor();
        
        appendLog("就绪。请选择包含 .pxls 文件的目录。");
    }
    
    private void startMemoryMonitor() {
        Timer timer = new Timer(500, e -> {
            Runtime rt = Runtime.getRuntime();
            long totalMB = rt.totalMemory() / 1048576;
            long freeMB = rt.freeMemory() / 1048576;
            long usedMB = totalMB - freeMB;
            memoryLabel.setText(String.format("内存: %d MB / %d MB", usedMB, totalMB));
        });
        timer.start();
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
            
            int threadCount = (int) threadSpinner.getValue();
            processDirectory(rootPath, threadCount);
        }
    }
    
    private void processDirectory(Path rootPath, int threadCount) {
        appendLog(String.format("正在扫描目录: %s (并发限制: %d 线程)", rootPath, threadCount));
        List<Path> pxlsPaths = new ArrayList<>();
        
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.getFileName().toString().toLowerCase().endsWith(".pxls")) {
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
        
        // 动态创建自定义大小的线程池
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (Path pxlsPath : pxlsPaths) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    processSingleFile(pxlsPath);
                    successCount.incrementAndGet();
                    appendLog("[成功] " + pxlsPath.getFileName());
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    appendLog("[错误] " + pxlsPath.getFileName() + ": " + e.getMessage());
                } finally {
                    SwingUtilities.invokeLater(() -> progressBar.setValue(
                        progressBar.getValue() + 1));
                }
            }, executor));
        }
        
        // 任务全部完成后安全回收资源
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            appendLog(String.format("\n================\n任务结束！成功: %d, 失败: %d", successCount.get()
                , failCount.get()));
            executor.shutdown();
            SwingUtilities.invokeLater(() -> selectButton.setEnabled(true));
        });
    }
    
    private void processSingleFile(Path pxlsPath) throws Exception {
        Path dir = pxlsPath.getParent();
        String fileName = pxlsPath.getFileName().toString();
        
        // 直接剥离后缀获取基准名，去除 Regex 和前缀限制
        String assetName = fileName.substring(0, fileName.toLowerCase().lastIndexOf(".pxls"));
        
        // 定义图像读取逻辑 (多策略智能匹配)
        Settings.LoadFromPngFunction pngReader = id -> {
            try {
                // 定义所有可能的贴图命名格式（按命中优先级排列）
                Path[] possiblePaths = {dir.resolve(String.format("Texture_%s.pxls.texture_%d" +
                                                                  ".png", assetName, id)), // 原生标准提取
                    dir.resolve(String.format("%s.pxls.texture_%d.png", assetName, id)),
                    // 去掉Texture前缀
                    dir.resolve(String.format("%s.texture_%d.png", assetName, id)),
                    // 去掉中间的.pxls
                    dir.resolve(String.format("%s_%d.png", assetName, id)),
                    // 极简下划线
                    dir.resolve(String.format("Texture_TextAsset_%s.pxls.texture_%d.png",
                                              assetName, id)) // 保留原始TextAsset遗迹
                };
                
                for (Path p : possiblePaths) {
                    if (Files.exists(p)) {
                        return Files.readAllBytes(p);
                    }
                }
                throw new IOException("无法找到 ID=" + id + " 对应的贴图碎片。");
            } catch (IOException e) {
                throw new RuntimeException("读取贴图时发生异常: " + e.getMessage(), e);
            }
        };
        
        Settings settings = new Settings.Builder().setLoadFromPngFunction(pngReader).build();
        
        // 核心解析与重新打包
        PxlCharacter character = new PxlCharacter(Files.readAllBytes(pxlsPath), settings);
        byte[] output = character.serializeToPxlsFile();
        
        // 结果输出 (生成与原基准名同名的 .pxl 文件)
        Path outputFile = dir.resolve(assetName + ".pxl");
        Files.write(outputFile, output);
    }
    
    public static void main(String[] args) {
        // 设置操作系统原生 UI 外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        
        SwingUtilities.invokeLater(() -> {
            PxlsKiller app = new PxlsKiller();
            app.setLocationRelativeTo(null);
            app.setVisible(true);
        });
    }
}