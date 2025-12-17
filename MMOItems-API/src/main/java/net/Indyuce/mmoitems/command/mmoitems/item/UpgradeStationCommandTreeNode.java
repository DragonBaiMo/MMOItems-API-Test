package net.Indyuce.mmoitems.command.mmoitems.item;

import io.lumine.mythic.lib.command.CommandTreeExplorer;
import io.lumine.mythic.lib.command.CommandTreeNode;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.util.message.Message;
import net.Indyuce.mmoitems.gui.UpgradeStationGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 强化工作台命令节点
 * <p>
 * 命令格式：/mi item station
 * </p>
 * <p>
 * 功能：为玩家打开强化工作台 GUI 界面
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public class UpgradeStationCommandTreeNode extends CommandTreeNode {

    /**
     * 权限节点
     */
    private static final String PERM = "mmoitems.command.item.station";

    public UpgradeStationCommandTreeNode(CommandTreeNode parent) {
        super(parent, "station");
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
        if (!player.hasPermission(PERM)) {
            Message.NOT_ENOUGH_PERMS_COMMAND.format(ChatColor.RED).send(player);
            return CommandResult.FAILURE;
        }

        // 打开强化工作台 GUI
        new UpgradeStationGUI(player).open();
        return CommandResult.SUCCESS;
    }
}
