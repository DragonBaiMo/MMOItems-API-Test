package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;

@HasCategory(cat = "vanilla_attribute")
public class MovementSpeed extends DoubleStat {
    public MovementSpeed() {
        super("MOVEMENT_SPEED",
                Material.LEATHER_BOOTS,
                "Movement Speed",
                "Increases the player's walking speed. Default MC walk speed is 0.1");
    }

    @Override
    public double multiplyWhenDisplaying() {
        return 100;
    }
}
