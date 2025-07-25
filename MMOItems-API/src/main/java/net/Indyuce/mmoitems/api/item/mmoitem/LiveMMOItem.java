package net.Indyuce.mmoitems.api.item.mmoitem;

import java.util.logging.Level;

import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.SupportedNBTTagValues;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.stat.type.StatHistory;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.NBTItem;

public class LiveMMOItem extends ReadMMOItem {

    /**
     * This class is used to load ALL the data from an item in one constructor.
     * They should be used with care because it is quite performance heavy
     *
     * @param item The item to read
     */
    public LiveMMOItem(ItemStack item) {
        this(MythicLib.plugin.getVersion().getWrapper().getNBTItem(item));
    }

    /**
     * This class is used to load ALL the data from an item in one constructor.
     * They should be used with care because it is quite performance heavy
     *
     * @param item The item to read
     */
    public LiveMMOItem(NBTItem item) {
        super(item);

        /*
         * Reads all the stats that this item may contain. Only the
         * stats declared for this item type were loaded before which
         * caused gem-provided stats to be ignored if the type did not
         * normally support them. Iterate over all registered stats so
         * that any stat present on the item NBT is properly loaded.
         */
        for (ItemStat stat : MMOItems.plugin.getStats().getAll())

            // Attempts to load it
            try {

                // Will not do much if the stat is missing from the item
                stat.whenLoaded(this);

                // History not prematurely loaded?
                if (!hasStatHistory(stat)) {

                    // Also load history :think ing:
                    ItemTag hisTag = ItemTag.getTagAtPath(ItemStackBuilder.history_keyword + stat.getId(), getNBT(), SupportedNBTTagValues.STRING);

                    if (hisTag != null) {
                        // Aye
                        StatHistory hist = StatHistory.fromNBTString(this, (String) hisTag.getValue());

                        // History valid? Record
                        if (hist != null)
                            this.setStatHistory(stat, hist);
                    }
                }

                // Some unknown error happened
            } catch (IllegalArgumentException exception) {
                MMOItems.plugin.getLogger().log(Level.SEVERE,
                        ChatColor.GRAY + "Could not load stat '"
                                + ChatColor.GOLD + stat.getId() + ChatColor.GRAY + "'item data from '"
                                + ChatColor.RED + getId() + ChatColor.GRAY + "': "
                                + ChatColor.YELLOW + exception.getMessage());
            }
    }
}
