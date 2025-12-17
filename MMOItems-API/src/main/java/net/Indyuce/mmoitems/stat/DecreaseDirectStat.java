package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.category.StatCategory;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;

/**
 * 固定值减伤属性，直接减少攻击或受到的伤害点数。
 */
public final class DecreaseDirectStat extends DoubleStat {

    public static final String ID = "DECREASE_DIRECT";

    public DecreaseDirectStat() {
        super(ID, Material.REDSTONE, "直接减伤",
                new String[]{"固定减少最终伤害数值"}, new String[]{"!block", "all"});
        setCategory(StatCategory.TEMPLATE_OPTION);
        setAliases(new String[]{ID.toLowerCase()});
    }
}
