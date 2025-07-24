package net.Indyuce.mmoitems.stat;

import io.lumine.mythic.lib.version.VMaterial;
import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;

@HasCategory(cat = "vanilla_attribute")
@VersionDependant(version = {1, 20, 5})
public class BlockInteractionRange extends DoubleStat {
    public BlockInteractionRange() {
        super("BLOCK_INTERACTION_RANGE",
                VMaterial.SPYGLASS.get(),
                "Block Interaction Range",
                "Determines the maximum range the player can interact with blocks. Ranges between 0 and 64, with the default value being 4.5 in survival mode.");
    }
}
