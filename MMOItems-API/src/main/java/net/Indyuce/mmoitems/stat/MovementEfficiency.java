package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
@VersionDependant(version = {1, 21})
public class MovementEfficiency extends DoubleStat {
    public MovementEfficiency() {
        super("MOVEMENT_EFFICIENCY",
                Material.LEATHER_BOOTS,
                "Movement Efficiency",
                "How efficiently the entity can move through impeding terrain that slows down movement. A factor of 1 removes all movement penalty, a factor of 0 applies full movement penalty. Default and minimum is 0, maximum is 1."
        );
    }

    @Override
    public double multiplyWhenDisplaying() {
        return 100;
    }
}
