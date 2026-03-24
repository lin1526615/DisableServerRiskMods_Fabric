import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
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
 * 5. 提供“禁用配置的模组项”/“启用配置的模组项”按钮一键关闭/开启所有风险模组并自动应用，以及“应用更改”按钮手动应用复选框状态
 * 6. 配置项（.minecraft 路径、上次选择版本、当前风险配置文件、语言文件）保存在 settings.ini 中
 * 7. 鼠标滚轮滚动速度优化（设置 unitIncrement=20）
 * 8. 支持通过（lang.json）修改UI语言
 */
public class DisableServerRiskMods extends JFrame {
    // 配置文件常量
    private static final String SETTINGS_FILE = "DSRMdata/settings.ini";
    private static final String RISK_CONFIGS_DIR = "DSRMdata/risk_configs";
    private static final String DEFAULT_RISK_CONFIG = "default.txt";
    private static final String DEFAULT_LANG_FILE = "DSRMdata/lang.json";

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
    private JButton riskActionButton; // 动态按钮（禁用/开启）

    // 数据
    private File minecraftDir;
    private File currentModsDir;
    private List<ModInfo> currentMods;
    private Set<String> riskModNames = new HashSet<>(); // 从当前风险配置文件加载

    // 设置
    private String minecraftPathSetting = "";
    private String lastVersionSetting = "";
    private String currentRiskConfig = DEFAULT_RISK_CONFIG;
    private String currentLangFile = DEFAULT_LANG_FILE;
    private Map<String, String> langStrings = new HashMap<>();

    // 未应用更改标记
    private boolean hasUnsavedChanges = false;

    public DisableServerRiskMods() {
        // 基础窗口设置（不依赖语言）
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(650, 500);
        setLocationRelativeTo(null);

        // 1. 加载设置（读取 minecraft_path, last_version, risk_config, language）
        loadSettings();

        // 2. 加载语言文件（现在 currentLangFile 已是 settings.ini 中指定的值）
        loadLanguage(currentLangFile);

        // 3. 设置窗口标题（依赖语言）
        setTitle(getString("title"));

        // 4. 确保风险配置目录存在
        ensureRiskConfigsDir();

        // 5. 创建 UI（所有 getString 调用此时都能获取正确语言）
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

        // 6. 后续初始化（刷新配置、扫描版本等）
        refreshRiskConfigList();
        loadRiskModNames();

        versionCombo.setEnabled(false);
        applyButton.setEnabled(false);
        riskActionButton.setEnabled(false);

        SwingUtilities.invokeLater(this::applySavedSettings);
    }

    // ==================== 语言文件管理 ====================
    private void loadLanguage(String langFileName) {
        File langFile = new File(langFileName);
        if (!langFile.exists()) {
            createDefaultLangFile(langFile);
        }

        // 读取文件内容
        StringBuilder sb = new StringBuilder();
        try (Reader reader = new InputStreamReader(new FileInputStream(langFile), StandardCharsets.UTF_8)) {
            char[] buffer = new char[1024];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    String.format("加载语言文件 %s 失败，将使用默认文本：%s", langFileName, e.getMessage()),
                    "警告", JOptionPane.WARNING_MESSAGE);
            setDefaultLangStrings();
            return;
        }

