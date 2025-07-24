package net.Indyuce.mmoitems.stat;

import io.lumine.mythic.lib.api.item.ItemTag;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.item.mmoitem.ReadMMOItem;
import net.Indyuce.mmoitems.api.util.NumericStatFormula;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.GemStoneStat;
import org.bukkit.Material;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Item damage is the item's vanilla damage. This is not the item's current
 * durability, these two values are linked by the following formula:
 * `max_durability = durability + damage`
 *
 * @author indyuce
 * @implNote Due to 1.20.5+ modifying the location of many vanilla tags,
 * all version-friendly implementations must use ItemMeta methods.
 */
public class ItemDamage extends DoubleStat implements GemStoneStat {
    public ItemDamage() {
        super("ITEM_DAMAGE", Material.FISHING_ROD, "Base Item Damage",
                new String[]{"Default item damage. This does &cNOT", "impact the item's max durability."}, new String[]{"!block", "all"});
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull DoubleData data) {
        if (item.getMeta() instanceof Damageable) {
            final int dmg = (int) data.getValue();
            if (dmg > 0) ((Damageable) item.getMeta()).setDamage(dmg);
        }
    }

    @Override
    public void whenPreviewed(@NotNull ItemStackBuilder item, @NotNull DoubleData currentData, @NotNull NumericStatFormula templateData) throws IllegalArgumentException {
        whenApplied(item, currentData);
    }

    /**
     * This stat is saved not as a custom tag, but as the vanilla HideFlag itself.
     * Alas this is an empty array
     */
    @NotNull
    @Override
    public ArrayList<ItemTag> getAppliedNBT(@NotNull DoubleData data) {
        return new ArrayList<>();
    }

    @Override
    public void whenLoaded(@NotNull ReadMMOItem mmoitem) {
        if (mmoitem.getNBT().getItem().getItemMeta() instanceof Damageable) {
            final int dmg = ((Damageable) mmoitem.getNBT().getItem().getItemMeta()).getDamage();
            if (dmg > 0) mmoitem.setData(ItemStats.ITEM_DAMAGE, new DoubleData(dmg));
        }
    }

    /**
     * This stat is saved not as a custom tag, but as the vanilla HideFlag itself.
     * Alas this method returns null.
     */
    @Nullable
    @Override
    public DoubleData getLoadedNBT(@NotNull ArrayList<ItemTag> storedTags) {
        return null;
    }

    @Override
    public String getLegacyTranslationPath() {
        return "durability";
    }
}
