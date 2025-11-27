package net.Indyuce.mmoitems.command.mmoitems;

import io.lumine.mythic.lib.command.CommandTreeExplorer;
import io.lumine.mythic.lib.command.CommandTreeNode;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.command.Arguments;
import net.Indyuce.mmoitems.gui.ItemBrowser;
import net.Indyuce.mmoitems.gui.TypeBrowser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BrowseCommandTreeNode extends CommandTreeNode {
	public BrowseCommandTreeNode(CommandTreeNode parent) {
		super(parent, "browse");

		addArgument(Arguments.ITEM_TYPE);
	}

	@Override
	public @NotNull CommandResult execute(CommandTreeExplorer explorer, CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This command is only for players.");
			return CommandResult.FAILURE;
		}

		if (args.length < 2) {
			TypeBrowser.of((Player) sender).open();
			return CommandResult.SUCCESS;
		}

		if (!Type.isValid(args[1])) {
			sender.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "Please specify a valid item type.");
			return CommandResult.FAILURE;
		}

		ItemBrowser.of((Player) sender, Type.get(args[1])).open();
		return CommandResult.SUCCESS;
	}
}
