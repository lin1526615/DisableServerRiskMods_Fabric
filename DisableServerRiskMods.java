import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 禁用服务器风险模组
 * 功能：
 * 1. 选择 .minecraft 目录，列出可用版本
 * 2. 扫描对应版本的 mods 文件夹，识别所有 Fabric 模组（通过读取 fabric.mod.json 中的 name 字段）
 * 3. 从同目录下的 disabled-mods.txt 文件中读取风险模组名称列表（第一行为默认 .minecraft 路径）
 * 4. GUI 中显示所有模组，风险模组用红色文字标记，复选框表示当前启用状态
 * 5. 提供“禁用风险模组”按钮一键关闭所有风险模组，以及“应用更改”按钮手动应用复选框状态
 * 6. 配置文件第一行为默认路径，修改路径后自动更新配置文件
 * 7. 鼠标滚轮滚动速度优化（设置 unitIncrement=20）
 */
public class DisableServerRiskMods extends JFrame {
    private static final String CONFIG_FILE = "disabled-mods.txt";
    private static final String MODS_DIR_NAME = "mods";
    private static final String VERSIONS_DIR_NAME = "versions";

    private JTextField minecraftPathField;
    private JComboBox<String> versionCombo;
    private JPanel modsPanel;
    private JScrollPane scrollPane;
    private JButton applyButton;
    private JButton disableRiskButton;

    private File minecraftDir;
    private File currentModsDir;
    private List<ModInfo> currentMods;
    private Set<String> riskModNames;      // 从配置文件加载的风险模组名称
    private String defaultMinecraftPath;   // 从配置文件第一行加载的默认路径

    public DisableServerRiskMods() {
        setTitle("禁用服务器风险模组");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);

        // 加载配置文件（包含路径和风险模组）
        loadConfig();

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 顶部选择面板
        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // 中间模组列表（放入滚动面板）
        modsPanel = new JPanel();
        modsPanel.setLayout(new BoxLayout(modsPanel, BoxLayout.Y_AXIS));
        scrollPane = new JScrollPane(modsPanel);
        // 优化鼠标滚轮滚动速度
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // 底部按钮面板
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // 初始状态
        versionCombo.setEnabled(false);
        applyButton.setEnabled(false);
        disableRiskButton.setEnabled(false);

