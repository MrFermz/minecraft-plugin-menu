package com.mrfermz.mcplugins.setting.command;

import com.mrfermz.mcplugins.core.CoreApi;
import com.mrfermz.mcplugins.core.settings.PlayerPreferenceService;
import com.mrfermz.mcplugins.core.settings.SettingDefinition;
import com.mrfermz.mcplugins.core.settings.SettingsRegistry;
import com.mrfermz.mcplugins.setting.ui.SettingsDialog;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /setting} — opens the player's personal settings menu (a native Dialog).
 * Pulls the available settings from core's {@link SettingsRegistry} and the
 * player's current values from {@link PlayerPreferenceService}.
 */
public final class SettingCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }
        if (!player.hasPermission("setting.use")) {
            player.sendMessage(Component.text("You don't have permission to open settings.",
                    NamedTextColor.RED));
            return true;
        }

        SettingsRegistry registry = CoreApi.settings(player.getServer()).orElse(null);
        PlayerPreferenceService prefs = CoreApi.preferences(player.getServer()).orElse(null);
        if (registry == null || prefs == null) {
            player.sendMessage(Component.text("Settings are unavailable right now "
                    + "(the core database isn't ready).", NamedTextColor.RED));
            return true;
        }

        List<SettingDefinition> definitions = registry.all();
        if (definitions.isEmpty()) {
            player.sendMessage(Component.text("There are no settings to change yet.",
                    NamedTextColor.YELLOW));
            return true;
        }

        SettingsDialog.open(player, definitions, prefs);
        return true;
    }
}
