package net.Indyuce.mmoitems.command.mmoitems.update;

import io.lumine.mythic.lib.command.CommandTreeExplorer;
import io.lumine.mythic.lib.command.CommandTreeNode;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.PluginUpdate;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ListCommandTreeNode extends CommandTreeNode {
	public ListCommandTreeNode(CommandTreeNode parent) {
		super(parent, "list");
	}

	@Override
	public @NotNull CommandResult execute(CommandTreeExplorer explorer, CommandSender sender, String[] args) {
		sender.sendMessage(ChatColor.LIGHT_PURPLE + "Available Config Updates");
		for (PluginUpdate update : MMOItems.plugin.getUpdates().getAll())
			sender.sendMessage(ChatColor.DARK_GRAY + "- Update " + update.getId());
		return CommandResult.SUCCESS;
	}
}
