package net.Indyuce.mmoitems.stat;

import io.lumine.mythic.lib.version.VMaterial;
import net.Indyuce.mmoitems.stat.annotation.HasCategory;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import net.Indyuce.mmoitems.stat.type.DoubleStat;

@HasCategory(cat = "vanilla_attribute")
@VersionDependant(version = {1, 20, 5})
public class EntityInteractionRange extends DoubleStat {
    public EntityInteractionRange() {
        super("ENTITY_INTERACTION_RANGE",
                VMaterial.SPYGLASS.get(),
                "Entity Interaction Range",
                "Determines the maximum range the player can interact with entities. Ranges between 0 and 64, with the default value being 3."
        );
    }
}
