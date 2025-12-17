package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;

/**
 * 直达石触发概率属性
 * <p>
 * 作为消耗品使用时，强化成功后有概率触发额外升级。
 * 效果值为触发概率百分比，例如值为 30 表示 30% 概率触发。
 * </p>
 * <p>
 * 需要配合 {@link AuxiliaryDirectUpLevels} 一起使用来指定跳级数量。
 * </p>
 * <p>
 * 配置示例：
 * <pre>
 * DIRECT_STONE:
 *   base:
 *     material: DIAMOND
 *     name: '&d直达石'
 *     auxiliary-direct-up-chance: 30   # 30% 概率触发
 *     auxiliary-direct-up-levels: 1    # 额外升 1 级
 * </pre>
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 * @see AuxiliaryDirectUpLevels
 */
public class AuxiliaryDirectUpChance extends DoubleStat {

    public AuxiliaryDirectUpChance() {
        super("AUXILIARY_DIRECT_UP_CHANCE",
                Material.DIAMOND,
                "直达石触发概率",
                new String[]{
                        "强化成功时有概率额外升级。",
                        "效果值为触发概率百分比。",
                        "例如：30 表示 30% 概率触发。"
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
