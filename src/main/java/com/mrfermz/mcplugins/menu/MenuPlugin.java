package com.mrfermz.mcplugins.menu;

import com.mrfermz.mcplugins.core.log.PluginLog;
import com.mrfermz.mcplugins.menu.command.MenuCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * In-game per-player menu. Provides {@code /menu}, which opens a native Paper
 * Dialog built from whatever options other plugins have registered on core's
 * {@code MenuRegistry}, and stores the player's choices through core's
 * {@code PlayerPreferenceService} (the shared central DB).
 *
 * <p>This plugin holds no state and owns no tables — it is purely the front-end.
 * It depends on {@code minecraft-plugin-core} for the registry, the preference
 * store and the logging convention (see the module README).
 */
public final class MenuPlugin extends JavaPlugin {

    private PluginLog log;

    @Override
    public void onEnable() {
        this.log = PluginLog.of(this);

        getCommand("menu").setExecutor(new MenuCommand());

        log.info("Menu UI ready (/menu).");
    }

    @Override
    public void onDisable() {
        if (log != null) {
            log.info("Menu UI disabled.");
        }
    }
}
