package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.type.WeaponBaseStat;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
public class AttackDamage extends WeaponBaseStat {
    public AttackDamage() {
        super("ATTACK_DAMAGE",
                Material.IRON_SWORD,
                "Attack Damage",
                new String[]{"The amount of damage your weapon deals."});
    }
}
