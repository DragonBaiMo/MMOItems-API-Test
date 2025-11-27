package net.Indyuce.mmoitems.command.mmoitems.debug;

import io.lumine.mythic.lib.command.CommandTreeExplorer;
import io.lumine.mythic.lib.command.CommandTreeNode;
import io.lumine.mythic.lib.command.argument.Argument;
import io.lumine.mythic.lib.command.argument.Arguments;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

public class CheckAttributeCommandTreeNode extends CommandTreeNode {
    private final Argument<Attribute> argAttribute;
    private final Argument<Player> argPlayer;

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.####");

    public CheckAttributeCommandTreeNode(CommandTreeNode parent) {
        super(parent, "checkattribute");

        argAttribute = addArgument(Argument.VANILLA_ATTRIBUTE);
        argPlayer = addArgument(Argument.PLAYER_OR_SENDER);
    }

    @NotNull
    @Override
    public CommandResult execute(CommandTreeExplorer explorer, CommandSender sender, String[] args) {
        Arguments.isTrue(sender instanceof Player, "This command is only for players.");

        final var attribute = explorer.parse(argAttribute);
        final var player = explorer.parse(argPlayer);
        final var instance = player.getAttribute(attribute);

        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "--------------------------------------------------");
        sender.sendMessage(ChatColor.AQUA + "Default Value = " + ChatColor.RESET + instance.getDefaultValue());
        sender.sendMessage(ChatColor.AQUA + "Base Value = " + ChatColor.RESET + instance.getBaseValue());
        sender.sendMessage(ChatColor.AQUA + "Value = " + ChatColor.RESET + instance.getValue());
        for (AttributeModifier mod : instance.getModifiers())
            sender.sendMessage(mod.getName() + " " + DECIMAL_FORMAT.format(mod.getAmount()) + " " + mod.getOperation() + " " + mod.getSlot());

        return CommandResult.SUCCESS;
    }
}
