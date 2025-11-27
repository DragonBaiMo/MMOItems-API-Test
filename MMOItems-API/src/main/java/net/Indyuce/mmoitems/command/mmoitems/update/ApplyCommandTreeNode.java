package net.Indyuce.mmoitems.command.mmoitems.update;

import io.lumine.mythic.lib.command.CommandTreeExplorer;
import io.lumine.mythic.lib.command.CommandTreeNode;
import io.lumine.mythic.lib.command.argument.Argument;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.PluginUpdate;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;


public class ApplyCommandTreeNode extends CommandTreeNode {
	private final Argument<Integer> argId;

	public ApplyCommandTreeNode(CommandTreeNode parent) {
		super(parent, "apply");

		argId = addArgument(UpdateCommandTreeNode.UPDATE_ID);
	}

	@Override
	public @NotNull CommandResult execute(CommandTreeExplorer explorer, CommandSender sender, String[] args) {
		final int id = explorer.parse(this.argId);

		if (!MMOItems.plugin.getUpdates().has(id)) {
			sender.sendMessage(ChatColor.RED + "Could not find any config update with ID " + id);
			return CommandResult.FAILURE;
		}

		PluginUpdate update = MMOItems.plugin.getUpdates().get(id);
		sender.sendMessage(ChatColor.YELLOW + "Applying config update " + id + "...");
		update.apply(sender);
		sender.sendMessage(
				ChatColor.YELLOW + "Config update " + id + " was successfully applied. Check the console for potential update error logs.");
		return CommandResult.SUCCESS;
	}
}
