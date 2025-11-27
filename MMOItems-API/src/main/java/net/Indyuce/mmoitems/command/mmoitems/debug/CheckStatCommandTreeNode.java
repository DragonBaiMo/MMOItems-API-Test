package net.Indyuce.mmoitems.command.mmoitems.debug;

import io.lumine.mythic.lib.command.CommandTreeExplorer;
import io.lumine.mythic.lib.command.CommandTreeNode;
import io.lumine.mythic.lib.command.argument.Argument;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.command.Arguments;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CheckStatCommandTreeNode extends CommandTreeNode {
    private final Argument<ItemStat<?, ?>> argStat;

    public CheckStatCommandTreeNode(CommandTreeNode parent) {
        super(parent, "checkstat");

        argStat = addArgument(Arguments.ITEM_STAT);
    }

    @Override
    public @NotNull CommandResult execute(CommandTreeExplorer explorer, CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command is only for players.");
            return CommandResult.FAILURE;
        }

        final var stat = explorer.parse(argStat);

        Player player = (Player) sender;
        player.sendMessage("Found stat with ID " + stat.getId() + " = " + PlayerData.get((Player) sender).getStat(stat));
        return CommandResult.SUCCESS;
    }
}
