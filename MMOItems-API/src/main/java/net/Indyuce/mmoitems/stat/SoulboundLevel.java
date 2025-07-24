package net.Indyuce.mmoitems.stat;

import io.lumine.mythic.lib.api.item.ItemTag;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.util.NumericStatFormula;
import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

@HasCategory(cat = "soulbound")
public class SoulboundLevel extends DoubleStat {
    public SoulboundLevel() {
        super("SOULBOUND_LEVEL", Material.ENDER_EYE, "Soulbinding Level", new String[]{"The soulbound level defines how much", "damage players will take when trying", "to use a soulbound item. It also determines", "how hard it is to break the binding."}, new String[]{"consumable"});
    }

    // Writes soulbound level with roman writing in lore
    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull DoubleData data) {
        int value = (int) data.getValue();
        item.addItemTag(new ItemTag("MMOITEMS_SOULBOUND_LEVEL", value));
        item.getLore().insert("soulbound-level", getGeneralStatFormat().replace("{value}", MMOUtils.intToRoman(value)));
    }

    @Override
    public void whenPreviewed(@NotNull ItemStackBuilder item, @NotNull DoubleData currentData, @NotNull NumericStatFormula templateData) throws IllegalArgumentException {
        whenApplied(item, currentData);
    }
}
