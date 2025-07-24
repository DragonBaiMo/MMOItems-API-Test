package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.util.MMOUtils;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
@VersionDependant(version = {1, 21})
public class ExplosionKnockbackResistance extends DoubleStat {
    public ExplosionKnockbackResistance() {
        super("EXPLOSION_KNOCKBACK_RESISTANCE",
                Material.OBSIDIAN,
                "Explosion Knockback Resistance",
                "A factor to how much knockback an Entity takes from an Explosion. A factor of 1 removes the entire knockback, a factor of 0 means no knockback reduction.");
    }

    @Override
    public double multiplyWhenDisplaying() {
        return 100;
    }
}
