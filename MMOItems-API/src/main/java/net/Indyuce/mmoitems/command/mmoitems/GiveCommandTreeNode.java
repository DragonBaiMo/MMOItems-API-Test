package net.Indyuce.mmoitems.command.mmoitems;

import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.command.CommandTreeExplorer;
import io.lumine.mythic.lib.command.CommandTreeNode;
import io.lumine.mythic.lib.command.argument.Argument;
import io.lumine.mythic.lib.util.SmartGive;
import io.lumine.mythic.lib.util.lang3.Validate;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.util.RandomAmount;
import net.Indyuce.mmoitems.api.util.message.Message;
import net.Indyuce.mmoitems.command.Arguments;
import net.Indyuce.mmoitems.stat.data.SoulboundData;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class GiveCommandTreeNode extends CommandTreeNode {
    private final Argument<Type> argType;
    private final Argument<String> argId;
    private final Argument<Player> argPlayer;
    private final Argument<RandomAmount> argAmount;
    private final Argument<Double> argUnidentifiedChance;
    private final Argument<Double> argDropChance;
    private final Argument<Double> argSoulboundChance;
    private final Argument<String> argSilent;

    public GiveCommandTreeNode(CommandTreeNode parent) {
        super(parent, "give");

        argType = addArgument(Arguments.ITEM_TYPE);
        argId = addArgument(Arguments.ITEM_ID_2);
        argPlayer = addArgument(Argument.PLAYER_OR_SENDER);
        argAmount = addArgument(new Argument<>("min-max",
                (explore, list) -> list.addAll(Arrays.asList("1", "10", "64", "1-3", "1-10")),
                (explore, input) -> new RandomAmount(input),
                explore -> new RandomAmount(1, 1)
        ));
        argUnidentifiedChance = addArgument(Arguments.CHANCE.withKey("unidentified_chance"));
        argDropChance = addArgument(Arguments.CHANCE.withKey("drop_chance").withFallback(explore -> 100d));
        argSoulboundChance = addArgument(Arguments.CHANCE.withKey("soulbound_chance"));
        argSilent = addArgument(new Argument<>("silent",
                (explore, list) -> list.addAll(Arrays.asList("silent", "s")),
                (explore, input) -> input,
                explore -> ""));
    }

    @Override
    public @NotNull CommandResult execute(CommandTreeExplorer explorer, CommandSender sender, String[] args) {

        final var target = explorer.parse(argPlayer);

        // item
        final var type = explorer.parse(argType);
        final var template = Arguments.getTemplate(type, explorer.parse(argId));
        final var amount = explorer.parse(argAmount);
        final var unidentify = explorer.parse(argUnidentifiedChance) / 100;
        final var drop = explorer.parse(argDropChance) / 100;
        final var soulbound = explorer.parse(argSoulboundChance) / 100;
        final var rawSilent = explorer.parse(argSilent);
        boolean silent = (rawSilent.equalsIgnoreCase("silent") || rawSilent.equalsIgnoreCase("s"));

        // roll drop chance
        if (RANDOM.nextDouble() > drop)
            return CommandResult.SUCCESS;

        // generate mmoitem
        MMOItem mmoitem = template.newBuilder(PlayerData.get(target).getRPG()).build();

        // roll soulbound
        if (RANDOM.nextDouble() < soulbound)
            mmoitem.setData(ItemStats.SOULBOUND, new SoulboundData(target, 1));

        // generate item
        ItemStack item = mmoitem.newBuilder().build();
        Validate.isTrue(item != null && item.getType() != Material.AIR,
                "Couldn't find/generate the item called '" + template.getId() + "'. Check your console for potential item generation issues.");

        // roll unidentification
        if (RANDOM.nextDouble() < unidentify)
            item = type.getUnidentifiedTemplate().newBuilder(NBTItem.get(item)).build();

        // set amount
        int amountComputed = amount.getRandomAmount();
        Validate.isTrue(amountComputed > 0, "Amount must be positive");
        item.setAmount(amountComputed);

        // message
        if (!silent) {
            Message.RECEIVED_ITEM.format(ChatColor.YELLOW, "#item#", MMOUtils.getDisplayName(item), "#amount#",
                    (amountComputed > 1 ? " x" + amountComputed : "")).send(target);
            if (!sender.equals(target))
                sender.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.YELLOW + "Successfully gave " + ChatColor.GOLD
                        + MMOUtils.getDisplayName(item) + (amountComputed > 1 ? " x" + amountComputed : "") + ChatColor.YELLOW + " to "
                        + ChatColor.GOLD + target.getName() + ChatColor.YELLOW + ".");
        }

        // item
        new SmartGive(target).give(item);
        return CommandResult.SUCCESS;
    }
}
