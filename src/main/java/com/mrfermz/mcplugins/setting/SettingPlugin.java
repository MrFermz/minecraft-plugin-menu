package com.mrfermz.mcplugins.setting;

import com.mrfermz.mcplugins.core.log.PluginLog;
import com.mrfermz.mcplugins.setting.command.SettingCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * In-game per-player settings UI. Provides {@code /setting}, which opens a native
 * Paper Dialog built from whatever settings other plugins have registered on
 * core's {@code SettingsRegistry}, and stores the player's choices through core's
 * {@code PlayerPreferenceService} (the shared central DB).
 *
 * <p>This plugin holds no state and owns no tables — it is purely the front-end.
 * It depends on {@code minecraft-plugin-core} for the registry, the preference
 * store and the logging convention (see the module README).
 */
public final class SettingPlugin extends JavaPlugin {

    private PluginLog log;

    @Override
    public void onEnable() {
        this.log = PluginLog.of(this);

        getCommand("setting").setExecutor(new SettingCommand());

        log.info("Settings UI ready (/setting).");
    }

    @Override
    public void onDisable() {
        if (log != null) {
            log.info("Settings UI disabled.");
        }
    }
}
