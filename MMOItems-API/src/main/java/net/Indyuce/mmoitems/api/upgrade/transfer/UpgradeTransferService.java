package net.Indyuce.mmoitems.api.upgrade.transfer;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.UpgradeTemplate;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.stat.data.UpgradeData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 强化等级转移服务
 * <p>
 * 提供物品之间强化等级转移的核心逻辑：
 * <ul>
 *     <li>源物品与目标物品的类型校验</li>
 *     <li>转移比例计算（默认 80%）</li>
 *     <li>转移石消耗</li>
 *     <li>等级转移执行</li>
 * </ul>
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public class UpgradeTransferService {

    /**
     * 默认转移比例（80%）
     */
    public static final double DEFAULT_TRANSFER_RATIO = 0.8;

    /**
     * 执行强化等级转移
     * <p>
     * 规则：
     * <ol>
     *     <li>源物品与目标物品必须同类型或配置允许的兼容类型</li>
     *     <li>源物品强化等级 > 0</li>
     *     <li>目标等级 = 源等级 × 转移比例（向下取整）</li>
     *     <li>源物品等级重置为 0</li>
     * </ol>
     * </p>
     *
     * @param player       执行转移的玩家
     * @param sourceItem   源物品
     * @param targetItem   目标物品
     * @param freeMode     是否免费模式（不消耗转移石）
     * @param transferRatio 转移比例（0-1），传 0 使用默认值
     * @return 转移结果
     */
    @NotNull
    public static TransferResult performTransfer(@NotNull Player player,
                                                  @NotNull ItemStack sourceItem,
                                                  @NotNull ItemStack targetItem,
                                                  boolean freeMode,
                                                  double transferRatio) {
        // 使用默认比例
        if (transferRatio <= 0) {
            transferRatio = DEFAULT_TRANSFER_RATIO;
        }

        // 1. 验证源物品
        if (sourceItem.getType() == Material.AIR) {
            return TransferResult.error("源物品为空");
        }
        NBTItem sourceNBT = NBTItem.get(sourceItem);
        if (!sourceNBT.hasType()) {
            return TransferResult.error("源物品不是 MMOItems 物品");
        }
        if (!sourceNBT.hasTag(ItemStats.UPGRADE.getNBTPath())) {
            return TransferResult.error("源物品没有强化属性");
        }

        // 2. 验证目标物品
        if (targetItem.getType() == Material.AIR) {
            return TransferResult.error("目标物品为空");
        }
        NBTItem targetNBT = NBTItem.get(targetItem);
        if (!targetNBT.hasType()) {
            return TransferResult.error("目标物品不是 MMOItems 物品");
        }
        if (!targetNBT.hasTag(ItemStats.UPGRADE.getNBTPath())) {
            return TransferResult.error("目标物品没有强化属性");
        }

        // 3. 类型兼容性检查
        Type sourceType = Type.get(sourceNBT);
        Type targetType = Type.get(targetNBT);
        if (sourceType == null || targetType == null) {
            return TransferResult.error("无法识别物品类型");
        }

        // 默认要求同类型，后续可以配置化允许兼容类型
        if (!isTypeCompatible(sourceType, targetType)) {
            return TransferResult.error("源物品与目标物品类型不兼容 (" + sourceType.getName() + " → " + targetType.getName() + ")");
        }

        // 4. 读取源物品强化数据
        MMOItem sourceMMO = new LiveMMOItem(sourceNBT);
        if (!sourceMMO.hasData(ItemStats.UPGRADE)) {
            return TransferResult.error("无法读取源物品强化数据");
        }
        UpgradeData sourceData = (UpgradeData) sourceMMO.getData(ItemStats.UPGRADE);
        int sourceLevel = sourceData.getLevel();

        if (sourceLevel <= 0) {
            return TransferResult.error("源物品没有可转移的强化等级 (当前等级: +" + sourceLevel + ")");
        }

        // 5. 读取目标物品强化数据
        MMOItem targetMMO = new LiveMMOItem(targetNBT);
        if (!targetMMO.hasData(ItemStats.UPGRADE)) {
            return TransferResult.error("无法读取目标物品强化数据");
        }
        UpgradeData targetData = (UpgradeData) targetMMO.getData(ItemStats.UPGRADE);
        int targetOriginalLevel = targetData.getLevel();

        // 6. 查找并消耗转移石（非免费模式）
        if (!freeMode) {
            ItemStack transferStone = findTransferStone(player);
            if (transferStone == null) {
                return TransferResult.error("背包中没有转移石");
            }
            transferStone.setAmount(transferStone.getAmount() - 1);
        }

        // 7. 计算转移后的等级
        int transferredLevel = (int) Math.floor(sourceLevel * transferRatio);
        // 叠加到目标物品现有等级
        int newTargetLevel = targetOriginalLevel + transferredLevel;

        // 检查目标物品等级上限
        if (targetData.getMax() > 0 && newTargetLevel > targetData.getMax()) {
            newTargetLevel = targetData.getMax();
        }

        // 8. 获取强化模板
        UpgradeTemplate sourceTemplate = sourceData.getTemplate();
        UpgradeTemplate targetTemplate = targetData.getTemplate();
        if (sourceTemplate == null) {
            return TransferResult.error("源物品强化模板不存在: " + sourceData.getTemplateName());
        }
        if (targetTemplate == null) {
            return TransferResult.error("目标物品强化模板不存在: " + targetData.getTemplateName());
        }

        // 9. 执行转移：源物品重置为 0，目标物品设置为新等级
        // 重置源物品
        sourceTemplate.upgradeTo(sourceMMO, 0);

        // 设置目标物品等级
        if (newTargetLevel > targetOriginalLevel) {
            targetTemplate.upgradeTo(targetMMO, newTargetLevel);
        }

        // 10. 更新 ItemStack
        NBTItem sourceResult = sourceMMO.newBuilder().buildNBT();
        sourceItem.setItemMeta(sourceResult.toItem().getItemMeta());

        NBTItem targetResult = targetMMO.newBuilder().buildNBT();
        targetItem.setItemMeta(targetResult.toItem().getItemMeta());

        return TransferResult.success(sourceLevel, targetOriginalLevel, newTargetLevel, sourceMMO, targetMMO);
    }

    /**
     * 检查两个物品类型是否兼容（可以转移）
     * <p>
     * 当前规则：
     * <ul>
     *     <li>同类型物品兼容</li>
     *     <li>同父类型物品兼容（如 SWORD 和 LONG_SWORD 都属于武器类）</li>
     * </ul>
     * </p>
     *
     * @param source 源类型
     * @param target 目标类型
     * @return 是否兼容
     */
    public static boolean isTypeCompatible(@NotNull Type source, @NotNull Type target) {
        // 同类型
        if (source.equals(target)) {
            return true;
        }

        // 同父类型
        Type sourceParent = source.getParent();
        Type targetParent = target.getParent();

        if (sourceParent != null && targetParent != null) {
            if (sourceParent.equals(targetParent)) {
                return true;
            }
        }

        // 源类型是目标的父类型，或反之
        if (sourceParent != null && sourceParent.equals(target)) {
            return true;
        }
        if (targetParent != null && targetParent.equals(source)) {
            return true;
        }

        return false;
    }

    /**
     * 从玩家背包中查找转移石
     * <p>
     * 转移石是带有 TRANSFER_STONE NBT 标签的消耗品
     * </p>
     *
     * @param player 玩家
     * @return 找到的转移石，未找到返回 null
     */
    @Nullable
    public static ItemStack findTransferStone(@NotNull Player player) {
        String nbtPath = ItemStats.TRANSFER_STONE.getNBTPath();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;

            NBTItem nbt = NBTItem.get(item);
            // 检查是否是消耗品类型
            Type type = Type.get(nbt);
            if (type == null || !type.corresponds(Type.CONSUMABLE)) continue;

            // 检查是否有转移石标签
            if (nbt.hasTag(nbtPath) && nbt.getBoolean(nbtPath)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 获取玩家背包中所有转移石
     *
     * @param player 玩家
     * @return 转移石列表
     */
    @NotNull
    public static List<ItemStack> findAllTransferStones(@NotNull Player player) {
        String nbtPath = ItemStats.TRANSFER_STONE.getNBTPath();
        List<ItemStack> stones = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;

            NBTItem nbt = NBTItem.get(item);
            Type type = Type.get(nbt);
            if (type == null || !type.corresponds(Type.CONSUMABLE)) continue;

            if (nbt.hasTag(nbtPath) && nbt.getBoolean(nbtPath)) {
                stones.add(item);
            }
        }
        return stones;
    }
}
