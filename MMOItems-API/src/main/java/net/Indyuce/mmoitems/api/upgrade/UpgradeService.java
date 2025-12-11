package net.Indyuce.mmoitems.api.upgrade;

import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.version.Sounds;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.UpgradeTemplate;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.VolatileMMOItem;
import net.Indyuce.mmoitems.api.util.message.Message;
import net.Indyuce.mmoitems.stat.data.UpgradeData;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 强化服务核心类
 * <p>
 * 提供可复用的强化逻辑，支持：
 * <ul>
 *     <li>背包强化（UpgradeStat 调用）</li>
 *     <li>命令强化（UpgradeCommandTreeNode 调用）</li>
 *     <li>工作台强化（CraftingStation 调用）</li>
 * </ul>
 * </p>
 * <p>
 * 核心功能：
 * <ul>
 *     <li>强化石查找与消耗</li>
 *     <li>成功率计算（含衰减）</li>
 *     <li>成功/失败处理</li>
 *     <li>惩罚判定与执行</li>
 * </ul>
 * </p>
 *
 * @author MMOItems Team
 * @since 强化命令系统
 */
public class UpgradeService {

    private static final Random RANDOM = new Random();

    /**
     * 执行强化操作
     * <p>
     * 这是强化服务的核心入口方法，根据上下文执行强化：
     * <ol>
     *     <li>验证前置条件（等级上限等）</li>
     *     <li>查找并验证强化石（非免费模式）</li>
     *     <li>计算实际成功率</li>
     *     <li>执行成功/失败判定</li>
     *     <li>应用结果（升级或惩罚）</li>
     * </ol>
     * </p>
     *
     * @param context 强化上下文
     * @return 强化结果
     */
    @NotNull
    public static UpgradeResult performUpgrade(@NotNull UpgradeContext context) {
        Player player = context.getPlayer();
        MMOItem targetMMO = context.getTargetItem();
        UpgradeData targetData = context.getTargetData();

        // 1. 验证强化模板
        UpgradeTemplate template = targetData.getTemplate();
        if (template == null) {
            return UpgradeResult.error("未找到强化模板: " + targetData.getTemplateName());
        }

        // 2. 等级检查（非强制模式）
        if (!context.isForceMode() && !targetData.canLevelUp()) {
            return UpgradeResult.error("已达到最大强化等级");
        }

        // 3. 直达模式等级检查
        if (context.isDirectMode()) {
            int currentLevel = targetData.getLevel();
            int directLevel = context.getDirectLevel();
            if (directLevel <= currentLevel) {
                return UpgradeResult.error("目标等级必须高于当前等级 (当前: " + currentLevel + ")");
            }
            if (!context.isForceMode() && targetData.getMax() > 0 && directLevel > targetData.getMax()) {
                return UpgradeResult.error("目标等级超出上限 (上限: " + targetData.getMax() + ")");
            }
        }

        // 4. 查找强化石（非免费模式）
        List<ItemStack> upgradeStones = new ArrayList<>();
        UpgradeData consumableData = null;
        int requiredStones = context.getRequiredStoneCount();

        if (!context.isFreeMode()) {
            upgradeStones = findUpgradeStones(player, targetData.getReference(), requiredStones);
            if (upgradeStones.size() < requiredStones) {
                return UpgradeResult.error("背包中强化石不足，需要 " + requiredStones + " 个，当前 " + upgradeStones.size() + " 个");
            }
            // 使用第一个强化石的成功率
            NBTItem firstStoneNBT = NBTItem.get(upgradeStones.get(0));
            VolatileMMOItem firstStone = new VolatileMMOItem(firstStoneNBT);
            if (firstStone.hasData(ItemStats.UPGRADE)) {
                consumableData = (UpgradeData) firstStone.getData(ItemStats.UPGRADE);
            }
        }

        // 5. 计算实际成功率
        double actualSuccess = calculateActualSuccess(consumableData, targetData, context.getChanceModifier());

        // 6. 保存原始等级
        int originalLevel = targetData.getLevel();

        // 7. 判定成功或失败
        boolean success = RANDOM.nextDouble() <= actualSuccess;

        if (success) {
            return handleUpgradeSuccess(context, targetMMO, targetData, template, upgradeStones, originalLevel);
        } else {
            return handleUpgradeFailure(context, player, targetMMO, targetData, upgradeStones, originalLevel);
        }
    }

