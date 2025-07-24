package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
@VersionDependant(version = {1, 21})
public class WaterMovementEfficiency extends DoubleStat {
    public WaterMovementEfficiency() {
        super("WATER_MOVEMENT_EFFICIENCY",
                Material.WATER_BUCKET,
                "Water Movement Efficiency",
                "The movement speed factor when submerged. The higher, the more of the underwater movement penalty is mitigated. Note that this represents only the submersion factor itself, and other factors (such as not touching the ground) also apply. Default and minimum value is 0, maximum value is 1.");
    }

    @Override
    public double multiplyWhenDisplaying() {
        return 100;
    }
}
