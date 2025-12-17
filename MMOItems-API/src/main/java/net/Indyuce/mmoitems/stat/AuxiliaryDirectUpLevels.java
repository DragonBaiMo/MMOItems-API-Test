package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;

/**
 * 直达石跳级数量属性
 * <p>
 * 指定直达石触发时额外升级的等级数量。
 * 效果值为跳级数量，例如值为 1 表示额外升 1 级。
 * </p>
 * <p>
 * 需要配合 {@link AuxiliaryDirectUpChance} 一起使用来指定触发概率。
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
 * @see AuxiliaryDirectUpChance
 */
public class AuxiliaryDirectUpLevels extends DoubleStat {

    public AuxiliaryDirectUpLevels() {
        super("AUXILIARY_DIRECT_UP_LEVELS",
                Material.DIAMOND,
                "直达石跳级数量",
                new String[]{
                        "直达石触发时额外升级的等级数量。",
                        "例如：1 表示额外升 1 级。",
                        "默认值：1"
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
