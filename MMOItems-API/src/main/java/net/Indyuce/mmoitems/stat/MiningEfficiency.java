package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
@VersionDependant(version = {1, 21})
public class MiningEfficiency extends DoubleStat {
    public MiningEfficiency() {
        super("MINING_EFFICIENCY",
                Material.IRON_PICKAXE,
                "Mining Efficiency",
                "Mining speed factor added to the speed of mining when using a tool that efficiently mines a block. Default and minimum is 0, maximum is 1024."
        );
    }
}