        String content = sb.toString();
        Map<String, String> loadedStrings = new HashMap<>();
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            loadedStrings.put(matcher.group(1), matcher.group(2));
        }

        // 将已加载的字符串放入 langStrings
        langStrings.clear();
        langStrings.putAll(loadedStrings);

        // 确保所有必要键都存在（内存补充）
        boolean needSave = false;
        String[] requiredKeys = {
                "title", "label_minecraft_dir", "button_browse", "label_version",
                "label_risk_config", "button_refresh_risk", "button_disable_risk",
                "button_enable_risk", "button_apply", "message_mods_folder_not_exist",
                "message_error_reading_settings", "message_error_saving_settings",
                "message_cannot_create_risk_dir", "message_error_reading_risk_config",
                "message_scan_mods_failed", "message_rename_failed", "message_choose_minecraft",
                "message_risk_config_file", "message_no_risk_mods"
        };
        for (String key : requiredKeys) {
            if (!langStrings.containsKey(key)) {
                langStrings.put(key, getDefaultString(key));
                needSave = true;
            }
        }

        // 如果有补充的键，将完整映射写回文件
        if (needSave) {
            saveLangFile(langFile);
        }
    }

    private void createDefaultLangFile(File langFile) {
        // 创建默认中文语言文件
        String defaultContent = "{\n" +
                "  \"title\": \"禁用服务器风险模组\",\n" +
                "  \"label_minecraft_dir\": \".minecraft 目录:\",\n" +
                "  \"button_browse\": \"浏览\",\n" +
                "  \"label_version\": \"版本:\",\n" +
                "  \"label_risk_config\": \"风险配置:\",\n" +
                "  \"button_refresh_risk\": \"刷新\",\n" +
                "  \"button_disable_risk\": \"禁用配置的模组项\",\n" +
                "  \"button_enable_risk\": \"启用配置的模组项\",\n" +
                "  \"button_apply\": \"应用更改\",\n" +
                "  \"message_mods_folder_not_exist\": \"mods 文件夹不存在: %s\",\n" +
                "  \"message_error_reading_settings\": \"读取 settings.ini 失败：%s\",\n" +
                "  \"message_error_saving_settings\": \"保存 settings.ini 失败：%s\",\n" +
                "  \"message_cannot_create_risk_dir\": \"无法创建风险配置目录：%s\",\n" +
                "  \"message_error_reading_risk_config\": \"读取风险配置文件失败：%s\",\n" +
                "  \"message_scan_mods_failed\": \"扫描模组失败: %s\",\n" +
                "  \"message_rename_failed\": \"重命名失败: %s -> %s\\n%s\",\n" +
                "  \"message_choose_minecraft\": \"选择 .minecraft 目录\",\n" +
                "  \"message_risk_config_file\": \"风险配置文件\",\n" +
                "  \"message_no_risk_mods\": \"当前版本未安装任何风险模组。\"\n" +
                "}";
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(langFile), StandardCharsets.UTF_8)) {
            writer.write(defaultContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setDefaultLangStrings() {
        langStrings.clear();
        langStrings.put("title", "禁用服务器风险模组");
        langStrings.put("label_minecraft_dir", ".minecraft 目录:");
        langStrings.put("button_browse", "浏览");
        langStrings.put("label_version", "版本:");
        langStrings.put("label_risk_config", "风险配置:");
        langStrings.put("button_refresh_risk", "刷新");
        langStrings.put("button_disable_risk", "禁用配置的模组项");
        langStrings.put("button_enable_risk", "启用配置的模组项");
        langStrings.put("button_apply", "应用更改");
        langStrings.put("message_mods_folder_not_exist", "mods 文件夹不存在: %s");
        langStrings.put("message_error_reading_settings", "读取 settings.ini 失败：%s");
        langStrings.put("message_error_saving_settings", "保存 settings.ini 失败：%s");
        langStrings.put("message_cannot_create_risk_dir", "无法创建风险配置目录：%s");
        langStrings.put("message_error_reading_risk_config", "读取风险配置文件失败：%s");
        langStrings.put("message_scan_mods_failed", "扫描模组失败: %s");
        langStrings.put("message_rename_failed", "重命名失败: %s -> %s\n%s");
        langStrings.put("message_choose_minecraft", "选择 .minecraft 目录");
        langStrings.put("message_risk_config_file", "风险配置文件");
        langStrings.put("message_no_risk_mods", "当前版本未安装任何风险模组。");
    }

    private void saveLangFile(File langFile) {
        // 构建 JSON 格式的字符串
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        List<String> keys = new ArrayList<>(langStrings.keySet());
        Collections.sort(keys); // 可选：排序使文件美观
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = langStrings.get(key);
            // 转义特殊字符
            String escapedValue = escapeJson(value);
            json.append("  \"").append(key).append("\": \"").append(escapedValue).append("\"");
            if (i < keys.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("}");

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(langFile), StandardCharsets.UTF_8)) {
            writer.write(json.toString());
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    String.format("保存语言文件 %s 失败：%s", langFile.getName(), e.getMessage()),
                    getString("title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String escapeJson(String s) {
        if (s == null)
            return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        // 控制字符转义为 \\uXXXX
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private String getDefaultString(String key) {
        // 提供一些合理的默认值（防止完全缺失）
        switch (key) {
            case "title":
                return "禁用服务器风险模组";
            case "label_minecraft_dir":
                return ".minecraft 目录:";
            case "button_browse":
                return "浏览";
            case "label_version":
                return "版本:";
            case "label_risk_config":
                return "风险配置:";
            case "button_refresh_risk":
                return "刷新";
            case "button_disable_risk":
                return "禁用配置的模组项";
            case "button_enable_risk":
                return "开启配置的模组项";
            case "button_apply":
                return "应用更改";
            case "message_mods_folder_not_exist":
                return "mods 文件夹不存在: %s";
            case "message_error_reading_settings":
                return "读取 settings.ini 失败：%s";
            case "message_error_saving_settings":
                return "保存 settings.ini 失败：%s";
            case "message_cannot_create_risk_dir":
                return "无法创建风险配置目录：%s";
            case "message_error_reading_risk_config":
                return "读取风险配置文件失败：%s";
            case "message_scan_mods_failed":
                return "扫描模组失败: %s";
            case "message_rename_failed":
                return "重命名失败: %s -> %s\n%s";
            case "message_choose_minecraft":
                return "选择 .minecraft 目录";
            case "message_risk_config_file":
                return "风险配置文件";
            case "message_no_risk_mods":
                return "当前版本未安装任何风险模组。";
            default:
                return "";
        }
    }

    private String getString(String key) {
        return langStrings.getOrDefault(key, getDefaultString(key));
    }

    // ==================== 设置管理 ====================
    private void loadSettings() {
        Properties props = new Properties();
        File settingsFile = new File(SETTINGS_FILE);
        if (settingsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(settingsFile)) {
                props.load(fis);
                minecraftPathSetting = props.getProperty("minecraft_path", "");
                lastVersionSetting = props.getProperty("last_version", "");
                currentRiskConfig = props.getProperty("risk_config", DEFAULT_RISK_CONFIG);
                currentLangFile = props.getProperty("language", DEFAULT_LANG_FILE);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        String.format(getString("message_error_reading_settings"), e.getMessage()),
                        getString("title"), JOptionPane.ERROR_MESSAGE);
            }
        } else {
            // 创建默认设置文件
            saveSettings();
        }
    }

    private void saveSettings() {
        Properties props = new Properties();
        props.setProperty("minecraft_path", minecraftPathSetting);
        props.setProperty("last_version", lastVersionSetting);
        props.setProperty("risk_config", currentRiskConfig);
        props.setProperty("language", currentLangFile);
        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            props.store(fos, "禁用服务器风险模组配置文件");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    String.format(getString("message_error_saving_settings"), e.getMessage()),
                    getString("title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ensureRiskConfigsDir() {
        File dir = new File(RISK_CONFIGS_DIR);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                JOptionPane.showMessageDialog(this,
                        String.format(getString("message_cannot_create_risk_dir"), RISK_CONFIGS_DIR),
                        getString("title"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void refreshRiskConfigList() {
        if (riskConfigCombo == null)
            return;
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
            JOptionPane.showMessageDialog(this,
                    String.format(getString("message_error_reading_risk_config"), e.getMessage()),
                    getString("title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onRiskConfigChanged() {
        String selected = (String) riskConfigCombo.getSelectedItem();
        if (selected != null && !selected.equals(currentRiskConfig)) {
            currentRiskConfig = selected;
            saveSettings();
            loadRiskModNames();
            if (currentMods != null) {
                updateModsPanel(currentMods);
            }
        }
    }

    private void applySavedSettings() {
        if (minecraftPathSetting != null && !minecraftPathSetting.isEmpty()) {
            File dir = new File(minecraftPathSetting);
            if (dir.exists() && dir.isDirectory()) {
                minecraftPathField.setText(minecraftPathSetting);
                minecraftDir = dir;
                scanVersions();
                if (lastVersionSetting != null && !lastVersionSetting.isEmpty()) {
                    versionCombo.setSelectedItem(lastVersionSetting);
                }
            }
        }
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // .minecraft 目录
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel(getString("label_minecraft_dir")), gbc);

        minecraftPathField = new JTextField();
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(minecraftPathField, gbc);

        JButton browseButton = new JButton(getString("button_browse"));
        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(browseButton, gbc);

        // 版本选择
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(new JLabel(getString("label_version")), gbc);

        versionCombo = new JComboBox<>();
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(versionCombo, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(new JLabel(""), gbc);

        // 风险配置文件选择
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        panel.add(new JLabel(getString("label_risk_config")), gbc);

        riskConfigCombo = new JComboBox<>();
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(riskConfigCombo, gbc);

        JButton refreshRiskButton = new JButton(getString("button_refresh_risk"));
        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(refreshRiskButton, gbc);

        // 事件绑定
        browseButton.addActionListener(e -> chooseMinecraftDirectory());
        versionCombo.addActionListener(e -> {
            if (versionCombo.getSelectedItem() != null && minecraftDir != null) {
                String selectedVersion = (String) versionCombo.getSelectedItem();
                lastVersionSetting = selectedVersion;
                saveSettings();
                determineModsDir(selectedVersion);
            }
        });
        riskConfigCombo.addActionListener(e -> onRiskConfigChanged());
        refreshRiskButton.addActionListener(e -> {
            refreshRiskConfigList();
            loadRiskModNames();
            String selectedVersion = (String) versionCombo.getSelectedItem();
            if (selectedVersion != null && minecraftDir != null) {
                determineModsDir(selectedVersion);
            }
        });

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        riskActionButton = new JButton();
        applyButton = new JButton(getString("button_apply"));

        panel.add(riskActionButton);
        panel.add(applyButton);

        applyButton.addActionListener(e -> applyChanges());

        return panel;
    }

    private void chooseMinecraftDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(getString("message_choose_minecraft"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            minecraftPathField.setText(selected.getAbsolutePath());
            minecraftDir = selected;
            minecraftPathSetting = selected.getAbsolutePath();
            saveSettings();
            scanVersions();
        }
    }

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
                    String.format(getString("message_mods_folder_not_exist"), currentModsDir.getAbsolutePath()),
                    getString("title"), JOptionPane.ERROR_MESSAGE);
            applyButton.setEnabled(false);
            riskActionButton.setEnabled(false);
            return;
        }

        scanModsInBackground();
    }

    private void scanModsInBackground() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        applyButton.setEnabled(false);
        riskActionButton.setEnabled(false);

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
                    applyButton.setEnabled(false);
                    riskActionButton.setEnabled(true);
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(DisableServerRiskMods.this,
                            String.format(getString("message_scan_mods_failed"), e.getMessage()),
                            getString("title"), JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        worker.execute();
    }

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

    private void updateModsPanel(List<ModInfo> mods) {
        modsPanel.removeAll();
        Map<ModCheckBox, Boolean> initialStates = new HashMap<>();

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
            initialStates.put(checkBox, mod.enabled);
        }

        for (ModCheckBox box : initialStates.keySet()) {
            box.addItemListener(e -> {
                boolean changed = false;
                for (ModCheckBox cb : initialStates.keySet()) {
                    if (cb.isSelected() != initialStates.get(cb)) {
                        changed = true;
                        break;
                    }
                }
                hasUnsavedChanges = changed;
                applyButton.setEnabled(hasUnsavedChanges);
            });
        }

        modsPanel.revalidate();
        modsPanel.repaint();

        // 更新动态按钮状态（根据当前风险模组的实际启用/禁用情况）
        updateRiskButtonState();
    }

    /**
     * 根据当前存在的风险模组状态，更新 riskActionButton 的文本和动作
     */
    private void updateRiskButtonState() {
        if (currentMods == null) {
            riskActionButton.setEnabled(false);
            return;
        }

        // 找出所有存在于当前模组列表中的风险模组
        List<ModInfo> presentRiskMods = new ArrayList<>();
        for (ModInfo mod : currentMods) {
            if (riskModNames.contains(mod.name)) {
                presentRiskMods.add(mod);
            }
        }

        if (presentRiskMods.isEmpty()) {
            // 没有风险模组存在，禁用按钮
            riskActionButton.setEnabled(false);
            riskActionButton.setText(getString("button_disable_risk")); // 保留原文本
            return;
        }

        // 检查是否全部禁用
        boolean allDisabled = true;
        for (ModInfo mod : presentRiskMods) {
            if (mod.enabled) {
                allDisabled = false;
                break;
            }
        }

        // 设置按钮文本和动作
        if (allDisabled) {
            riskActionButton.setText(getString("button_enable_risk"));
            // 移除旧监听器，添加新监听器
            for (ActionListener al : riskActionButton.getActionListeners()) {
                riskActionButton.removeActionListener(al);
            }
            riskActionButton.addActionListener(e -> {
                // 启用所有存在的风险模组
                for (Component comp : modsPanel.getComponents()) {
                    if (comp instanceof ModCheckBox) {
                        ModCheckBox box = (ModCheckBox) comp;
                        if (riskModNames.contains(box.modInfo.name)) {
                            box.setSelected(true);
                        }
                    }
                }
                applyChanges();
            });
        } else {
            riskActionButton.setText(getString("button_disable_risk"));
            for (ActionListener al : riskActionButton.getActionListeners()) {
                riskActionButton.removeActionListener(al);
            }
            riskActionButton.addActionListener(e -> {
                // 禁用所有存在的风险模组
                for (Component comp : modsPanel.getComponents()) {
                    if (comp instanceof ModCheckBox) {
                        ModCheckBox box = (ModCheckBox) comp;
                        if (riskModNames.contains(box.modInfo.name)) {
                            box.setSelected(false);
                        }
                    }
                }
                applyChanges();
            });
        }
        riskActionButton.setEnabled(true);
    }

    private void applyChanges() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        applyButton.setEnabled(false);
        riskActionButton.setEnabled(false);

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
                                String.format(getString("message_rename_failed"),
                                        currentFile.getName(), targetFile.getName(), e.getMessage()),
                                getString("title"), JOptionPane.ERROR_MESSAGE));
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