import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
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
 * 3. 从选定的风险配置文件中读取风险模组名称列表（配置文件位于程序目录下的 risk_configs 文件夹）
 * 4. GUI 中显示所有模组，风险模组用红色文字标记，复选框表示当前启用状态
 * 5. 提供“禁用风险模组”按钮一键关闭所有风险模组，以及“应用更改”按钮手动应用复选框状态
 * 6. 配置项（.minecraft 路径、上次选择版本、当前风险配置文件）保存在 settings.ini 中
 * 7. 鼠标滚轮滚动速度优化（设置 unitIncrement=20）
 */
public class DisableServerRiskMods extends JFrame {
    // 配置文件常量
    private static final String SETTINGS_FILE = "settings.ini";
    private static final String RISK_CONFIGS_DIR = "risk_configs";
    private static final String DEFAULT_RISK_CONFIG = "default.txt";

    // 游戏目录相关常量
    private static final String MODS_DIR_NAME = "mods";
    private static final String VERSIONS_DIR_NAME = "versions";

    // UI 组件
    private JTextField minecraftPathField;
    private JComboBox<String> versionCombo;
    private JComboBox<String> riskConfigCombo;
    private JPanel modsPanel;
    private JScrollPane scrollPane;
    private JButton applyButton;
    private JButton disableRiskButton;

    // 数据
    private File minecraftDir;
    private File currentModsDir;
    private List<ModInfo> currentMods;
    private Set<String> riskModNames = new HashSet<>(); // 从当前风险配置文件加载

    // 设置
    private String minecraftPathSetting = "";
    private String lastVersionSetting = "";
    private String currentRiskConfig = DEFAULT_RISK_CONFIG;

    public DisableServerRiskMods() {
        setTitle("禁用服务器风险模组");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(650, 500);
        setLocationRelativeTo(null);

        // 先加载设置和确保目录存在（此时 riskConfigCombo 尚未创建）
        loadSettings();
        ensureRiskConfigsDir();

        // 创建 UI（其中包括 riskConfigCombo）
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);

        modsPanel = new JPanel();
        modsPanel.setLayout(new BoxLayout(modsPanel, BoxLayout.Y_AXIS));
        scrollPane = new JScrollPane(modsPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // 现在 riskConfigCombo 已存在，可以安全地刷新列表和加载风险名称
        refreshRiskConfigList();
        loadRiskModNames();

        // 初始状态
        versionCombo.setEnabled(false);
        applyButton.setEnabled(false);
        disableRiskButton.setEnabled(false);

        // 窗口显示后应用保存的路径和版本
        SwingUtilities.invokeLater(this::applySavedSettings);
    }

    /**
     * 加载 settings.ini 中的配置
     */
    private void loadSettings() {
        Properties props = new Properties();
        File settingsFile = new File(SETTINGS_FILE);
        if (settingsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(settingsFile)) {
                props.load(fis);
                minecraftPathSetting = props.getProperty("minecraft_path", "");
                lastVersionSetting = props.getProperty("last_version", "");
                currentRiskConfig = props.getProperty("risk_config", DEFAULT_RISK_CONFIG);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "读取 settings.ini 失败：" + e.getMessage(), "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            // 创建默认设置文件
            saveSettings();
        }
    }

