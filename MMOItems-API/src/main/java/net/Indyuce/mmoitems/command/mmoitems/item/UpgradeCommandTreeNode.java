package net.Indyuce.mmoitems.command.mmoitems.item;

import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.command.CommandTreeExplorer;
import io.lumine.mythic.lib.command.CommandTreeNode;
import io.lumine.mythic.lib.command.argument.Argument;
import io.lumine.mythic.lib.version.Sounds;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.upgrade.PenaltyResult;
import net.Indyuce.mmoitems.api.upgrade.UpgradeContext;
import net.Indyuce.mmoitems.api.upgrade.UpgradeMode;
import net.Indyuce.mmoitems.api.upgrade.UpgradeResult;
import net.Indyuce.mmoitems.api.upgrade.UpgradeService;
import net.Indyuce.mmoitems.api.util.message.Message;
import net.Indyuce.mmoitems.stat.data.UpgradeData;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * 强化命令节点
 * <p>
 * 命令格式：/mi item upgrade &lt;common|protect&gt; &lt;chance&gt; [-free] [-force] [-direct:XX]
 * </p>
 * <p>
 * 参数说明：
 * <ul>
 *     <li>common/protect - 强化模式</li>
 *     <li>chance - 成功率系数（浮点数，1.0 = 100%）</li>
 *     <li>-free - 免费模式，不消耗强化石</li>
 *     <li>-force - 强制模式，可突破等级上限</li>
 *     <li>-direct:XX - 直达模式，成功时直达指定等级</li>
 * </ul>
 * </p>
 *
 * @author MMOItems Team
 * @since 强化命令系统
 */
public class UpgradeCommandTreeNode extends CommandTreeNode {

    /**
     * 权限节点定义
     */
    private static final String PERM_BASE = "mmoitems.command.item.upgrade";
    private static final String PERM_PROTECT = PERM_BASE + ".protect";
    private static final String PERM_FREE = PERM_BASE + ".free";
    private static final String PERM_FORCE = PERM_BASE + ".force";
    private static final String PERM_DIRECT = PERM_BASE + ".direct";

    /**
     * 命令参数定义
     */
    private final Argument<UpgradeMode> argMode;
    private final Argument<Double> argChance;
    private final Argument<String> argFlags;

    public UpgradeCommandTreeNode(CommandTreeNode parent) {
        super(parent, "upgrade");

        // 模式参数：common 或 protect
        argMode = addArgument(new Argument<>("mode",
                (explorer, list) -> {
                    list.add("common");
                    list.add("protect");
                },
                (explorer, input) -> UpgradeMode.fromId(input),
                explorer -> UpgradeMode.COMMON
        ));

        // 成功率系数参数
        argChance = addArgument(new Argument<>("chance",
                (explorer, list) -> list.addAll(Arrays.asList("1.0", "0.5", "2.0", "10")),
                (explorer, input) -> {
                    double value = Double.parseDouble(input);
                    return Math.max(0, value);
                },
                explorer -> 1.0
        ));

        // 可选标志参数（可重复）
        argFlags = addArgument(new Argument<>("flags",
                (explorer, list) -> list.addAll(Arrays.asList("-free", "-force", "-direct:")),
                (explorer, input) -> input,
                explorer -> ""
        ));
    }

