package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
@VersionDependant(version = {1, 20, 2})
public class MaxAbsorption extends DoubleStat {
    public MaxAbsorption() {
        super("MAX_ABSORPTION",
                Material.ENCHANTED_GOLDEN_APPLE,
                "Max Absorption",
                "This does not provide permanent absorption but rather increases your maximum amount of absorption hearts you can have at any time.");
    }
}
