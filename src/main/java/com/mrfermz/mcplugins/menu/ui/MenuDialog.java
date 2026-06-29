package com.mrfermz.mcplugins.menu.ui;

import com.mrfermz.mcplugins.core.menu.MenuItem;
import com.mrfermz.mcplugins.core.menu.PlayerPreferenceService;
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
 * Builds and opens the per-player menu from the registered
 * {@link MenuItem}s, using Paper's native Dialog API — toggles, dropdowns
 * and sliders render as a real form (no chest-inventory hacks).
 *
 * <p>Current values are pre-filled from {@link PlayerPreferenceService}; clicking
 * <em>Save</em> writes every input back through the same service, which updates
 * its cache immediately so the change takes effect in real time. Pressing escape
 * cancels without saving.
 */
public final class MenuDialog {

    private MenuDialog() {
    }

    /** Shows the menu dialog to {@code player}. */
    public static void open(Player player, List<MenuItem> definitions,
                            PlayerPreferenceService prefs) {
        // Dialog input keys are used as command-macro names, so they may only be
        // [A-Za-z0-9_] — our setting keys contain dots. Use a positional key
        // ("s0", "s1", …) for each input and map it back by index on save.
        List<DialogInput> inputs = new ArrayList<>(definitions.size());
        for (int i = 0; i < definitions.size(); i++) {
            inputs.add(toInput(definitions.get(i), inputKey(i), player.getUniqueId(), prefs));
        }

        ActionButton save = ActionButton.builder(Component.text("Save", NamedTextColor.GREEN))
                .tooltip(Component.text("Save your changes"))
                .action(DialogAction.customClick(
                        (view, audience) -> applyAndSave(view, player, definitions, prefs),
                        ClickCallback.Options.builder().uses(1).build()))
                .build();

        // Cancel just closes the dialog (afterAction CLOSE) without persisting —
        // no PlayerPreferenceService.set() is called.
        ActionButton cancel = ActionButton.builder(Component.text("Cancel", NamedTextColor.RED))
                .tooltip(Component.text("Close without saving"))
                .action(DialogAction.customClick(
                        (view, audience) -> player.sendMessage(
                                Component.text("Cancelled — nothing was saved.", NamedTextColor.GRAY)),
                        ClickCallback.Options.builder().uses(1).build()))
                .build();

        DialogBase base = DialogBase.builder(Component.text("Menu"))
                .canCloseWithEscape(true)
                .inputs(inputs)
                .build();

        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(base)
                .type(DialogType.multiAction(List.of(save, cancel)).columns(2).build()));

        player.showDialog(dialog);
    }

    /** The macro-safe dialog input key for the definition at {@code index}. */
    private static String inputKey(int index) {
        return "s" + index;
    }

    /** Maps one setting definition to the matching Dialog input, pre-filled. */
    private static DialogInput toInput(MenuItem def, String inputKey, UUID player,
                                       PlayerPreferenceService prefs) {
        Component label = Component.text(def.title());
        return switch (def.type()) {
            case TOGGLE -> DialogInput.bool(inputKey, label)
                    .initial(prefs.getBoolean(player, def.key(), Boolean.parseBoolean(def.defaultValue())))
                    .build();
            case CHOICE -> {
                String current = prefs.get(player, def.key(), def.defaultValue());
                List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();
                for (MenuItem.Option opt : def.options()) {
                    entries.add(SingleOptionDialogInput.OptionEntry.create(
                            opt.value(), Component.text(opt.label()), opt.value().equals(current)));
                }
                yield DialogInput.singleOption(inputKey, label, entries).build();
            }
            case NUMBER -> {
                float current = (float) prefs.getDouble(player, def.key(),
                        Double.parseDouble(def.defaultValue()));
                var builder = DialogInput.numberRange(inputKey, label, (float) def.min(), (float) def.max())
                        .initial(current);
                if (def.step() > 0) {
                    builder = builder.step((float) def.step());
                }
                yield builder.build();
            }
            case TEXT -> DialogInput.text(inputKey, label)
                    .initial(prefs.get(player, def.key(), def.defaultValue()))
                    .build();
        };
    }

    /** Reads each input from the submitted response and persists it. */
    private static void applyAndSave(DialogResponseView view, Player player,
                                     List<MenuItem> definitions, PlayerPreferenceService prefs) {
        UUID id = player.getUniqueId();
        int changed = 0;
        for (int i = 0; i < definitions.size(); i++) {
            MenuItem def = definitions.get(i);
            String value = readValue(view, def, inputKey(i));
            if (value == null) {
                continue;
            }
            prefs.set(id, def.key(), value, id);
            changed++;
        }
        player.sendMessage(Component.text("Saved " + changed + " setting(s).", NamedTextColor.GREEN));
    }

    /** Pulls one value out of the response in the type the input produced. */
    private static String readValue(DialogResponseView view, MenuItem def, String inputKey) {
        return switch (def.type()) {
            case TOGGLE -> {
                Boolean b = view.getBoolean(inputKey);
                yield b == null ? null : Boolean.toString(b);
            }
            case CHOICE, TEXT -> view.getText(inputKey);
            case NUMBER -> {
                Float f = view.getFloat(inputKey);
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