    @Override
    public @NotNull CommandResult execute(CommandTreeExplorer explorer, CommandSender sender, String[] args) {
        // 仅限玩家使用
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "该命令仅限玩家使用。");
            return CommandResult.FAILURE;
        }

        Player player = (Player) sender;

        // 基础权限检查
        if (!player.hasPermission(PERM_BASE)) {
            Message.NOT_ENOUGH_PERMS_COMMAND.format(ChatColor.RED).send(player);
            return CommandResult.FAILURE;
        }

        // 解析必要参数
        UpgradeMode mode;
        double chanceModifier;
        try {
            mode = explorer.parse(argMode);
            chanceModifier = explorer.parse(argChance);
        } catch (Exception e) {
            sendUsage(player);
            return CommandResult.FAILURE;
        }

        if (mode == null) {
            sendUsage(player);
            return CommandResult.FAILURE;
        }

        // 解析可选标志（从原始 args 数组中解析，因为可能有多个标志）
        boolean freeMode = false;
        boolean forceMode = false;
        int directLevel = 0;

        for (String arg : args) {
            String lowerArg = arg.toLowerCase();
            if ("-free".equals(lowerArg)) {
                freeMode = true;
            } else if ("-force".equals(lowerArg)) {
                forceMode = true;
            } else if (lowerArg.startsWith("-direct:")) {
                try {
                    directLevel = Integer.parseInt(lowerArg.substring(8));
                    if (directLevel <= 0) {
                        player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "直达等级必须为正数。");
                        return CommandResult.FAILURE;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "无效的直达等级格式。");
                    return CommandResult.FAILURE;
                }
            }
        }

        // 权限检查
        if (mode == UpgradeMode.PROTECT && !player.hasPermission(PERM_PROTECT)) {
            player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "你没有权限使用 protect 模式。");
            return CommandResult.FAILURE;
        }
        if (freeMode && !player.hasPermission(PERM_FREE)) {
            player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "你没有权限使用 -free 标志。");
            return CommandResult.FAILURE;
        }
        if (forceMode && !player.hasPermission(PERM_FORCE)) {
            player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "你没有权限使用 -force 标志。");
            return CommandResult.FAILURE;
        }
        if (directLevel > 0 && !player.hasPermission(PERM_DIRECT)) {
            player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "你没有权限使用 -direct 标志。");
            return CommandResult.FAILURE;
        }

        // 获取主手物品
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        if (mainHandItem == null || mainHandItem.getType() == Material.AIR) {
            player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "请手持需要强化的物品。");
            return CommandResult.FAILURE;
        }

        // 检查物品是否可强化
        NBTItem targetNBT = NBTItem.get(mainHandItem);
        if (!targetNBT.hasTag(ItemStats.UPGRADE.getNBTPath())) {
            player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "该物品无法强化。");
            return CommandResult.FAILURE;
        }

        // 检查堆叠物品
        if (mainHandItem.getAmount() > 1) {
            Message.CANT_UPGRADED_STACK.format(ChatColor.RED).send(player);
            return CommandResult.FAILURE;
        }

        // 构建强化上下文
        MMOItem targetMMO = new LiveMMOItem(targetNBT);
        UpgradeData targetData = (UpgradeData) targetMMO.getData(ItemStats.UPGRADE);

        if (targetData == null) {
            player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "无法读取物品强化数据。");
            return CommandResult.FAILURE;
        }

        UpgradeContext context = new UpgradeContext.Builder()
                .player(player)
                .targetItem(targetMMO)
                .targetData(targetData)
                .targetItemStack(mainHandItem)
                .mode(mode)
                .chanceModifier(chanceModifier)
                .freeMode(freeMode)
                .forceMode(forceMode)
                .directLevel(directLevel)
                .build();

        // 执行强化
        UpgradeResult result = UpgradeService.performUpgrade(context);

        // 处理结果
        handleResult(player, result, mainHandItem, targetMMO);

        return CommandResult.SUCCESS;
    }

    /**
     * 处理强化结果
     *
     * @param player       玩家
     * @param result       强化结果
     * @param mainHandItem 主手物品
     * @param targetMMO    目标 MMOItem
     */
    private void handleResult(Player player, UpgradeResult result, ItemStack mainHandItem, MMOItem targetMMO) {
        String itemName = MMOUtils.getDisplayName(mainHandItem);

        switch (result.getStatus()) {
            case SUCCESS:
                // 更新物品
                UpgradeService.updateMainHandItem(player, result.getUpgradedItem());
                // 发送消息
                Message.UPGRADE_CMD_SUCCESS.format(ChatColor.GREEN,
                        "#item#", itemName,
                        "#level#", String.valueOf(result.getNewLevel())).send(player);
                player.playSound(player.getLocation(), Sounds.ENTITY_PLAYER_LEVELUP, 1, 2);
                // 显示消耗信息
                if (result.getConsumedStones() > 0) {
                    player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.GRAY + "消耗了 " +
                            ChatColor.GOLD + result.getConsumedStones() + ChatColor.GRAY + " 个强化石。");
                }
                break;

            case FAILURE_PROTECTED:
                String protectedMsg = result.getMessage();
                if (protectedMsg == null || protectedMsg.isEmpty()) {
                    Message.UPGRADE_CMD_FAIL_PROTECTED.format(ChatColor.YELLOW).send(player);
                } else {
                    player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.YELLOW + protectedMsg);
                }
                player.playSound(player.getLocation(), Sounds.ENTITY_PLAYER_LEVELUP, 1, 1.5f);
                if (result.getConsumedStones() > 0) {
                    player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.GRAY + "消耗了 " +
                            ChatColor.GOLD + result.getConsumedStones() + ChatColor.GRAY + " 个强化石。");
                }
                break;

            case FAILURE_WITH_PENALTY:
                // 惩罚已经在 UpgradeService.applyPenalty 中处理
                // 这里只需要显示消耗信息
                if (result.getConsumedStones() > 0) {
                    player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.GRAY + "消耗了 " +
                            ChatColor.GOLD + result.getConsumedStones() + ChatColor.GRAY + " 个强化石。");
                }
                break;

            case FAILURE_NO_PENALTY:
                Message.UPGRADE_CMD_FAIL_NO_PENALTY.format(ChatColor.RED).send(player);
                player.playSound(player.getLocation(), Sounds.ENTITY_ITEM_BREAK, 1, 2);
                if (result.getConsumedStones() > 0) {
                    player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.GRAY + "消耗了 " +
                            ChatColor.GOLD + result.getConsumedStones() + ChatColor.GRAY + " 个强化石。");
                }
                break;

            case ERROR:
                player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + result.getMessage());
                break;
        }
    }

    /**
     * 发送命令用法提示
     *
     * @param player 玩家
     */
    private void sendUsage(Player player) {
        player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "用法: /mi item upgrade <common|protect> <chance> [-free] [-force] [-direct:XX]");
        player.sendMessage(ChatColor.GRAY + "  • common - 普通模式，失败时触发惩罚");
        player.sendMessage(ChatColor.GRAY + "  • protect - 防护模式，失败时跳过惩罚");
        player.sendMessage(ChatColor.GRAY + "  • chance - 成功率系数 (例如: 1.0 = 100%, 0.5 = 50%, 10 = 1000%)");
        player.sendMessage(ChatColor.GRAY + "  • -free - 不消耗强化石");
        player.sendMessage(ChatColor.GRAY + "  • -force - 可突破等级上限");
        player.sendMessage(ChatColor.GRAY + "  • -direct:XX - 成功时直达指定等级");
    }
}
