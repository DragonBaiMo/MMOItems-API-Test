package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.util.MMOUtils;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
@VersionDependant(version = {1, 21})
public class BurningTime extends DoubleStat {
    public BurningTime() {
        super("BURNING_TIME",
                Material.FIRE_CHARGE,
                "Burning Time",
                "A factor to how long the player remains on fire after being ignited. A factor of 0 removes the entire burn time, a factor of 1 lets the Entity burn the default fire time - larger values increase the amount of time the entity remains on fire. Default is 1, minimum is 0, maximum is 1024.");
    }

    @Override
    public double multiplyWhenDisplaying() {
        return 100;
    }
}
