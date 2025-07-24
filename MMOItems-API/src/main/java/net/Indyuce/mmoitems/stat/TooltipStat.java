package net.Indyuce.mmoitems.stat;

import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.api.item.ItemTag;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.tooltip.TooltipTexture;
import net.Indyuce.mmoitems.gui.edition.EditionInventory;
import net.Indyuce.mmoitems.stat.data.StringData;
import net.Indyuce.mmoitems.stat.type.GemStoneStat;
import net.Indyuce.mmoitems.stat.type.StringStat;
import io.lumine.mythic.lib.util.lang3.Validate;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TooltipStat extends StringStat implements GemStoneStat {
    public TooltipStat() {
        super("TOOLTIP", Material.BIRCH_SIGN, "Custom Tooltip", new String[]{"The identifier of the custom MMOItems tooltip texture", "you'd like to use. Check the wiki for usage!"}, new String[0]);
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull StringData data) {
        final String format = UtilityMethods.enumName(data.toString());
        final @Nullable TooltipTexture texture = MMOItems.plugin.getLore().getTooltip(format);
        Validate.notNull(texture, "Could not find tooltip with ID '" + format + "'");
        item.addItemTag(new ItemTag("MMOITEMS_TOOLTIP", texture.getId()));
    }

    @Override
    public void whenInput(@NotNull EditionInventory inv, @NotNull String message, Object... info) {
        final String format = UtilityMethods.enumName(message);
        Validate.isTrue(MMOItems.plugin.getLore().hasTooltip(format), "Couldn't find tooltip with ID '" + format + "'");

        inv.getEditedSection().set(getPath(), format);
        inv.registerTemplateEdition();
        inv.getPlayer().sendMessage(MMOItems.plugin.getPrefix() + "Tier successfully changed to " + format + ".");
    }
}
