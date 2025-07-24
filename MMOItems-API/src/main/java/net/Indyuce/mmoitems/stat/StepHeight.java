package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
@VersionDependant(version = {1, 20, 5})
public class StepHeight extends DoubleStat {
    public StepHeight() {
        super("STEP_HEIGHT",
                Material.STONE_SLAB,
                "Step Height",
                "Determines the max height in blocks where a mob can walk above without jumping. Default is 0.6, and the valid range is from 0 to 10.");
    }
}
