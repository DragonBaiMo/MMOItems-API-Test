package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.type.GemStoneStat;
import net.Indyuce.mmoitems.stat.type.StringStat;
import net.Indyuce.mmoitems.stat.type.TemplateOption;
import org.bukkit.Material;

public class CraftingPermission extends StringStat implements TemplateOption, GemStoneStat {
    public CraftingPermission() {
        super("CRAFT_PERMISSION", Material.OAK_SIGN, "Crafting Recipe Permission",
                new String[]{"The permission needed to craft this item.", "Changing this value requires &o/mi reload recipes&7."},
                new String[0]);
    }
}