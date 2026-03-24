# 禁用服务器风险模组

一个 Java Swing 图形界面工具，专为 Minecraft 玩家设计，用于快速禁用整合包中可能被服务器反作弊系统拒绝的风险模组。通过重命名文件（添加 `.disabled` 后缀）实现模组的启用/禁用，无需删除文件，安全且可逆。

> **说明**：本代码基于与 DeepSeek 的对话生成

> **界面语言**：当前用户界面支持 **简体中文** 若没有简体中文界面可尝试将 `DSRMdata/setting.ini`中的 `language` 项改为 `DSRMdata/zh-cn.json`

## 其他语言

1. [英文](README.md)

## 配置文件说明

所有风险模组配置均存放在程序 JAR 同目录下的 **`risk_configs`** 文件夹中，程序启动时会自动创建。你可以创建多个 `.txt` 文件，每个文件内每行一个模组名称。程序会记住上次使用的配置文件（保存在 `settings.ini` 中）。

**配置文件格式**（例如 `default.txt`）：

- 每行一个风险模组的**名称**（即 `fabric.mod.json` 中的 `name` 字段）。
- 空行和以 `#` 开头的行会被忽略。

**示例**：

```txt
# 风险模组列表
Xaero's Minimap
Inventory HUD+
Tweakeroo
```

> **注意**：如果 `risk_configs` 文件夹为空，程序会自动创建 `default.txt` 空文件。你需要根据实际服务器规则编辑该文件，添加需要禁用的模组名称。

## 使用方法

1. **运行程序**：

   - 确保已安装 Java 8 或更高版本。
   - 双击 JAR 文件，或在终端中执行 `java -jar DisableServerRiskMods.jar`。
2. **选择目录与版本**：

   - 在 GUI 中点击“浏览”选择 `.minecraft` 目录。
   - 从下拉框选择要管理的 Minecraft 版本。程序会自动定位到对应的 `mods` 文件夹。
3. **查看模组列表**：

   - 所有 Fabric 模组将以复选框形式列出，当前启用状态由复选框勾选表示。
   - 风险模组名称（根据当前选中的配置文件）显示为**红色**，其他为黑色。
4. **禁用风险模组**：

   - 点击“禁用配置的模组项”按钮，所有风险模组的复选框将自动取消勾选，并立即应用更改。
5. **手动调整**：

   - 手动勾选/取消勾选需要更改的模组。然后按“应用更改”按钮即可。

- **刷新**：

  - 点击“刷新”按钮会重新加载风险配置文件列表、重新读取当前配置内容，并重新扫描当前版本的 `mods` 文件夹，更新模组列表及风险模组的红色标记。如果你在程序运行期间编辑了配置文件，可以用此按钮立即生效。
- **生效**：

  - 应用更改后，模组文件会被重命名：启用状态的文件名为 `模组名.jar`，禁用状态的文件名为 `模组名.jar.disabled`。
  - **需要关闭 Minecraft** 才能使更改生效。

## 注意事项

- **仅支持 Fabric 模组**：程序通过读取 `fabric.mod.json` 获取模组名称，Forge 或其他模组加载器无法识别。
- **文件重命名**：禁用/启用通过重命名文件实现，不会删除任何文件。如果目标文件已存在（例如同时存在 `.jar` 和 `.jar.disabled`），程序会先将目标文件重命名为 `.bak` 备份，再执行移动操作。
- **风险模组名称**：务必使用 `fabric.mod.json` 中的 `name` 字段（区分大小写），而非文件名。你可以用压缩软件打开一个 Fabric 模组 JAR 文件，查看 `fabric.mod.json` 中的 `"name"` 值。
- **手动操作建议**：不要手动修改 `mods` 文件夹内的文件，以免程序状态与文件状态不一致。
- **多版本管理**：不同 Minecraft 版本的模组可能名称相同，程序会根据所选版本处理对应的 `mods` 文件夹。

## 构建与运行

1. 下载源代码：

```bash
cd 你的.minecraft所在目录
git clone https://github.com/lin1526615/DisableServerRiskMods_Fabric.git
cd DisableServerRiskMods_Fabric
```

2. 编译：

```bash
Build.bat
```

3. 运行：

```bash
:: 将编译后的 JAR 移动到 .Minecraft 所在目录下
move DisableServerRiskMods.jar ..
:: 创建 DSRMdata 文件夹
copy DSRMdata ..
java -jar ../DisableServerRiskMods.jar
```

## 许可证

本项目基于 MIT 许可证开源，详情请参阅 [LICENSE](LICENSE) 文件。

## 反馈与贡献

如有问题或建议，欢迎提交 Issue 或 Pull Request。

---

**Happy Minecrafting!**