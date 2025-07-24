package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
@VersionDependant(version = {1, 21})
public class SubmergedMiningSpeed extends DoubleStat {
    public SubmergedMiningSpeed() {
        super("SUBMERGED_MINING_SPEED",
                Material.WATER_BUCKET,
                "Submerged Mining Speed",
                "The mining speed factor when submerged. A factor of 1 means mining as fast submerged as on land, a factor of 0 means unable to mine while submerged. Note that this represents only the submersion factor itself, and other factors(such as not touching the ground) also apply. Default is 0.2, minimum is 0 and maximum is 20.");
    }

    @Override
    public double multiplyWhenDisplaying() {
        return 100;
    }
}
