package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;

/**
 * 幸运石属性 - 强化成功率加成
 * <p>
 * 作为消耗品使用时，增加强化的成功率。
 * 效果值为百分比加成，例如值为 10 表示成功率 +10%。
 * </p>
 * <p>
 * 配置示例：
 * <pre>
 * LUCKY_STONE:
 *   base:
 *     material: EMERALD
 *     name: '&a幸运石'
 *     auxiliary-chance-bonus: 10  # 成功率 +10%
 * </pre>
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public class AuxiliaryChanceBonus extends DoubleStat {

    public AuxiliaryChanceBonus() {
        super("AUXILIARY_CHANCE_BONUS",
                Material.EMERALD,
                "辅料成功率加成",
                new String[]{
                        "强化时使用，提升成功率。",
                        "效果值为百分比加成（如 10 表示 +10%）。"
                },
                new String[]{"consumable"});
    }

    /**
     * 不支持负值（负值没有意义）
     *
     * @return false
     */
    @Override
    public boolean handleNegativeStats() {
        return false;
    }
}
