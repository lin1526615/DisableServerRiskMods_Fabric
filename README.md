# Disable Server Risk Mods

A Java Swing GUI tool for Minecraft players to quickly disable mods that might be rejected by server anti-cheat systems. It enables/disables mods by renaming files (appending `.disabled`), which is safe and reversible—no files are deleted.

> **Note**: This code is generated based on conversations with DeepSeek.

> **UI Language**: Currently, the user interface is only available in **Chinese** (Simplified). English UI may be added in future versions.

# Other Languages

1. [中文](README.zh_cn.md)

## Configuration Files

All risk mod configurations are stored in the **`risk_configs`** folder, which is automatically created in the same directory as the program JAR. You can create multiple `.txt` files, each containing a list of mod names to disable. The program remembers the last used configuration file via `settings.ini`.

**Configuration file format** (e.g., `default.txt`):

- Each line contains the exact **`name`** field from `fabric.mod.json` of a risk mod.
- Empty lines and lines starting with `#` are ignored.

**Example**:

```txt
# Risk mods list
Xaero's Minimap
Inventory HUD+
Tweakeroo
```

> **Note**: The program will create a `default.txt` file with an empty list if the folder is missing. You should edit it to include the actual mods you need to disable based on your server's rules.

## How to Use

1. **Run the program**:

   - Ensure Java 8 or higher is installed.
   - Double‑click the JAR file or run in terminal: `java -jar DisableServerRiskMods.jar`
2. **First launch**:

   - The program creates the `risk_configs` folder and a `default.txt` file. Edit the file to add risk mod names.
3. **Select directory and version**:

   - Click "Browse" to choose your `.minecraft` directory (the path is saved in `settings.ini` for next launch).
   - Choose the Minecraft version from the dropdown. The program will locate the corresponding `mods` folder (preferring the version‑isolated path, falling back to the root `mods` folder).
4. **View mods list**:

   - All Fabric mods are displayed as checkboxes. A checked box means the mod is currently enabled.
   - Risk mod names (from the selected configuration) appear in **red**; others are black.
5. **Disable risk mods**:

   - Click the "禁用配置的模组项" button. All risk mod checkboxes will be unchecked and changes applied immediately. This is equivalent to manually unchecking those boxes and clicking "应用更改".
6. **Manual adjustments**:

   - Manually check/uncheck mods as desired. The "应用更改" button becomes enabled only when there are unsaved changes. Click it to rename the corresponding files.

- **Refresh**:

   - The "刷新" button updates the risk configuration list, reloads the current configuration, and re‑scans the mods folder. This is useful if you edit the configuration file while the program is running.

- **Effect**:

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

1. Compile:

```bash
Build.bat
```

2. Run:

```bash
java -jar DisableServerRiskMods.jar
```

## License

This project is open‑sourced under the MIT License. See the [LICENSE](LICENSE) file for details.

## Feedback & Contributions

Issues and pull requests are welcome!

---

**Happy Minecrafting!**