    /**
     * 从玩家背包中查找符合条件的强化石
     * <p>
     * 强化石必须同时满足以下条件：
     * <ol>
     *     <li>物品类型为 CONSUMABLE（消耗品）</li>
     *     <li>拥有 UPGRADE 数据</li>
     *     <li>reference 与目标物品匹配</li>
     * </ol>
     * 通过类型检查，可被强化的装备（SWORD、ARMOR等类型）不会被错误识别为强化石。
     * </p>
     *
     * @param player          玩家
     * @param targetReference 目标物品的强化参考标识
     * @param count           需要的数量
     * @return 找到的强化石列表
     */
    @NotNull
    public static List<ItemStack> findUpgradeStones(@NotNull Player player, @Nullable String targetReference, int count) {
        List<ItemStack> found = new ArrayList<>();

        for (ItemStack item : player.getInventory().getContents()) {
            if (found.size() >= count) break;
            if (item == null || item.getType() == Material.AIR) continue;

            NBTItem nbt = NBTItem.get(item);

            // 核心检查：强化石必须是 CONSUMABLE 类型
            // 这是区分"强化石"与"可强化装备"的关键判断
            // 武器(SWORD)、防具(ARMOR)等类型会被直接过滤
            Type itemType = Type.get(nbt);
            if (itemType == null || !itemType.corresponds(Type.CONSUMABLE)) {
                continue;
            }

            // 检查是否有强化数据
            if (!nbt.hasTag(ItemStats.UPGRADE.getNBTPath())) continue;

            VolatileMMOItem mmoitem = new VolatileMMOItem(nbt);
            if (!mmoitem.hasData(ItemStats.UPGRADE)) continue;

            UpgradeData data = (UpgradeData) mmoitem.getData(ItemStats.UPGRADE);
            // 检查强化石的 reference 是否匹配目标物品
            if (MMOUtils.checkReference(data.getReference(), targetReference)) {
                found.add(item);
            }
        }

        return found;
    }

    /**
     * 计算实际成功率
     * <p>
     * 公式：实际成功率 = 基础成功率 × 衰减系数^当前等级 × chance系数
     * </p>
     *
     * @param consumableData  消耗品强化数据（提供基础成功率）
     * @param targetData      目标物品强化数据（提供衰减配置）
     * @param chanceModifier  成功率系数
     * @return 实际成功率（0-1）
     */
    public static double calculateActualSuccess(@Nullable UpgradeData consumableData,
                                                @NotNull UpgradeData targetData,
                                                double chanceModifier) {
        // 获取基础成功率
        double baseSuccess;
        if (consumableData != null) {
            baseSuccess = consumableData.getSuccess();
        } else {
            // 免费模式或无消耗品数据时，使用 100% 成功率
            baseSuccess = 1.0;
        }

        // 应用衰减
        double actualSuccess = baseSuccess;
        if (targetData.isDecayEnabled() && targetData.getDecayFactor() < 1.0) {
            actualSuccess *= Math.pow(targetData.getDecayFactor(), targetData.getLevel());
        }

        // 应用 chance 系数
        actualSuccess *= chanceModifier;

        return actualSuccess;
    }

    /**
     * 处理强化成功
     */
    @NotNull
    private static UpgradeResult handleUpgradeSuccess(@NotNull UpgradeContext context,
                                                       @NotNull MMOItem targetMMO,
                                                       @NotNull UpgradeData targetData,
                                                       @NotNull UpgradeTemplate template,
                                                       @NotNull List<ItemStack> upgradeStones,
                                                       int originalLevel) {
        // 消耗强化石
        int consumedStones = 0;
        if (!context.isFreeMode()) {
            int requiredStones = context.getRequiredStoneCount();
            consumedStones = consumeStones(upgradeStones, requiredStones);
        }

        // 执行升级
        int newLevel;
        if (context.isDirectMode()) {
            // 直达模式：直接到目标等级
            int targetLevel = context.getDirectLevel();
            // 检查上限（非强制模式）
            if (!context.isForceMode() && targetData.getMax() > 0 && targetLevel > targetData.getMax()) {
                targetLevel = targetData.getMax();
            }
            template.upgradeTo(targetMMO, targetLevel);
            newLevel = targetLevel;
        } else {
            // 普通模式：+1
            template.upgrade(targetMMO);
            newLevel = originalLevel + 1;
        }

        return UpgradeResult.success(targetMMO, originalLevel, newLevel, consumedStones);
    }

