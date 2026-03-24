javac -encoding utf-8 -d Build_class DisableServerRiskMods.java
jar cvfm DisableServerRiskMods.jar Build_class/MANIFEST.MF -C Build_class .