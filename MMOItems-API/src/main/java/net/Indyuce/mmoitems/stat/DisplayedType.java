package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.type.GemStoneStat;
import net.Indyuce.mmoitems.stat.type.StringStat;
import org.bukkit.Material;

public class DisplayedType extends StringStat implements GemStoneStat {
    public DisplayedType() {
        super("DISPLAYED_TYPE", Material.OAK_SIGN, "Displayed Type", new String[]{"This option will only affect the", "type displayed on the item lore."}, new String[0]);
    }
}
