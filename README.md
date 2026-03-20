# Disable Server Risk Mods

A Java Swing GUI tool for Minecraft players to quickly disable mods that might be rejected by server anti-cheat systems. It enables/disables mods by renaming files (appending `.disabled`), which is safe and reversible—no files are deleted.

> **Note**: This code is generated based on conversations with DeepSeek.

> **UI Language**: Currently, the user interface is only available in **Chinese** (Simplified). English UI may be added in future versions.

## Configuration File

The configuration file is named **`disabled-mods.txt`** and must be placed in the same directory as the program JAR. Its format is as follows:

1. **First line**: The default `.minecraft` directory path (optional; can be empty or commented with `#`).
2. **Subsequent lines**: One risk mod name per line (the `name` field from `fabric.mod.json`). Empty lines and lines starting with `#` are ignored.

**Example**:
```
C:\Users\Alice\AppData\Roaming\.minecraft
# Risk mods list
Xaero's Minimap
Inventory HUD+
Tweakeroo
```

> **Note**: The default configuration file provided in this repository contains only a **small set of example mods**. You should edit it to include the actual mods you need to disable based on your server's rules.

If the configuration file does not exist, the program will create an empty one and prompt you to edit it.

## How to Use

1. **Run the program**:
   - Ensure Java 8 or higher is installed.
   - Double‑click the JAR file or run in terminal: `java -jar DisableServerRiskMods.jar`

2. **First launch**:
   - The program will notify you if the config file is missing. Edit `disabled-mods.txt` to set your `.minecraft` path and risk mod names.

3. **Select directory and version**:
   - Click "Browse" to choose your `.minecraft` directory (if already set in config, it will be loaded automatically).
   - Choose the Minecraft version from the dropdown. The program will locate the corresponding `mods` folder (preferring the version‑isolated path, falling back to the root `mods` folder).

4. **View mods list**:
   - All Fabric mods are displayed as checkboxes. A checked box means the mod is currently enabled.
   - Risk mod names appear in **red**; others are black.

5. **Disable risk mods**:
   - Click the "禁用风险模组" button. All risk mod checkboxes will be unchecked and changes applied immediately (equivalent to clicking "应用更改").

6. **Manual adjustments**:
   - Manually check/uncheck mods as desired, then click "应用更改" to rename the corresponding files.

7. **Effect**:
   - After applying, mod files are renamed: enabled files are `modname.jar`, disabled files are `modname.jar.disabled`.
   - **You must restart Minecraft** for the changes to take effect.

## Important Notes

- **Fabric only**: This tool recognizes Fabric mods by reading `fabric.mod.json`. Forge or other mod loaders are not supported.
- **File renaming**: Disabling/enabling is done by renaming files. No files are deleted. If the target filename already exists (e.g., both `.jar` and `.jar.disabled` exist), the program will first rename the target to a `.bak` backup before moving.
- **Risk mod names**: Use the exact `name` field from `fabric.mod.json` (case‑sensitive), not the filename. To find it, open the mod JAR with an archive tool and look at `fabric.mod.json`.
- **Avoid manual editing**: Do not manually rename files in the `mods` folder while the program is running, as it may cause inconsistency.
- **Multiple versions**: The tool handles each Minecraft version separately; mods for different versions are stored in their respective `mods` folders.

## Build and Run

### Option 1: Compile and run directly (no dependencies)
```bash
javac DisableServerRiskMods.java
java DisableServerRiskMods
```

### Option 2: Package as an executable JAR
1. Compile and create a manifest file (e.g., `MANIFEST.MF`):
```
Main-Class: DisableServerRiskMods
```
2. Package:
```bash
jar cfm DisableServerRiskMods.jar MANIFEST.MF *.class
```
3. Run:
```bash
java -jar DisableServerRiskMods.jar
```

## License

This project is open‑sourced under the MIT License. See the [LICENSE](LICENSE) file for details.

## Feedback & Contributions

Issues and pull requests are welcome!

---

**Happy Minecrafting!**

---

# 禁用服务器风险模组

