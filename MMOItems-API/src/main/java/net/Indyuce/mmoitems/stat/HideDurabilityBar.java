package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.type.BooleanStat;
import org.bukkit.Material;

public class HideDurabilityBar extends BooleanStat {
    public HideDurabilityBar() {
        super("DURABILITY_BAR", Material.DAMAGED_ANVIL, "Hide Durability Bar",
                new String[] { "Enable this to have the green bar", "hidden when using &ncustom&7 durability.", "Does not work when using vanilla durability." }, new String[] { "!block", "all"});
    }
}
