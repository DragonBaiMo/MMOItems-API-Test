package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
@VersionDependant(version = {1, 21})
public class OxygenBonus extends DoubleStat {
    public OxygenBonus() {
        super("OXYGEN_BONUS",
                Material.CONDUIT,
                "Bonus Oxygen",
                "Factor to the chance an Entity has to not use up air when underwater. 0 has no effect, values over 0 are used in the following formula to determine the chance of using up air: 1 / ( bonus_oxygen + 1 ). Maximum is 1024.");
    }
}
