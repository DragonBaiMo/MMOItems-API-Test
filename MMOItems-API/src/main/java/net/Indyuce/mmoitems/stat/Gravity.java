package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
@VersionDependant(version = {1, 20, 5})
public class Gravity extends DoubleStat {
    public Gravity() {
        super("GRAVITY",
                Material.STONE,
                "Gravity",
                "Controls blocks/tick^2 acceleration downward. Default is 0.08, and the valid range is from -1 to +1.");
    }

    @Override
    public double multiplyWhenDisplaying() {
        return 100;
    }
}
