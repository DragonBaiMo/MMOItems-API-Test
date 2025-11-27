package net.Indyuce.mmoitems.command.mmoitems.debug;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.command.CommandTreeExplorer;
import io.lumine.mythic.lib.command.CommandTreeNode;
import io.lumine.mythic.lib.command.argument.Argument;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetTagCommandTreeNode extends CommandTreeNode {
	public SetTagCommandTreeNode(CommandTreeNode parent) {
		super(parent, "settag");

		addArgument(new Argument<>("path", (explorer, list) -> list.add("TagPath")));
		addArgument(new Argument<>("value", (explorer, list) -> list.add("TagValue")));
	}

	@Override
	public @NotNull CommandResult execute(CommandTreeExplorer explorer, CommandSender sender, String[] args) {
		if (args.length < 4)
			return CommandResult.THROW_USAGE;

		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This command is only for players.");
			return CommandResult.FAILURE;
		}

		try {
			Player player = (Player) sender;
			player.getInventory().setItemInMainHand(MythicLib.plugin.getVersion().getWrapper().getNBTItem(player.getInventory().getItemInMainHand())
					.addTag(new ItemTag(args[2].toUpperCase().replace("-", "_"), args[3].replace("%%", " "))).toItem());
			player.sendMessage("Successfully set tag.");
			return CommandResult.SUCCESS;

		} catch (Exception exception) {
			sender.sendMessage("Couldn't set tag.");
			return CommandResult.FAILURE;
		}
	}
}
