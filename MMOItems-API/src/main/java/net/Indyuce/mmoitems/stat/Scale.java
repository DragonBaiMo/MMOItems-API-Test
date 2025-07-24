package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
@VersionDependant(version = {1, 20, 5})
public class Scale extends DoubleStat {
    public Scale() {
        super("SCALE",
                Material.STONE,
                "Scale",
                "Allows changing the size of the player to anywhere between 0.0625 and 16 times their default size.");
    }

    @Override
    public double multiplyWhenDisplaying() {
        return 100;
    }
}
