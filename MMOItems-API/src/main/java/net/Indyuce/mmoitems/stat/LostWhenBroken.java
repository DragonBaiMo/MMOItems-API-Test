package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.type.BooleanStat;
import org.bukkit.Material;

public class LostWhenBroken extends BooleanStat {
	public LostWhenBroken() {
		super("WILL_BREAK", Material.SHEARS, "Lost when Broken?", new String[] { "Items with custom durability do not break", "by default when reaching 0 durability.", "Toggle this option on to have your item break.", "", "By default, vanilla items break when reaching", "0 durability. Toggle this option on to have it not break instead." }, new String[] { "!block", "all" });
	}
}
