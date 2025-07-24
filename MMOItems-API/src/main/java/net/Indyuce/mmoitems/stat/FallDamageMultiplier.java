package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;

@VersionDependant(version = {1, 20, 5})
public class FallDamageMultiplier extends DoubleStat {
    public FallDamageMultiplier() {
        super("FALL_DAMAGE_MULTIPLIER",
                Material.DAMAGED_ANVIL,
                "Fall Damage Multiplier",
                "Multiply overall fall damage amount. Default is 1, and the valid range is from 0 to 100."
        );
    }

    @Override
    public double multiplyWhenDisplaying() {
        return 100;
    }
}
