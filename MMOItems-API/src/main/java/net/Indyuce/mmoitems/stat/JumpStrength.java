package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
@VersionDependant(version = {1, 20, 5})
public class JumpStrength extends DoubleStat {
    public JumpStrength() {
        super("JUMP_STRENGTH",
                Material.SADDLE,
                "Jump Strength",
                "This controls the base impulse from a jump (before jump boost or modifier on block). Default is 0.42, and the valid range is from 0 to 32.");
    }
}
