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

public class CreateCommandTreeNode extends CommandTreeNode {
    public CreateCommandTreeNode(CommandTreeNode parent) {
        super(parent, "create");

        addArgument(Arguments.ITEM_TYPE);
        addArgument(new Argument<>("id", (explorer, list) -> list.add("NEW_ITEM_ID")));
    }

    @Override
    public @NotNull CommandResult execute(CommandTreeExplorer explorer, CommandSender sender, String[] args) {
        if (args.length < 3)
            return CommandResult.THROW_USAGE;

        if (!Type.isValid(args[1])) {
            sender.sendMessage(
                    MMOItems.plugin.getPrefix() + ChatColor.RED + "There is no item type called " + args[1].toUpperCase().replace("-", "_") + ".");
            sender.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "Type " + ChatColor.GREEN + "/mi list type" + ChatColor.RED
                    + " to see all the available item types.");
            return CommandResult.FAILURE;
        }

        Type type = Type.get(args[1]);
        String name = args[2].toUpperCase().replace("-", "_");
        ConfigFile config = type.getConfigFile();
        if (config.getConfig().contains(name)) {
            sender.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "There is already an item called " + name + ".");
            return CommandResult.FAILURE;
        }

        config.getConfig().set(name + ".base.material", type.getItem().getType().name());
        config.save();
        MMOItems.plugin.getTemplates().requestTemplateUpdate(type, name);

        if (sender instanceof Player)
            ItemEdition.of((Player) sender, MMOItems.plugin.getTemplates().getTemplate(type, name)).open();
        sender.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.GREEN + "You successfully created " + name + "!");
        return CommandResult.SUCCESS;
    }
}
