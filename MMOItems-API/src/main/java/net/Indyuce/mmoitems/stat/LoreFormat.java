package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.data.StringData;
import net.Indyuce.mmoitems.stat.data.StringListData;
import net.Indyuce.mmoitems.stat.type.GemStoneStat;
import io.lumine.mythic.lib.util.lang3.Validate;
import org.bukkit.Material;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.gui.edition.EditionInventory;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.StringStat;
import io.lumine.mythic.lib.api.item.ItemTag;
import org.jetbrains.annotations.NotNull;

public class LoreFormat extends StringStat implements GemStoneStat {
	public LoreFormat() {
		super("LORE_FORMAT", Material.MAP, "Lore Format", new String[] { "The lore format decides",
				"where each stat goes.", "&9Formats can be configured in", "&9the lore-formats folder" },
				new String[0]);
	}

	@Override
	public void whenApplied(@NotNull ItemStackBuilder item, @NotNull StringData data) {
		String path = data.toString();
		Validate.isTrue(MMOItems.plugin.getLore().hasFormat(path), "Could not find lore format with ID '" + path + "'");

		item.addItemTag(new ItemTag(getNBTPath(), path));
	}

	@Override
	public void whenInput(@NotNull EditionInventory inv, @NotNull String message, Object... info) {
		Validate.isTrue(MMOItems.plugin.getLore().hasFormat(message), "Couldn't find lore format with ID '" + message + "'.");

		inv.getEditedSection().set(getPath(), message);
		inv.registerTemplateEdition();
		inv.getPlayer().sendMessage(MMOItems.plugin.getPrefix() + "Lore Format successfully changed to " + message + ".");
	}
}
