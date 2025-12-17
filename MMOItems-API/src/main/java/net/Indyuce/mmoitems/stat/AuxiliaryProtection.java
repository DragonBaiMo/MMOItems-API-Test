package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;

/**
 * 保护石属性 - 降低强化失败惩罚概率
 * <p>
 * 作为消耗品使用时，降低强化失败时触发惩罚（掉级/碎裂）的概率。
 * 效果值为惩罚概率的百分比降低，例如值为 50 表示惩罚概率降低 50%。
 * </p>
 * <p>
 * 计算方式：实际惩罚概率 = 原始惩罚概率 × (1 - 保护值/100)
 * </p>
 * <p>
 * 配置示例：
 * <pre>
 * PROTECT_STONE:
 *   base:
 *     material: LAPIS_LAZULI
 *     name: '&b保护石'
 *     auxiliary-protection: 50  # 惩罚概率降低 50%
 * </pre>
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public class AuxiliaryProtection extends DoubleStat {

    public AuxiliaryProtection() {
        super("AUXILIARY_PROTECTION",
                Material.LAPIS_LAZULI,
                "辅料惩罚保护",
                new String[]{
                        "强化时使用，降低失败惩罚概率。",
                        "效果值为惩罚概率的百分比降低。",
                        "例如：50 表示惩罚概率降低 50%。"
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
