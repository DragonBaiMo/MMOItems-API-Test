package net.Indyuce.mmoitems.command.mmoitems;

import io.lumine.mythic.lib.command.CommandTreeExplorer;
import io.lumine.mythic.lib.command.CommandTreeNode;
import io.lumine.mythic.lib.command.argument.Argument;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.ConfigFile;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.command.Arguments;
import net.Indyuce.mmoitems.gui.edition.ItemEdition;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CopyCommandTreeNode extends CommandTreeNode {
	public CopyCommandTreeNode(CommandTreeNode parent) {
		super(parent, "copy");

		addArgument(Arguments.ITEM_TYPE);
		addArgument(Arguments.ITEM_ID_2);
		addArgument(new Argument<>("new-id", (a, b) -> {
		}));
	}

	@Override
	public @NotNull CommandResult execute(CommandTreeExplorer explorer, CommandSender sender, String[] args) {
		if (args.length < 4)
			return CommandResult.THROW_USAGE;

		if (!Type.isValid(args[1])) {
			sender.sendMessage(
					MMOItems.plugin.getPrefix() + ChatColor.RED + "There is no item type called " + args[1].toUpperCase().replace("-", "_") + ".");
			sender.sendMessage(MMOItems.plugin.getPrefix() + "Type " + ChatColor.GREEN + "/mi list type " + ChatColor.GRAY
					+ "to see all the available item types.");
			return CommandResult.FAILURE;
		}

		Type type = Type.get(args[1]);
		ConfigFile config = type.getConfigFile();
		String id1 = args[2].toUpperCase().replace("-", "_");
		if (!config.getConfig().contains(id1)) {
			sender.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "There is no item called " + id1 + ".");
			return CommandResult.FAILURE;
		}

		String id2 = args[3].toUpperCase();
		if (config.getConfig().contains(id2)) {
			sender.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "There is already an item called " + id2 + "!");
			return CommandResult.FAILURE;
		}

		config.getConfig().set(id2, config.getConfig().getConfigurationSection(id1));
		config.save();
		MMOItems.plugin.getTemplates().requestTemplateUpdate(type, id2);

		if (sender instanceof Player)
			ItemEdition.of((Player) sender, MMOItems.plugin.getTemplates().getTemplate(type, id2)).open();
		sender.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.GREEN + "You successfully copied " + id1 + " to " + id2 + "!");
		return CommandResult.SUCCESS;
	}
}
