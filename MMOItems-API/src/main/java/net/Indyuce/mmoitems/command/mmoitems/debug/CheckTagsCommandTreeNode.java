package net.Indyuce.mmoitems.command.mmoitems.debug;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.command.CommandTreeExplorer;
import io.lumine.mythic.lib.command.CommandTreeNode;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CheckTagsCommandTreeNode extends CommandTreeNode {
	public CheckTagsCommandTreeNode(CommandTreeNode parent) {
		super(parent, "checktags");
	}

	@Override
	public @NotNull CommandResult execute(CommandTreeExplorer explorer, CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This command is only for players.");
			return CommandResult.FAILURE;
		}

		Player player = (Player) sender;
		player.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "--------------------------------------------------");
		for (String s : MythicLib.plugin.getVersion().getWrapper().getNBTItem(player.getInventory().getItemInMainHand()).getTags())
			player.sendMessage("- " + s);
		return CommandResult.SUCCESS;
	}
}
