package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
@VersionDependant(version = {1, 21})
public class SneakingSpeed extends DoubleStat {
    public SneakingSpeed() {
        super("SNEAKING_SPEED",
                Material.LEATHER_BOOTS,
                "Sneaking Speed",
                "The movement speed factor when sneaking. A factor of 1 means sneaking is as fast as walking, a factor of 0 means unable to move while sneaking. Default is 0.3, minimum is 0 and maximum is 1.");
    }

    @Override
    public double multiplyWhenDisplaying() {
        return 100;
    }
}
