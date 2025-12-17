package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.category.StatCategory;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;

/**
 * 百分比减伤属性，按百分比降低最终伤害。
 */
public final class DecreasePercentageStat extends DoubleStat {

    public static final String ID = "DECREASE_PERCENTAGE";

    public DecreasePercentageStat() {
        super(ID, Material.NETHER_WART, "百分比减伤",
                new String[]{"按百分比减少最终伤害"}, new String[]{"!block", "all"});
        setCategory(StatCategory.TEMPLATE_OPTION);
        setAliases(new String[]{ID.toLowerCase()});
    }
}
