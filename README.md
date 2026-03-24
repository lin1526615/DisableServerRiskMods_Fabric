# Disable Server Risk Mods

A Java Swing GUI tool designed for Minecraft players to quickly disable risk mods that may be rejected by server anti-cheat systems. Mods are enabled/disabled by renaming files (adding a `.disabled` suffix), which is safe and reversible without deleting files.

> **Note**: This code was generated with the help of DeepSeek.

> **Interface Language**: The UI currently supports **English** and **Simplified Chinese**. To switch the interface language, modify the `language` option in `DSRMdata/settings.ini`. For English, set `language=DSRMdata/en.json`; for Chinese, set `language=DSRMdata/zh-cn.json`.

## Other Languages

1. [简体中文](README.zh_cn.md)

## Configuration File Description

All risk mod configurations are stored in the **`risk_configs`** folder located in the same directory as the program JAR. This folder is automatically created when the program starts. You can create multiple `.txt` files, each containing one mod name per line. The program remembers the last used configuration file (saved in `settings.ini`).

**Configuration file format** (e.g., `default.txt`):

- One risk mod **name** per line (the `name` field in `fabric.mod.json`).
- Blank lines and lines starting with `#` are ignored.

**Example**:

```txt
# List of risk mods
Xaero's Minimap
Inventory HUD+
Tweakeroo
```

> **Note**: If the `risk_configs` folder is empty, the program automatically creates an empty `default.txt` file. You need to edit this file according to the actual server rules and add the names of mods that must be disabled.

## How to Use

1. **Run the program**:
   - Ensure Java 8 or higher is installed.
   - Double-click the JAR file, or execute `java -jar DisableServerRiskMods.jar` in the terminal.
2. **Select directory and version**:
   - Click "Browse" to choose the `.minecraft` directory.
   - Select the Minecraft version from the dropdown. The program will automatically locate the corresponding `mods` folder.
3. **View the mod list**:
   - All Fabric mods are displayed as checkboxes. The checked state indicates whether the mod is enabled.
   - Risk mod names (based on the currently selected configuration file) are shown in **red**, others in black.
4. **Disable risk mods**:
   - Click the "Disable Configured Risk Mods" button to automatically uncheck all risk mod checkboxes and apply the changes immediately.
5. **Manual adjustments**:
   - Manually check/uncheck mods you wish to change, then press the "Apply Changes" button.
6. **Refresh**:
   - Click the "Refresh" button to reload the list of risk configuration files, re-read the currently selected configuration, rescan the `mods` folder for the current version, and update the mod list and red markings for risk mods. This is useful if you edit a configuration file while the program is running.
7. **Effect**:
   - After applying changes, mod files are renamed: enabled mods remain as `modname.jar`, disabled mods become `modname.jar.disabled`.
   - **Minecraft must be closed** for the changes to take effect.

## Important Notes

- **Fabric mods only**: The program obtains mod names by reading `fabric.mod.json`. Forge or other mod loaders are not supported.
- **File renaming**: Enabling/disabling is done by renaming files; no files are deleted. If the target file already exists (e.g., both `.jar` and `.jar.disabled` exist), the program first renames the target file to a `.bak` backup before performing the move.
- **Risk mod names**: Use the exact `name` field from `fabric.mod.json` (case‑sensitive), not the filename. You can open a Fabric mod JAR with a compression tool and look for the `"name"` value inside `fabric.mod.json`.
- **Manual manipulation**: Avoid manually editing files in the `mods` folder to keep the program’s state consistent with the actual file state.
- **Multiple versions**: Mod names may be the same across different Minecraft versions; the program handles the appropriate `mods` folder based on the selected version.

## Build & Run

1. Clone the source code:
   ```bash
   cd your-minecraft-directory
   git clone https://github.com/lin1526615/DisableServerRiskMods_Fabric.git
   cd DisableServerRiskMods_Fabric
   ```
2. Compile:
   ```bash
   Build.bat
   ```
3. Run:
   ```bash
   :: Move the compiled JAR to the .minecraft directory
   move DisableServerRiskMods.jar ..
   :: Copy the DSRMdata folder
   copy DSRMdata ..
   java -jar ../DisableServerRiskMods.jar
   ```

## License

This project is open‑sourced under the MIT License. See the [LICENSE](LICENSE) file for details.

## Feedback & Contributions

If you have any questions or suggestions, feel free to open an issue or submit a pull request.

---

**Happy Minecrafting!**