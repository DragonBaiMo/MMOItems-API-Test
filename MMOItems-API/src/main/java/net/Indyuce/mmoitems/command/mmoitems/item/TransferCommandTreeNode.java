package net.Indyuce.mmoitems.command.mmoitems.item;

import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.command.CommandTreeExplorer;
import io.lumine.mythic.lib.command.CommandTreeNode;
import io.lumine.mythic.lib.command.argument.Argument;
import io.lumine.mythic.lib.version.Sounds;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.upgrade.transfer.TransferResult;
import net.Indyuce.mmoitems.api.upgrade.transfer.UpgradeTransferService;
import net.Indyuce.mmoitems.api.util.message.Message;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * 强化等级转移命令节点
 * <p>
 * 命令格式：/mi item transfer [-free] [-ratio:X]
 * </p>
 * <p>
 * 用法：
 * <ul>
 *     <li>主手持有源物品（要提取等级的物品）</li>
 *     <li>副手持有目标物品（要接收等级的物品）</li>
 *     <li>-free：免费模式，不消耗转移石</li>
 *     <li>-ratio:X：自定义转移比例（默认0.8即80%）</li>
 * </ul>
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public class TransferCommandTreeNode extends CommandTreeNode {

    /**
     * 权限节点
     */
    private static final String PERM_BASE = "mmoitems.command.item.transfer";
    private static final String PERM_FREE = PERM_BASE + ".free";
    private static final String PERM_RATIO = PERM_BASE + ".ratio";

    public TransferCommandTreeNode(CommandTreeNode parent) {
        super(parent, "transfer");

        // 可选标志参数
        addArgument(new Argument<>("flags",
                (explorer, list) -> list.addAll(Arrays.asList("-free", "-ratio:")),
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

        // 权限检查
        if (!player.hasPermission(PERM_BASE)) {
            Message.NOT_ENOUGH_PERMS_COMMAND.format(ChatColor.RED).send(player);
            return CommandResult.FAILURE;
        }

        // 解析标志
        boolean freeMode = false;
        double ratio = 0; // 0 表示使用默认值

        for (String arg : args) {
            String lowerArg = arg.toLowerCase();
            if ("-free".equals(lowerArg)) {
                freeMode = true;
            } else if (lowerArg.startsWith("-ratio:")) {
                try {
                    ratio = Double.parseDouble(lowerArg.substring(7));
                    if (ratio < 0 || ratio > 1) {
                        player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "转移比例必须在 0-1 之间。");
                        return CommandResult.FAILURE;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "无效的转移比例格式。");
                    return CommandResult.FAILURE;
                }
            }
        }

        // 权限检查
        if (freeMode && !player.hasPermission(PERM_FREE)) {
            player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "你没有权限使用 -free 标志。");
            return CommandResult.FAILURE;
        }
        if (ratio > 0 && !player.hasPermission(PERM_RATIO)) {
            player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "你没有权限使用 -ratio 标志。");
            return CommandResult.FAILURE;
        }

        // 获取物品
        PlayerInventory inv = player.getInventory();
        ItemStack sourceItem = inv.getItemInMainHand();
        ItemStack targetItem = inv.getItemInOffHand();

        // 验证物品
        if (sourceItem == null || sourceItem.getType() == Material.AIR) {
            player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "请将源物品（要提取等级）放在主手。");
            return CommandResult.FAILURE;
        }
        if (targetItem == null || targetItem.getType() == Material.AIR) {
            player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "请将目标物品（要接收等级）放在副手。");
            return CommandResult.FAILURE;
        }

        // 执行转移
        TransferResult result = UpgradeTransferService.performTransfer(player, sourceItem, targetItem, freeMode, ratio);

        // 处理结果
        if (result.isSuccess()) {
            String sourceName = MMOUtils.getDisplayName(sourceItem);
            String targetName = MMOUtils.getDisplayName(targetItem);

            player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.GREEN + "转移成功！");
            player.sendMessage(ChatColor.GRAY + "  源物品 " + ChatColor.GOLD + sourceName +
                    ChatColor.GRAY + ": +" + result.getSourceOriginalLevel() + " → +0");
            player.sendMessage(ChatColor.GRAY + "  目标物品 " + ChatColor.GOLD + targetName +
                    ChatColor.GRAY + ": +" + result.getTargetOriginalLevel() + " → +" + result.getTransferredLevel());
            player.playSound(player.getLocation(), Sounds.ENTITY_PLAYER_LEVELUP, 1, 1.5f);
        } else {
            player.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + result.getMessage());
            player.playSound(player.getLocation(), Sounds.ENTITY_VILLAGER_NO, 1, 1);
        }

        return CommandResult.SUCCESS;
    }
}
