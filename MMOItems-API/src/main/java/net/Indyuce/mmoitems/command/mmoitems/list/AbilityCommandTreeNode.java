package net.Indyuce.mmoitems.command.mmoitems.list;

import io.lumine.mythic.lib.command.CommandTreeExplorer;
import io.lumine.mythic.lib.command.CommandTreeNode;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.skill.RegisteredSkill;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class AbilityCommandTreeNode extends CommandTreeNode {
	public AbilityCommandTreeNode(CommandTreeNode parent) {
		super(parent, "ability");
	}

	@Override
	public @NotNull CommandResult execute(CommandTreeExplorer explorer, CommandSender sender, String[] args) {
		sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "-----------------[" + ChatColor.LIGHT_PURPLE + " Abilities "
				+ ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "]-----------------");
		sender.sendMessage(ChatColor.WHITE + "Here are all the abilities you can bind to items.");
		sender.sendMessage(ChatColor.WHITE + "The values inside brackets are " + ChatColor.UNDERLINE + "modifiers" + ChatColor.WHITE
				+ " which allow you to change the ability values (cooldown, damage...)");
		for (RegisteredSkill ability : MMOItems.plugin.getSkills().getAll()) {
			String modFormat = ChatColor.GRAY + String.join(ChatColor.WHITE + ", " + ChatColor.GRAY, ability.getHandler().getModifiers());
			modFormat = ChatColor.WHITE + "(" + modFormat + ChatColor.WHITE + ")";
			sender.sendMessage("* " + ChatColor.LIGHT_PURPLE + ability.getName() + " " + modFormat);
		}
		return CommandResult.SUCCESS;
	}
}
