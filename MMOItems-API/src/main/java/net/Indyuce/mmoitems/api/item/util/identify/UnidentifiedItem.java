package net.Indyuce.mmoitems.api.item.util.identify;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.gui.util.IconOptions;
import io.lumine.mythic.lib.util.AdventureUtils;
import io.lumine.mythic.lib.util.lang3.Validate;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.api.ItemTier;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.VolatileMMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnidentifiedItem {
    @Nullable
    private final String displayName;
    private final List<String> lore;
    private final IconOptions icon;

    public UnidentifiedItem(@NotNull ConfigurationSection config) {
        Validate.notNull(config, "Config cannot be null");

        displayName = config.getString("name");
        lore = config.getStringList("lore");

        icon = IconOptions.from(config);
    }

    @NotNull
    public ItemBuilder newBuilder(NBTItem item) {
        return new ItemBuilder(item);
    }

    /**
     * Allows to build an unidentified item based on the given NBTItem.
     */
    public class ItemBuilder {
        private final int amount;
        private final NBTItem item;

        private String name = UnidentifiedItem.this.displayName;
        private final List<String> lore = new ArrayList<>(UnidentifiedItem.this.lore);

        public ItemBuilder(NBTItem item) {
            this.amount = item.getItem().getAmount();
            this.item = item;
        }

        // {tier} only displays when tier
        // {level} only displays when level + tier
        public ItemStack build() {

            // Load item data
            final MMOItem mmoitem = new VolatileMMOItem(item);
            final @Nullable ItemTier tier = ItemTier.ofItem(item);
            final int level = mmoitem.hasData(ItemStats.REQUIRED_LEVEL) ? (int) ((DoubleData) mmoitem.getData(ItemStats.REQUIRED_LEVEL)).getValue() : -1;

            // Load placeholders
            Map<String, String> placeholders = new HashMap<>();
            if (tier != null) {
                placeholders.put("prefix", tier.getUnidentificationInfo().getPrefix());
                placeholders.put("tier", tier.getUnidentificationInfo().getDisplayName());

                if (level > -1) {
                    int[] range = tier.getUnidentificationInfo().calculateRange(level);
                    placeholders.put("range", range[0] + "-" + range[1]);
                }
            } else
                name = name.replace("#prefix#", "");

            // Remove useless lore lines
            lore.removeIf(s -> (s.startsWith("{tier}") && tier == null) || (s.startsWith("{range}") && (tier == null || level < 0)));

            // Apply placeholders
            for (String placeholder : placeholders.keySet())
                name = name.replace("#" + placeholder + "#", placeholders.get(placeholder));
            for (int n = 0; n < lore.size(); n++) {
                String str = lore.get(n);
                for (String placeholder : placeholders.keySet())
                    str = str.replace("#" + placeholder + "#", placeholders.get(placeholder));
                lore.set(n, MythicLib.plugin.parseColors(str.replace("{range}", "").replace("{tier}", "")));
            }

            // Apply changes to item
            item.getItem().setAmount(1);

            // Save serialized item inside NBT
            final var rawItemStack = new ItemStack(UnidentifiedItem.this.icon.getMaterialElse(item.getItem().getType()));
            final ItemStack unidentified = NBTItem.get(rawItemStack)
                    .addTag(new ItemTag("MMOITEMS_UNIDENTIFIED_ITEM", serialize(item.getItem())))
                    .toItem();

            final var meta = unidentified.getItemMeta();
            UnidentifiedItem.this.icon.applyToItemMeta(meta);
            MMOUtils.fixAttributeLore(meta);
            AdventureUtils.setDisplayName(meta, name);
            AdventureUtils.setLore(meta, lore);
            unidentified.setItemMeta(meta);

            unidentified.setAmount(this.amount);
            return unidentified;
        }

        @NotNull
        private String serialize(ItemStack item) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
                dataOutput.writeObject(item);
                dataOutput.close();
                return Base64Coder.encodeLines(outputStream.toByteArray());
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }
}
