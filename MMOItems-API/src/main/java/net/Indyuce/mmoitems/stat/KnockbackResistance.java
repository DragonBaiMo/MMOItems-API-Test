package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
public class KnockbackResistance extends DoubleStat {
    public KnockbackResistance() {
        super("KNOCKBACK_RESISTANCE",
                Material.CHAINMAIL_CHESTPLATE,
                "Knockback Resistance",
                "The chance of your item to block the knockback from explosions, creepers... 1.0 corresponds to 100%, 0.7 to 70%...");
    }

    @Override
    public double multiplyWhenDisplaying() {
        return 100;
    }
}