        // 窗口显示后尝试应用默认路径
        SwingUtilities.invokeLater(this::applyDefaultPath);
    }

    /**
     * 加载配置文件：第一行为默认路径，其余行为风险模组名称
     */
    private void loadConfig() {
        riskModNames = new HashSet<>();
        defaultMinecraftPath = "";
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            // 创建空文件，不写入任何内容
            try {
                configFile.createNewFile();
                JOptionPane.showMessageDialog(this,
                        "未找到配置文件，已创建空文件：" + CONFIG_FILE + "\n请编辑该文件，第一行为 .minecraft 路径，后续每行一个风险模组名称（支持#注释）。",
                        "提示", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "无法创建配置文件：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (firstLine) {
                    firstLine = false;
                    // 第一行作为路径，忽略空行和注释
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        defaultMinecraftPath = line;
                    }
                    continue;
                }
                // 剩余行作为风险模组名称，忽略空行和注释
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                riskModNames.add(line);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "读取配置文件失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 将新路径更新到配置文件第一行，同时保留原有的风险模组列表
     */
    private void updateConfigFileWithPath(String newPath) {
        File configFile = new File(CONFIG_FILE);
        List<String> riskLines = new ArrayList<>();

        // 先读取原有风险模组名称（跳过第一行）
        if (configFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                        continue; // 跳过旧路径
                    }
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        riskLines.add(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 写入新配置：第一行为新路径，后面为风险模组名称
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            writer.write(newPath);
            writer.newLine();
            for (String risk : riskLines) {
                writer.write(risk);
                writer.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "更新配置文件失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 尝试应用默认路径（如果存在且有效）
     */
    private void applyDefaultPath() {
        if (defaultMinecraftPath != null && !defaultMinecraftPath.isEmpty()) {
            File dir = new File(defaultMinecraftPath);
            if (dir.exists() && dir.isDirectory()) {
                minecraftPathField.setText(defaultMinecraftPath);
                minecraftDir = dir;
                scanVersions();
            }
        }
    }

    /**
     * 创建顶部面板（选择目录和版本）
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // .minecraft 目录
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel(".minecraft 目录:"), gbc);

        minecraftPathField = new JTextField();
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(minecraftPathField, gbc);

        JButton browseButton = new JButton("浏览");
        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(browseButton, gbc);

        // 版本选择
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("版本:"), gbc);

        versionCombo = new JComboBox<>();
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(versionCombo, gbc);

        // 占位
        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(new JLabel(""), gbc);

        // 浏览按钮事件
        browseButton.addActionListener(e -> chooseMinecraftDirectory());

        // 版本选择事件
        versionCombo.addActionListener(e -> {
            if (versionCombo.getSelectedItem() != null && minecraftDir != null) {
                String selectedVersion = (String) versionCombo.getSelectedItem();
                determineModsDir(selectedVersion);
            }
        });

        return panel;
    }

    /**
     * 创建底部按钮面板
     */
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        disableRiskButton = new JButton("禁用风险模组");
        applyButton = new JButton("应用更改");

        panel.add(disableRiskButton);
        panel.add(applyButton);

        // 禁用风险按钮事件：将所有风险模组的复选框设为未选中并应用
        disableRiskButton.addActionListener(e -> {
            for (Component comp : modsPanel.getComponents()) {
                if (comp instanceof ModCheckBox) {
                    ModCheckBox checkBox = (ModCheckBox) comp;
                    if (riskModNames.contains(checkBox.modInfo.name)) {
                        checkBox.setSelected(false);
                    }
                }
            }
            applyChanges();
        });

        // 应用按钮事件
        applyButton.addActionListener(e -> applyChanges());

        return panel;
    }

    /**
     * 选择 .minecraft 目录
     */
    private void chooseMinecraftDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("选择 .minecraft 目录");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            minecraftPathField.setText(selected.getAbsolutePath());
            minecraftDir = selected;
            // 更新配置文件中的默认路径
            updateConfigFileWithPath(selected.getAbsolutePath());
            // 更新内存中的默认路径
            defaultMinecraftPath = selected.getAbsolutePath();
            scanVersions();
        }
    }

    /**
     * 扫描 versions 文件夹，填充版本下拉框
     */
    private void scanVersions() {
        File versionsDir = new File(minecraftDir, VERSIONS_DIR_NAME);
        versionCombo.removeAllItems();
        if (versionsDir.exists() && versionsDir.isDirectory()) {
            File[] versionFolders = versionsDir.listFiles(File::isDirectory);
            if (versionFolders != null) {
                for (File vf : versionFolders) {
                    versionCombo.addItem(vf.getName());
                }
            }
        }
        // 添加一个“根目录mods”选项，用于没有版本隔离的情况
        versionCombo.addItem("[根目录 mods]");
        versionCombo.setEnabled(versionCombo.getItemCount() > 0);
    }

    /**
     * 根据所选版本确定 mods 文件夹路径
     */
    private void determineModsDir(String selectedVersion) {
        if (minecraftDir == null) return;

        if ("[根目录 mods]".equals(selectedVersion)) {
            currentModsDir = new File(minecraftDir, MODS_DIR_NAME);
        } else {
            // 先检查 versions/<version>/mods
            File versionMods = new File(minecraftDir, VERSIONS_DIR_NAME + File.separator + selectedVersion + File.separator + MODS_DIR_NAME);
            if (versionMods.exists() && versionMods.isDirectory()) {
                currentModsDir = versionMods;
            } else {
                // 回退到根目录 mods
                currentModsDir = new File(minecraftDir, MODS_DIR_NAME);
            }
        }

        if (!currentModsDir.exists()) {
            JOptionPane.showMessageDialog(this,
                    "mods 文件夹不存在: " + currentModsDir.getAbsolutePath(),
                    "错误", JOptionPane.ERROR_MESSAGE);
            applyButton.setEnabled(false);
            disableRiskButton.setEnabled(false);
            return;
        }

        // 开始扫描模组
        scanModsInBackground();
    }

    /**
     * 在后台线程扫描 mods 文件夹
     */
    private void scanModsInBackground() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        applyButton.setEnabled(false);
        disableRiskButton.setEnabled(false);

        SwingWorker<List<ModInfo>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ModInfo> doInBackground() {
                return scanMods(currentModsDir);
            }

            @Override
            protected void done() {
                try {
                    currentMods = get();
                    updateModsPanel(currentMods);
                    applyButton.setEnabled(true);
                    disableRiskButton.setEnabled(true);
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(DisableServerRiskMods.this,
                            "扫描模组失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        worker.execute();
    }

    /**
     * 扫描指定文件夹下的所有 Fabric 模组
     */
    private List<ModInfo> scanMods(File modsDir) {
        List<ModInfo> mods = new ArrayList<>();
        File[] files = modsDir.listFiles((dir, name) ->
                name.endsWith(".jar") || name.endsWith(".jar.disabled"));
        if (files == null) return mods;

        for (File file : files) {
            String name = extractModName(file);
            if (name != null) {
                boolean enabled = !file.getName().endsWith(".disabled");
                mods.add(new ModInfo(name, file, enabled));
            }
        }
        return mods;
    }

    /**
     * 从 jar 文件中读取 fabric.mod.json 并提取 name 字段
     */
    private String extractModName(File jarFile) {
        try (ZipFile zip = new ZipFile(jarFile)) {
            ZipEntry entry = zip.getEntry("fabric.mod.json");
            if (entry == null) return null;

            StringBuilder content = new StringBuilder();
            try (InputStream is = zip.getInputStream(entry);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }

            Pattern pattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(content.toString());
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (IOException e) {
            // 无法读取，忽略
        }
        return null;
    }

    /**
     * 更新界面上的模组复选框列表
     */
    private void updateModsPanel(List<ModInfo> mods) {
        modsPanel.removeAll();
        for (ModInfo mod : mods) {
            ModCheckBox checkBox = new ModCheckBox(mod);
            checkBox.setSelected(mod.enabled);
            if (riskModNames.contains(mod.name)) {
                checkBox.setForeground(Color.RED);
            } else {
                checkBox.setForeground(Color.BLACK);
            }
            checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            modsPanel.add(checkBox);
        }
        modsPanel.revalidate();
        modsPanel.repaint();
    }

    /**
     * 应用当前复选框状态
     */
    private void applyChanges() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        applyButton.setEnabled(false);
        disableRiskButton.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                List<ModCheckBox> checkBoxes = new ArrayList<>();
                for (Component comp : modsPanel.getComponents()) {
                    if (comp instanceof ModCheckBox) {
                        checkBoxes.add((ModCheckBox) comp);
                    }
                }

                for (ModCheckBox checkBox : checkBoxes) {
                    boolean wantEnabled = checkBox.isSelected();
                    ModInfo mod = checkBox.modInfo;
                    if (mod.enabled == wantEnabled) {
                        continue;
                    }

                    File currentFile = mod.file;
                    File targetFile;
                    if (wantEnabled) {
                        String path = currentFile.getAbsolutePath();
                        if (path.endsWith(".disabled")) {
                            targetFile = new File(path.substring(0, path.length() - 9));
                        } else {
                            targetFile = currentFile;
                        }
                    } else {
                        String path = currentFile.getAbsolutePath();
                        if (path.endsWith(".jar") && !path.endsWith(".disabled")) {
                            targetFile = new File(path + ".disabled");
                        } else {
                            targetFile = currentFile;
                        }
                    }

                    if (!targetFile.equals(currentFile) && targetFile.exists()) {
                        File backup = new File(targetFile.getAbsolutePath() + ".bak");
                        try {
                            Files.move(targetFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    try {
                        Files.move(currentFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        mod.file = targetFile;
                        mod.enabled = wantEnabled;
                    } catch (IOException e) {
                        e.printStackTrace();
                        SwingUtilities.invokeLater(() ->
                                JOptionPane.showMessageDialog(DisableServerRiskMods.this,
                                        "重命名失败: " + currentFile.getName() + " -> " + targetFile.getName() + "\n" + e.getMessage(),
                                        "错误", JOptionPane.ERROR_MESSAGE)
                        );
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                scanModsInBackground();
            }
        };
        worker.execute();
    }

    /**
     * 模组信息类
     */
    private static class ModInfo {
        String name;
        File file;
        boolean enabled;

        ModInfo(String name, File file, boolean enabled) {
            this.name = name;
            this.file = file;
            this.enabled = enabled;
        }
    }

    /**
     * 关联 ModInfo 的 JCheckBox
     */
    private static class ModCheckBox extends JCheckBox {
        ModInfo modInfo;

        ModCheckBox(ModInfo modInfo) {
            super(modInfo.name);
            this.modInfo = modInfo;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new DisableServerRiskMods().setVisible(true);
        });
    }
}