一个 Java Swing 图形界面工具，专为 Minecraft 玩家设计，用于快速禁用整合包中可能被服务器反作弊系统拒绝的风险模组。通过重命名文件（添加 `.disabled` 后缀）实现模组的启用/禁用，无需删除文件，安全且可逆。

> **说明**：本代码基于与 DeepSeek 的对话生成

> **界面语言**：当前用户界面仅支持**中文**（简体），暂未提供英文界面。

## 配置文件说明

文件名为 **`disabled-mods.txt`**，与程序 JAR 文件放在同一目录下。格式如下：

1. **第一行**：默认的 `.minecraft` 目录路径（可选，可以留空或使用 `#` 注释）。
2. **后续行**：每行一个风险模组的**名称**（即 `fabric.mod.json` 中的 `name` 字段），空行和以 `#` 开头的行会被忽略。

**示例**：
```
C:\Users\Alice\AppData\Roaming\.minecraft
# 风险模组列表
Xaero的小地图
Inventory HUD+
Tweakeroo
```

> **注意**：本仓库提供的默认配置文件仅包含**少量示例模组**，你需要根据实际服务器规则编辑该文件，添加你需要禁用的模组名称。

如果配置文件不存在，程序会自动创建一个空文件，并提示用户编辑。

## 使用方法

1. **首次运行**：
   - 程序会提示创建配置文件。编辑 `disabled-mods.txt`，填入 `.minecraft` 路径和风险模组名称。

2. **选择目录与版本**：
   - 在 GUI 中点击“浏览”选择 `.minecraft` 目录（如果配置文件中已填写，会自动加载）。
   - 从下拉框选择要管理的 Minecraft 版本。程序会自动定位到对应的 `mods` 文件夹（优先使用版本隔离目录，不存在则回退到根目录 `mods`）。

3. **查看模组列表**：
   - 所有 Fabric 模组将以复选框形式列出，当前启用状态由复选框勾选表示。
   - 风险模组名称显示为**红色**，其他为黑色。

4. **禁用风险模组**：
   - 点击“禁用风险模组”按钮，所有风险模组的复选框将自动取消勾选，并立即应用更改（相当于执行了一次“应用更改”）。

5. **手动调整**：
   - 手动勾选/取消勾选需要更改的模组，然后点击“应用更改”按钮生效。

8. **生效**：
   - 应用更改后，模组文件会被重命名：启用状态的文件名为 `模组名.jar`，禁用状态的文件名为 `模组名.jar.disabled`。
   - **需要重启 Minecraft** 才能使更改生效。

## 注意事项

- **仅支持 Fabric 模组**：程序通过读取 `fabric.mod.json` 获取模组名称，Forge 或其他模组加载器无法识别。
- **文件重命名**：禁用/启用通过重命名文件实现，不会删除任何文件。如果目标文件已存在（例如同时存在 `.jar` 和 `.jar.disabled`），程序会先将目标文件重命名为 `.bak` 备份，再执行移动操作。
- **风险模组名称**：务必使用 `fabric.mod.json` 中的 `name` 字段（区分大小写），而非文件名。你可以用压缩软件打开一个 Fabric 模组 JAR 文件，查看 `fabric.mod.json` 中的 `"name"` 值。
- **手动操作建议**：不要手动修改 `mods` 文件夹内的文件，以免程序状态与文件状态不一致。
- **多版本管理**：不同 Minecraft 版本的模组可能名称相同，程序会根据所选版本处理对应的 `mods` 文件夹。

## 构建与运行

### 方法一：直接编译运行（无依赖）
```bash
javac DisableServerRiskMods.java
java DisableServerRiskMods
```

### 方法二：打包为可执行 JAR
1. 编译并创建 Manifest 文件（例如 `MANIFEST.MF`）：
```
Main-Class: DisableServerRiskMods
```
2. 打包：
```bash
jar cfm DisableServerRiskMods.jar MANIFEST.MF *.class
```
3. 运行：
```bash
java -jar DisableServerRiskMods.jar
```

## 许可证

本项目基于 MIT 许可证开源，详情请参阅 [LICENSE](LICENSE) 文件。

## 反馈与贡献

如有问题或建议，欢迎提交 Issue 或 Pull Request。

---

**Happy Minecrafting!**
