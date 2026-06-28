package com.mrfermz.mcplugins.setting.ui;

import com.mrfermz.mcplugins.core.settings.PlayerPreferenceService;
import com.mrfermz.mcplugins.core.settings.SettingDefinition;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * Builds and opens the per-player settings dialog from the registered
 * {@link SettingDefinition}s, using Paper's native Dialog API — toggles, dropdowns
 * and sliders render as a real form (no chest-inventory hacks).
 *
 * <p>Current values are pre-filled from {@link PlayerPreferenceService}; clicking
 * <em>Save</em> writes every input back through the same service, which updates
 * its cache immediately so the change takes effect in real time. Pressing escape
 * cancels without saving.
 */
public final class SettingsDialog {

    private SettingsDialog() {
    }

    /** Shows the settings dialog to {@code player}. */
    public static void open(Player player, List<SettingDefinition> definitions,
                            PlayerPreferenceService prefs) {
        List<DialogInput> inputs = new ArrayList<>(definitions.size());
        for (SettingDefinition def : definitions) {
            inputs.add(toInput(def, player.getUniqueId(), prefs));
        }

        ActionButton save = ActionButton.builder(Component.text("Save", NamedTextColor.GREEN))
                .tooltip(Component.text("Save your settings"))
                .action(DialogAction.customClick(
                        (view, audience) -> applyAndSave(view, player, definitions, prefs),
                        ClickCallback.Options.builder().uses(1).build()))
                .build();

        DialogBase base = DialogBase.builder(Component.text("Settings"))
                .canCloseWithEscape(true)
                .inputs(inputs)
                .build();

        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(base)
                .type(DialogType.notice(save)));

        player.showDialog(dialog);
    }

    /** Maps one setting definition to the matching Dialog input, pre-filled. */
    private static DialogInput toInput(SettingDefinition def, UUID player, PlayerPreferenceService prefs) {
        Component label = Component.text(def.title());
        return switch (def.type()) {
            case TOGGLE -> DialogInput.bool(def.key(), label)
                    .initial(prefs.getBoolean(player, def.key(), Boolean.parseBoolean(def.defaultValue())))
                    .build();
            case CHOICE -> {
                String current = prefs.get(player, def.key(), def.defaultValue());
                List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();
                for (SettingDefinition.Option opt : def.options()) {
                    entries.add(SingleOptionDialogInput.OptionEntry.create(
                            opt.value(), Component.text(opt.label()), opt.value().equals(current)));
                }
                yield DialogInput.singleOption(def.key(), label, entries).build();
            }
            case NUMBER -> {
                float current = (float) prefs.getDouble(player, def.key(),
                        Double.parseDouble(def.defaultValue()));
                var builder = DialogInput.numberRange(def.key(), label, (float) def.min(), (float) def.max())
                        .initial(current);
                if (def.step() > 0) {
                    builder = builder.step((float) def.step());
                }
                yield builder.build();
            }
            case TEXT -> DialogInput.text(def.key(), label)
                    .initial(prefs.get(player, def.key(), def.defaultValue()))
                    .build();
        };
    }

    /** Reads each input from the submitted response and persists it. */
    private static void applyAndSave(DialogResponseView view, Player player,
                                     List<SettingDefinition> definitions, PlayerPreferenceService prefs) {
        UUID id = player.getUniqueId();
        int changed = 0;
        for (SettingDefinition def : definitions) {
            String value = readValue(view, def);
            if (value == null) {
                continue;
            }
            prefs.set(id, def.key(), value, id);
            changed++;
        }
        player.sendMessage(Component.text("Saved " + changed + " setting(s).", NamedTextColor.GREEN));
    }

    /** Pulls one value out of the response in the type the input produced. */
    private static String readValue(DialogResponseView view, SettingDefinition def) {
        return switch (def.type()) {
            case TOGGLE -> {
                Boolean b = view.getBoolean(def.key());
                yield b == null ? null : Boolean.toString(b);
            }
            case CHOICE, TEXT -> view.getText(def.key());
            case NUMBER -> {
                Float f = view.getFloat(def.key());
                yield f == null ? null : trimNumber(f);
            }
        };
    }

    /** Formats a slider value without a needless trailing {@code .0}. */
    private static String trimNumber(float f) {
        if (!Float.isInfinite(f) && !Float.isNaN(f) && f == Math.rint(f)) {
            return Long.toString((long) f);
        }
        return Float.toString(f);
    }
}
