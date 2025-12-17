package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.type.BooleanStat;
import org.bukkit.Material;

/**
 * 转移石属性
 * <p>
 * 消耗品配置此属性为 true 后，该物品可作为强化等级转移的消耗材料。
 * </p>
 * <p>
 * 配置示例（在 consumable.yml 中）：
 * <pre>
 * TRANSFER_STONE:
 *   base:
 *     material: ENDER_EYE
 *     name: '&d转移石'
 *     lore:
 *       - '&7用于转移强化等级'
 *     transfer-stone: true
 * </pre>
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public class TransferStone extends BooleanStat {

    public TransferStone() {
        super("TRANSFER_STONE",
                Material.ENDER_EYE,
                "转移石",
                new String[]{"启用后，该消耗品可作为", "强化等级转移的材料使用。"},
                new String[]{"consumable"});
    }
}
