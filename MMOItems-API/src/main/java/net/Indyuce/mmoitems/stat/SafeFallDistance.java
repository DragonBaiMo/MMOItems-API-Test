package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
@VersionDependant(version = {1, 20, 5})
public class SafeFallDistance extends DoubleStat {
    public SafeFallDistance() {
        super("SAFE_FALL_DISTANCE",
                Material.RED_BED,
                "Safe Fall Distance",
                "Controls the fall distance after which the player takes fall damage. Default is 3, and the valid range is from -1024 to +1024.");
    }
}