    /**
     * 处理强化失败
     */
    @NotNull
    private static UpgradeResult handleUpgradeFailure(@NotNull UpgradeContext context,
                                                       @NotNull Player player,
                                                       @NotNull MMOItem targetMMO,
                                                       @NotNull UpgradeData targetData,
                                                       @NotNull List<ItemStack> upgradeStones,
                                                       int originalLevel) {
        // 消耗强化石
        int consumedStones = 0;
        if (!context.isFreeMode()) {
            int requiredStones = context.getRequiredStoneCount();
            consumedStones = consumeStones(upgradeStones, requiredStones);
        }

        // protect 模式：跳过所有惩罚
        if (context.isProtectMode()) {
            return UpgradeResult.failureProtected(consumedStones);
        }

        // 执行惩罚判定
        PenaltyResult penalty = applyPenalty(player, targetMMO, targetData, context.getTargetItemStack(), originalLevel);

        if (penalty == PenaltyResult.NONE) {
            return UpgradeResult.failureNoPenalty(consumedStones);
        }

        int newLevel = originalLevel;
        if (penalty == PenaltyResult.DOWNGRADE) {
            // 计算新等级
            int downgradeAmount = targetData.getDowngradeAmount();
            newLevel = Math.max(targetData.getMin(), originalLevel - downgradeAmount);
        }

        return UpgradeResult.failureWithPenalty(penalty, originalLevel, newLevel, consumedStones);
    }

    /**
     * 消耗指定数量的强化石
     *
     * @param stones 强化石列表
     * @param count  需要消耗的数量
     * @return 实际消耗的数量
     */
    private static int consumeStones(@NotNull List<ItemStack> stones, int count) {
        int consumed = 0;
        for (ItemStack stone : stones) {
            if (consumed >= count) break;
            stone.setAmount(stone.getAmount() - 1);
            consumed++;
        }
        return consumed;
    }

