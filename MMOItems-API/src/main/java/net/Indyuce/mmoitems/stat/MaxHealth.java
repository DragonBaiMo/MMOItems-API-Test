package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
public class MaxHealth extends DoubleStat {
    public MaxHealth() {
        super("MAX_HEALTH",
                Material.GOLDEN_APPLE,
                "Max Health",
                "The amount of health your item gives to the holder.");
    }
}
