package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
public class ArmorToughness extends DoubleStat {
    public ArmorToughness() {
        super("ARMOR_TOUGHNESS",
                Material.DIAMOND_CHESTPLATE,
                "Armor Toughness",
                "Armor toughness reduces damage taken.");
    }
}