    /**
     * 应用强化失败的惩罚
     * <p>
     * 惩罚优先级：碎裂 → 掉级 → 销毁
     * </p>
     *
     * @param player          玩家
     * @param targetMMO       目标物品
     * @param targetData      目标强化数据
     * @param targetItemStack 目标 ItemStack（用于修改物品，可为 null）
     * @param originalLevel   强化前的等级
     * @return 惩罚结果
     */
    @NotNull
    public static PenaltyResult applyPenalty(@NotNull Player player,
                                             @NotNull MMOItem targetMMO,
                                             @NotNull UpgradeData targetData,
                                             @Nullable ItemStack targetItemStack,
                                             int originalLevel) {

        String itemName = targetMMO.hasData(ItemStats.NAME)
                ? targetMMO.getData(ItemStats.NAME).toString()
                : "物品";

        // 优先级1：碎裂判定
        if (targetData.isInBreakRange(originalLevel) && targetData.getBreakChance() > 0) {
            if (RANDOM.nextDouble() < targetData.getBreakChance()) {
                // 触发碎裂，检查保护
                if (tryConsumeProtection(player, targetData.getBreakProtectKey())) {
                    Message.UPGRADE_FAIL_PROTECTED.format(ChatColor.GREEN, "#item#", itemName).send(player);
                    player.playSound(player.getLocation(), Sounds.ENTITY_PLAYER_LEVELUP, 1, 1.5f);
                    return PenaltyResult.PROTECTED;
                } else {
                    // 执行碎裂
                    if (targetItemStack != null) {
                        targetItemStack.setAmount(0);
                    }
                    Message.UPGRADE_FAIL_BREAK.format(ChatColor.RED, "#item#", itemName).send(player);
                    player.playSound(player.getLocation(), Sounds.ENTITY_ITEM_BREAK, 1, 0.5f);
                    return PenaltyResult.BREAK;
                }
            }
        }

        // 优先级2：掉级判定
        if (targetData.isInDowngradeRange(originalLevel) && targetData.getDowngradeChance() > 0) {
            if (RANDOM.nextDouble() < targetData.getDowngradeChance()) {
                // 触发掉级，检查保护
                if (tryConsumeProtection(player, targetData.getDowngradeProtectKey())) {
                    Message.UPGRADE_FAIL_PROTECTED.format(ChatColor.GREEN, "#item#", itemName).send(player);
                    player.playSound(player.getLocation(), Sounds.ENTITY_PLAYER_LEVELUP, 1, 1.5f);
                    return PenaltyResult.PROTECTED;
                } else {
                    // 执行掉级
                    int downgradeAmount = targetData.getDowngradeAmount();
                    int newLevel = Math.max(targetData.getMin(), originalLevel - downgradeAmount);
                    int actualDowngrade = originalLevel - newLevel;

                    if (actualDowngrade > 0) {
                        UpgradeTemplate template = targetData.getTemplate();
                        if (template != null) {
                            template.upgradeTo(targetMMO, newLevel);
                            // 如果有 ItemStack，更新它
                            if (targetItemStack != null) {
                                NBTItem result = targetMMO.newBuilder().buildNBT();
                                targetItemStack.setItemMeta(result.toItem().getItemMeta());
                            }
                        }
                        Message.UPGRADE_FAIL_DOWNGRADE.format(ChatColor.RED, "#item#", itemName,
                                "#amount#", String.valueOf(actualDowngrade)).send(player);
                        player.playSound(player.getLocation(), Sounds.ENTITY_ITEM_BREAK, 1, 1.5f);
                        return PenaltyResult.DOWNGRADE;
                    } else {
                        // 已经在最低等级，无法掉级，但掉级判定已触发，不再继续判定其他惩罚
                        Message.UPGRADE_CMD_FAIL_NO_PENALTY.format(ChatColor.RED).send(player);
                        player.playSound(player.getLocation(), Sounds.ENTITY_ITEM_BREAK, 1, 1.5f);
                        return PenaltyResult.NONE;
                    }
                }
            }
        }

        // 优先级3：销毁判定
        if (targetData.destroysOnFail()) {
            if (targetItemStack != null) {
                targetItemStack.setAmount(0);
            }
            Message.UPGRADE_FAIL.format(ChatColor.RED).send(player);
            player.playSound(player.getLocation(), Sounds.ENTITY_ITEM_BREAK, 1, 2);
            return PenaltyResult.DESTROY;
        }

        // 无惩罚
        return PenaltyResult.NONE;
    }

    /**
     * 尝试从玩家背包中消耗指定保护标签的保护物品
     *
     * @param player     玩家
     * @param protectKey 保护标签
     * @return 是否成功找到并消耗保护物品
     */
    public static boolean tryConsumeProtection(@NotNull Player player, @Nullable String protectKey) {
        if (protectKey == null || protectKey.isEmpty()) return false;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;

            NBTItem nbt = NBTItem.get(item);
            if (!nbt.hasTag(ItemStats.UPGRADE_PROTECTION.getNBTPath())) continue;

            String itemProtectKey = nbt.getString(ItemStats.UPGRADE_PROTECTION.getNBTPath());
            if (protectKey.equals(itemProtectKey)) {
                // 消耗一个保护物品
                item.setAmount(item.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    /**
     * 更新玩家手中的物品
     * <p>
     * 用于命令模式下更新主手物品
     * </p>
     *
     * @param player       玩家
     * @param upgradedMMO  升级后的 MMOItem
     */
    public static void updateMainHandItem(@NotNull Player player, @NotNull MMOItem upgradedMMO) {
        NBTItem result = upgradedMMO.newBuilder().buildNBT();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        mainHand.setItemMeta(result.toItem().getItemMeta());
    }
}
