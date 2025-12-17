package net.Indyuce.mmoitems.api.interaction.weapon;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.player.EquipmentSlot;
import io.lumine.mythic.lib.comp.flags.CustomFlag;
import io.lumine.mythic.lib.damage.DamageType;
import io.lumine.mythic.lib.damage.MeleeAttackMetadata;
import io.lumine.mythic.lib.player.PlayerMetadata;
import io.lumine.mythic.lib.skill.SimpleSkill;
import io.lumine.mythic.lib.skill.SkillMetadata;
import io.lumine.mythic.lib.skill.handler.SkillHandler;
import io.lumine.mythic.lib.skill.result.SkillResult;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.event.item.UntargetedWeaponUseEvent;
import net.Indyuce.mmoitems.api.interaction.UseItem;
import net.Indyuce.mmoitems.api.interaction.WeaponAttackResult;
import net.Indyuce.mmoitems.api.interaction.util.DurabilityItem;
import net.Indyuce.mmoitems.api.interaction.weapon.untargeted.LegacyWeapon;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.util.message.Message;
import net.Indyuce.mmoitems.stat.ActionLeftClick;
import net.Indyuce.mmoitems.combat.CombatContext;
import net.Indyuce.mmoitems.combat.CombatModifierPipeline;
import net.Indyuce.mmoitems.combat.modifier.DistanceBonusModifier;
import net.Indyuce.mmoitems.combat.modifier.FlatReductionModifier;
import net.Indyuce.mmoitems.combat.modifier.PercentReductionModifier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class Weapon extends UseItem {
    @Deprecated
    public Weapon(Player player, NBTItem item) {
        this(PlayerData.get(player), item);
    }

    public Weapon(PlayerData playerData, NBTItem item) {
        super(playerData, item);
    }

    @Override
    public boolean checkItemRequirements(boolean message) {

        // Light checks first
        if (playerData.isEncumbered()) {
            Message.HANDS_TOO_CHARGED.format(ChatColor.RED).send(getPlayer());
            return false;
        }

        // Check for class, level... then flags
        return playerData.getRPG().canUse(getNBTItem(), message) && flagCheck(MMOItems.plugin.getLanguage().weaponFlagChecks, CustomFlag.MI_WEAPONS);
    }

    /**
     * Only applies mana and stamina weapon costs
     *
     * @return If the attack was cast successfully
     */
    public boolean checkAndApplyWeaponCosts() {
        if (!checkWeaponCosts(false)) return false;
        applyWeaponCosts(null);
        return true;
    }

    /**
     * @return If instantaneous weapon costs are met.
     * @see #applyWeaponCosts(Double)
     */
    public boolean checkWeaponCosts(boolean cooldowns) {
        if (cooldowns && getPlayerData().getMMOPlayerData().getCooldownMap().isOnCooldown(mmoitem.getType()))
            return false;

        double manaCost = getNBTItem().getStat("MANA_COST");
        if (manaCost > 0 && playerData.getRPG().getMana() < manaCost) {
            Message.NOT_ENOUGH_MANA.format(ChatColor.RED).send(getPlayer());
            return false;
        }

        double staminaCost = getNBTItem().getStat("STAMINA_COST");
        if (staminaCost > 0 && playerData.getRPG().getStamina() < staminaCost) {
            Message.NOT_ENOUGH_STAMINA.format(ChatColor.RED).send(getPlayer());
            return false;
        }

        return true;
    }

    /**
     * Hard coded instantaneous weapon costs are as follows:
     * - mana and stamina (resources)
     * - cooldown (given by the item type attack cooldown key)
     * <p>
     * Other weapon costs are inherent to the item type and are
     * fully configurable inside of the types config file.
     *
     * @see #checkWeaponCosts(boolean)
     */
    public void applyWeaponCosts(@Nullable Double attackDelay) {

        final double manaCost = getNBTItem().getStat("MANA_COST");
        if (manaCost > 0) playerData.getRPG().giveMana(-manaCost);

        final double staminaCost = getNBTItem().getStat("STAMINA_COST");
        if (staminaCost > 0) playerData.getRPG().giveStamina(-staminaCost);

        if (attackDelay != null)
            getPlayerData().getMMOPlayerData().getCooldownMap().applyCooldown(mmoitem.getType(), attackDelay);
    }

    /**
     * Only applies mana and stamina costs. Cooldown is not required for
     * targeted attacks since the vanilla attack bar already does that.
     *
     * @param attackMeta The attack being performed
     * @param attacker   The player attacker
     * @param target     The attack target
     * @return If the attack is successful, or if it was canceled otherwise
     */
    public boolean handleTargetedAttack(@NotNull MeleeAttackMetadata attackMeta,
                                        @NotNull PlayerMetadata attacker,
                                        @NotNull LivingEntity target,
                                        @NotNull Event sourceEvent) {

        // Handle weapon mana and stamina costs ONLY
        if (!checkAndApplyWeaponCosts()) return false;

        // 设置该物品类型的近战攻击伤害类型
        attackMeta.getDamage().getInitialPacket().setTypes(mmoitem.getType().getAttackDamageTypes().toArray(new DamageType[0]));

        // Handle on-hit attack effects
        final SkillHandler onHitSkill = mmoitem.getType().onAttack();
        if (onHitSkill != null && !getNBTItem().getBoolean("MMOITEMS_DISABLE_ATTACK_PASSIVE"))
            new SimpleSkill(onHitSkill).cast(SkillMetadata.of(attacker, target, attackMeta, sourceEvent));

        // 统一战斗修正：固定减伤 -> 百分比减伤 -> 距离加成
        applyCombatModifiers(attackMeta, target);

        return true;
    }

    @Nullable
    private SkillHandler findClickSkill(boolean rightClick) {
        String skillId = getNBTItem().getString((rightClick ? ItemStats.RIGHT_CLICK_SCRIPT : ItemStats.LEFT_CLICK_SCRIPT).getNBTPath());

        // (Deprecated) Support for staff spirits on left-clicks
        if (!rightClick && (skillId == null || skillId.isEmpty()))
            skillId = getNBTItem().getString(ActionLeftClick.LEGACY_PATH);

        if (skillId == null || skillId.isEmpty()) {

            // Find item type action
            final Type itemType = mmoitem.getType();
            return rightClick ? itemType.onRightClick() : itemType.onLeftClick();
        }

        // Override item type with custom action
        return MythicLib.plugin.getSkills().getHandler(skillId);
    }

    /**
     * Called when the player interacts with the item. This method is used to
     * apply durability and cast the weapon attack
     *
     * @param rightClick Is the click a right click? If false, it's a left click.
     * @param actionHand Slot being interacted with
     * @return Null if there are no attack detected
     * @implNote Since MI 6.7.3 this method handles custom durability, cooldown
     *         checks and player stat snapshots.
     */
    @Nullable
    public WeaponAttackResult handleUntargetedAttack(boolean rightClick, @NotNull EquipmentSlot actionHand) {
        if (this instanceof LegacyWeapon) return legacyHandleUntargetedAttack(rightClick, actionHand);

        // Check for item type attack effect
        final SkillHandler handler = findClickSkill(rightClick);
        if (handler == null) return WeaponAttackResult.NO_ATTACK;

        // Check for attack effect conditions
        final SkillMetadata meta = SkillMetadata.of(getPlayerData().getMMOPlayerData(), actionHand);
        final SkillResult result = handler.getResult(meta);
        if (!result.isSuccessful()) return WeaponAttackResult.NO_ATTACK;

        // Check for durability
        DurabilityItem durItem = DurabilityItem.from(getPlayer(), getNBTItem(), actionHand.toBukkit());
        if (durItem != null && durItem.isBroken()) return WeaponAttackResult.DURABILITY;

        // Apply weapon instantaneous costs
        if (!checkWeaponCosts(true)) return WeaponAttackResult.WEAPON_COSTS;

        // Check for Bukkit event
        UntargetedWeaponUseEvent called = new UntargetedWeaponUseEvent(playerData, this);
        Bukkit.getPluginManager().callEvent(called);
        if (called.isCancelled()) return WeaponAttackResult.BUKKIT_EVENT;

        // 先尝试自动绑定（仅在绑定成功时由工具方法写回对应手槽）
        final boolean boundNow = net.Indyuce.mmoitems.util.AutoBindUtil.applyAutoBindIfNeeded(playerData, getNBTItem(), actionHand.toBukkit());

        /*
         * 关键修复：自动绑定会重建并写回手持物品；而 DurabilityItem 是在绑定之前创建的，
         * 若后续 updateInInventory 使用旧引用，会把“未绑定”的旧物品覆盖回去，从而导致反复提示已绑定。
         * 因此：当本次确实发生了绑定时，重新基于当前手持物品构建 DurabilityItem。
         */
        if (boundNow && durItem != null) {
            try {
                final org.bukkit.inventory.ItemStack currentInHand = getPlayer().getInventory().getItem(actionHand.toBukkit());
                durItem = DurabilityItem.from(getPlayer(), currentInHand, actionHand.toBukkit());
            } catch (Throwable ignored) {
                // 兼容性保护：不因耐久更新器重建失败影响技能释放
            }
        }

        // Attack is ready to be performed.
        // Apply weapon costs
        final double attackDelay = 1 / requireNonZero(meta.getCaster().getStat("ATTACK_SPEED"), MMOItems.plugin.getConfig().getDouble("default.attack-speed"));
        applyWeaponCosts(attackDelay);

        // Apply weapon attack effect
        handler.whenCast(result, meta);

        // Apply durability loss
        if (durItem != null) durItem.decreaseDurability(1).updateInInventory();
        return WeaponAttackResult.SUCCESS;
    }

    @Deprecated
    public boolean handleTargetedAttack(MeleeAttackMetadata attackMeta, PlayerMetadata attacker, LivingEntity target) {
        return this.handleTargetedAttack(attackMeta, attacker, target, null);
    }

    @Deprecated
    public boolean handleTargetedAttack(io.lumine.mythic.lib.damage.AttackMetadata attackMeta, PlayerMetadata attacker, LivingEntity target) {
        return this.handleTargetedAttack((MeleeAttackMetadata) attackMeta, attacker, target, null);
    }

    @Deprecated
    private WeaponAttackResult legacyHandleUntargetedAttack(boolean rightClick, @NotNull EquipmentSlot actionHand) {

        // Check for attack effect conditions
        if (!((LegacyWeapon) this).canAttack(rightClick, actionHand)) return WeaponAttackResult.NO_ATTACK;

        // Check for durability
        DurabilityItem durItem = DurabilityItem.from(getPlayer(), getNBTItem(), actionHand.toBukkit());
        if (durItem != null && durItem.isBroken()) return WeaponAttackResult.DURABILITY;

        // Apply weapon instantaneous costs
        PlayerMetadata stats = playerData.getMMOPlayerData().getStatMap().cache(actionHand);
        if (!checkWeaponCosts(true)) return WeaponAttackResult.WEAPON_COSTS;

        // Check for Bukkit event
        UntargetedWeaponUseEvent called = new UntargetedWeaponUseEvent(playerData, this);
        Bukkit.getPluginManager().callEvent(called);
        if (called.isCancelled()) return WeaponAttackResult.BUKKIT_EVENT;

        // Attack is ready to be performed.
        // Apply weapon costs
        final double attackDelay = 1 / requireNonZero(stats.getStat("ATTACK_SPEED"), MMOItems.plugin.getConfig().getDouble("default.attack-speed"));
        applyWeaponCosts(attackDelay);

        // Apply weapon attack effect
        ((LegacyWeapon) this).applyAttackEffect(stats, actionHand);

        // Apply durability loss
        if (durItem != null) durItem.decreaseDurability(1).updateInInventory();
        return WeaponAttackResult.SUCCESS;
    }

    @Deprecated
    protected Location getGround(Location loc) {
        for (int j = 0; j < 20; j++) {
            if (loc.getBlock().getType().isSolid()) return loc;
            loc.add(0, -1, 0);
        }
        return loc;
    }

    /**
     * @return First argument, or second if zero or lower
     */
    protected double requireNonZero(double number, double elseNumber) {
        return number <= 0 ? elseNumber : number;
    }

    /**
     * 使用 CombatModifier 管线对伤害进行额外修正。
     */
    private void applyCombatModifiers(@NotNull MeleeAttackMetadata attackMeta, @NotNull LivingEntity target) {
        double base = attackMeta.getDamage().getDamage();
        if (base <= 0D) {
            return;
        }
        CombatModifierPipeline pipeline = new CombatModifierPipeline();
        pipeline.register(new FlatReductionModifier());
        pipeline.register(new PercentReductionModifier());
        pipeline.register(new DistanceBonusModifier());

        CombatContext ctx = new CombatContext(playerData.getPlayer(), target, attackMeta,
                MythicLib.plugin.getVersion().getWrapper().getNBTItem(playerData.getPlayer().getInventory().getItem(attackMeta.getHand().toBukkit())),
                playerData);
        double finalDamage = pipeline.applyAll(ctx, base);
        if (finalDamage != base) {
            attackMeta.getDamage().multiplicativeModifier(finalDamage / base);
        }
    }
}
