package net.Indyuce.mmoitems.command;

import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.command.argument.Argument;
import io.lumine.mythic.lib.command.argument.ArgumentParseException;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Arguments {

    public static final Argument<Type> ITEM_TYPE = new Argument<>("type",
            (explorer, list) -> MMOItems.plugin.getTypes().getAll().forEach(type -> list.add(type.getId())),
            (explorer, input) -> {
                final var type = Type.get(UtilityMethods.enumName(input));
                if (type == null) throw new ArgumentParseException("No such item type '" + input + "'");
                return type;
            });

    public static final Argument<String> ITEM_ID_2 = new Argument<>("id", (explorer, list) -> {
        try {
            Type type = Type.get(explorer.getArguments()[1]);
            if (type == null) return;
            MMOItems.plugin.getTemplates().getTemplates(type).forEach(template -> list.add(template.getId()));
        } catch (Exception ignored) {
        }
    }, (explorer, input) -> UtilityMethods.enumName(input));

    public static final Argument<ItemStat<?, ?>> ITEM_STAT = new Argument<>("STAT_ID",
            (explorer, list) -> MMOItems.plugin.getStats().getAll().forEach(stat -> list.add(stat.getId())),
            (explorer, input) -> {
                final var stat = MMOItems.plugin.getStats().get(UtilityMethods.enumName(input));
                if (stat == null) throw new ArgumentParseException("Could not find stat with ID '" + input + "'");
                return stat;
            });

    /**
     * Defaults to 0%
     */
    public static final Argument<Double> CHANCE = Argument.AMOUNT_DOUBLE
            .withFallback(explore -> 0d)
            .withAutoComplete((explore, list) -> list.addAll(Arrays.asList("0", "25", "50", "75", "100")));

    @NotNull
    public static MMOItemTemplate getTemplate(@NotNull Type type, @NotNull String id) {
        try {
            return MMOItems.plugin.getTemplates().getTemplateOrThrow(type, id);
        } catch (Exception exception) {
            throw new ArgumentParseException("No item with ID '" + id + "' for type '" + type.getId() + "'");
        }
    }
}