    /**
     * 保存设置到 settings.ini
     */
    private void saveSettings() {
        Properties props = new Properties();
        props.setProperty("minecraft_path", minecraftPathSetting);
        props.setProperty("last_version", lastVersionSetting);
        props.setProperty("risk_config", currentRiskConfig);
        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            props.store(fos, "禁用服务器风险模组配置文件");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "保存 settings.ini 失败：" + e.getMessage(), "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 确保 risk_configs 目录存在
     */
    private void ensureRiskConfigsDir() {
        File dir = new File(RISK_CONFIGS_DIR);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                JOptionPane.showMessageDialog(this, "无法创建风险配置目录：" + RISK_CONFIGS_DIR, "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 刷新风险配置文件下拉列表
     */
    private void refreshRiskConfigList() {
        if (riskConfigCombo == null)
            return; // 防御性检查
        File dir = new File(RISK_CONFIGS_DIR);
        String[] files = dir.list((d, name) -> name.endsWith(".txt"));
        riskConfigCombo.removeAllItems();
        if (files != null && files.length > 0) {
            for (String file : files) {
                riskConfigCombo.addItem(file);
            }
        } else {
            createDefaultRiskConfig();
            riskConfigCombo.addItem(DEFAULT_RISK_CONFIG);
        }
        // 尝试选中 currentRiskConfig，若无效则选择第一项
        if (riskConfigCombo.getItemCount() > 0) {
            if (currentRiskConfig != null) {
                riskConfigCombo.setSelectedItem(currentRiskConfig);
            }
            if (riskConfigCombo.getSelectedItem() == null) {
                riskConfigCombo.setSelectedIndex(0);
                currentRiskConfig = (String) riskConfigCombo.getSelectedItem();
                saveSettings();
            }
        }
    }

    /**
     * 创建默认风险配置文件（空文件）
     */
    private void createDefaultRiskConfig() {
        File defaultFile = new File(RISK_CONFIGS_DIR, DEFAULT_RISK_CONFIG);
        if (!defaultFile.exists()) {
            try {
                defaultFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 从当前选中的风险配置文件加载风险模组名称
     */
    private void loadRiskModNames() {
        riskModNames.clear();
        File configFile = new File(RISK_CONFIGS_DIR, currentRiskConfig);
        if (!configFile.exists()) {
            createDefaultRiskConfig();
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                riskModNames.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "读取风险配置文件失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 切换风险配置文件时调用
     */
    private void onRiskConfigChanged() {
        String selected = (String) riskConfigCombo.getSelectedItem();
        if (selected != null && !selected.equals(currentRiskConfig)) {
            currentRiskConfig = selected;
            saveSettings();
            loadRiskModNames();
            // 如果已有模组列表，刷新显示以更新颜色
            if (currentMods != null) {
                updateModsPanel(currentMods);
            }
        }
    }

    /**
     * 应用保存的设置（路径和版本）
     */
    private void applySavedSettings() {
        if (minecraftPathSetting != null && !minecraftPathSetting.isEmpty()) {
            File dir = new File(minecraftPathSetting);
            if (dir.exists() && dir.isDirectory()) {
                minecraftPathField.setText(minecraftPathSetting);
                minecraftDir = dir;
                scanVersions();

                // 尝试恢复上次选择的版本
                if (lastVersionSetting != null && !lastVersionSetting.isEmpty()) {
                    versionCombo.setSelectedItem(lastVersionSetting);
                }
            }
        }
    }

    /**
     * 创建顶部面板（目录、版本、风险配置）
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

        // 风险配置文件选择
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        panel.add(new JLabel("风险配置:"), gbc);

        riskConfigCombo = new JComboBox<>();
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(riskConfigCombo, gbc);

        JButton refreshRiskButton = new JButton("刷新");
        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(refreshRiskButton, gbc);

        // 事件绑定
        browseButton.addActionListener(e -> chooseMinecraftDirectory());
        versionCombo.addActionListener(e -> {
            if (versionCombo.getSelectedItem() != null && minecraftDir != null) {
                String selectedVersion = (String) versionCombo.getSelectedItem();
                // 保存选中的版本
                lastVersionSetting = selectedVersion;
                saveSettings();
                determineModsDir(selectedVersion);
            }
        });
        riskConfigCombo.addActionListener(e -> onRiskConfigChanged());
        refreshRiskButton.addActionListener(e -> refreshRiskConfigList());

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
            minecraftPathSetting = selected.getAbsolutePath();
            saveSettings();
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
        versionCombo.addItem("[根目录 mods]");
        versionCombo.setEnabled(versionCombo.getItemCount() > 0);
    }

    /**
     * 根据所选版本确定 mods 文件夹路径
     */
    private void determineModsDir(String selectedVersion) {
        if (minecraftDir == null)
            return;

        if ("[根目录 mods]".equals(selectedVersion)) {
            currentModsDir = new File(minecraftDir, MODS_DIR_NAME);
        } else {
            File versionMods = new File(minecraftDir,
                    VERSIONS_DIR_NAME + File.separator + selectedVersion + File.separator + MODS_DIR_NAME);
            if (versionMods.exists() && versionMods.isDirectory()) {
                currentModsDir = versionMods;
            } else {
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
        File[] files = modsDir.listFiles((dir, name) -> name.endsWith(".jar") || name.endsWith(".jar.disabled"));
        if (files == null)
            return mods;

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
            if (entry == null)
                return null;

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
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DisableServerRiskMods.this,
                                "重命名失败: " + currentFile.getName() + " -> " + targetFile.getName() + "\n"
                                        + e.getMessage(),
                                "错误", JOptionPane.ERROR_MESSAGE));
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