package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
@VersionDependant(version = {1, 20, 5})
public class BlockBreakSpeed extends DoubleStat {
    public BlockBreakSpeed() {
        super("BLOCK_BREAK_SPEED",
                Material.IRON_PICKAXE,
                "Mining Speed",
                "Additional block breaking speed. Bare hands have a mining speed of 1");
    }
}
