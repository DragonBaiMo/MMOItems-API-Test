package net.Indyuce.mmoitems.comp.mythicmobs.crafting;

import io.lumine.mythic.api.skills.Skill;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.utils.MythicUtil;
import io.lumine.mythic.lib.api.MMOLineConfig;
import io.lumine.mythic.lib.util.lang3.Validate;
import net.Indyuce.mmoitems.api.crafting.trigger.Trigger;
import net.Indyuce.mmoitems.api.player.PlayerData;
import org.bukkit.entity.Entity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MythicMobsSkillTrigger extends Trigger {
    private final Skill skill;

    public MythicMobsSkillTrigger(MMOLineConfig config) {
        super("mmskill");

        config.validate("id");
        String id = config.getString("id");
        Optional<Skill> opt = MythicBukkit.inst().getSkillManager().getSkill(id);
        Validate.isTrue(opt.isPresent(), "Could not find MM skill " + id);
        skill = opt.get();
    }

    @Override
    public void whenCrafting(PlayerData data) {
        if (!data.isOnline()) return;

        var targetedEntity = MythicUtil.getTargetedEntity(data.getPlayer());
        List<Entity> targets = targetedEntity == null ? Collections.emptyList() : Collections.singletonList(targetedEntity);
        var origin = data.getPlayer().getLocation();

        MythicBukkit.inst().getAPIHelper().castSkill(data.getPlayer(), this.skill.getInternalName(), data.getPlayer(), origin, targets, null, 1);
    }
}
