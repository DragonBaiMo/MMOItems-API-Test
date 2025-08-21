package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.type.BooleanStat;
import org.bukkit.Material;
import io.lumine.mythic.lib.api.item.ItemTag;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.stat.data.BooleanData;
import org.jetbrains.annotations.NotNull;

@HasCategory(cat = "soulbound")
public class AutoBindOnUse extends BooleanStat {
    public AutoBindOnUse() {
        super(
                "AUTO_BIND_ON_USE",
                Material.ENDER_EYE,
                "Auto Bind On Use",
                new String[]{
                        "Automatically binds this item to",
                        "the user when they use it for the first time."
                },
                new String[]{"!block", "all"}
        );
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull BooleanData data) {
        // 始终写入 NBT：true/false 都持久化，避免为 false 时键被省略
        item.addItemTag(new ItemTag(getNBTPath(), data.isEnabled()));

        // lore 仍保持默认行为：仅在 true 时展示
        if (data.isEnabled()) {
            item.getLore().insert(getPath(), getGeneralStatFormat());
        }
    }
}
