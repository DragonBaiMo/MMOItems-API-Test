package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
@VersionDependant(version = {1, 21})
public class SweepingDamageRatio extends DoubleStat {
    public SweepingDamageRatio() {
        super("SWEEPING_DAMAGE_RATIO",
                Material.LIGHT_GRAY_DYE,
                "Sweeping Damage Ratio",
                "How much of the base attack damage that gets transferred transfer to secondary targets in a sweep attack. This is additive to the base attack of the sweep damage itself of 1. A value of 0 means none of the base attack damage is transferred (sweep damage is 1). A value of 1 means all of the base attack damage is transferred. Default and minimum value is 0, maximum value is 1.");
    }

    @Override
    public double multiplyWhenDisplaying() {
        return 100;
    }
}
