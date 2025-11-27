package net.Indyuce.mmoitems.command.mmoitems.update;

import io.lumine.mythic.lib.command.CommandTreeExplorer;
import io.lumine.mythic.lib.command.CommandTreeNode;
import io.lumine.mythic.lib.command.argument.Argument;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.PluginUpdate;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class InfoCommandTreeNode extends CommandTreeNode {
    private final Argument<Integer> argId;

    public InfoCommandTreeNode(CommandTreeNode parent) {
        super(parent, "info");

        argId = addArgument(UpdateCommandTreeNode.UPDATE_ID);
    }

	@Override
	public @NotNull CommandResult execute(CommandTreeExplorer explorer, CommandSender sender, String[] args) {
		final int id = explorer.parse(argId);

		if (!MMOItems.plugin.getUpdates().has(id)) {
			sender.sendMessage(ChatColor.RED + "Could not find any config update with ID " + id);
			return CommandResult.FAILURE;
		}

		PluginUpdate update = MMOItems.plugin.getUpdates().get(id);

		sender.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Config Update n" + update.getId());
		if (update.hasDescription()) {
			sender.sendMessage("");
			sender.sendMessage(ChatColor.DARK_GRAY + "Description:");
			for (String line : update.getDescription())
				sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.translateAlternateColorCodes('&', line));
		}

		sender.sendMessage("");
		sender.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.GOLD + "/mi update apply " + update.getId() + ChatColor.YELLOW
				+ " to apply this config update.");
		return CommandResult.SUCCESS;
	}
}
