package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.type.BooleanStat;
import org.bukkit.Material;

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
}
