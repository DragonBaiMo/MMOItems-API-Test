package net.Indyuce.mmoitems.listener;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.api.event.PlayerAttackEvent;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.player.EquipmentSlot;
import io.lumine.mythic.lib.comp.interaction.InteractionType;
import io.lumine.mythic.lib.damage.MeleeAttackMetadata;
import io.lumine.mythic.lib.entity.ProjectileMetadata;
import io.lumine.mythic.lib.entity.ProjectileType;
import io.lumine.mythic.lib.skill.SimpleSkill;
import io.lumine.mythic.lib.skill.handler.SkillHandler;
import io.lumine.mythic.lib.skill.trigger.TriggerMetadata;
import io.lumine.mythic.lib.skill.trigger.TriggerType;
import net.Indyuce.mmoitems.MMOItems;
import io.lumine.mythic.lib.version.Sounds;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.event.item.SpecialWeaponAttackEvent;
import net.Indyuce.mmoitems.api.event.item.ApplySoulboundEvent;
import net.Indyuce.mmoitems.api.event.item.UntargetedWeaponUseEvent;
import net.Indyuce.mmoitems.api.interaction.*;
import net.Indyuce.mmoitems.api.interaction.projectile.ArrowParticles;
import net.Indyuce.mmoitems.api.interaction.weapon.Weapon;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.util.message.Message;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.VolatileMMOItem;
import net.Indyuce.mmoitems.stat.data.SoulboundData;
import net.Indyuce.mmoitems.stat.data.BooleanData;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import net.Indyuce.mmoitems.stat.data.type.Mergeable;
import net.Indyuce.mmoitems.util.MMOUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

public class ItemUse implements Listener {

    @EventHandler
    public void rightClickEffects(PlayerInteractEvent event) {

        // NPCs sometimes randomly calling click events
        final var playerData = PlayerData.getOrNull(event.getPlayer());
        if (playerData == null) return;

        // [WTF BUKKIT] When hitting entities, `event.getItem()` is set to `null`
        final var eventItem = resolveEventItem(event);
        final NBTItem item = NBTItem.get(eventItem);

        // PlayerInteracts cancellability are a little bit trickier
        if (event.useItemInHand() == Event.Result.DENY) return;

        /*
         * Disables both clicks if corresponding option is found on the item.
         * Also disabled interactions with unidentified items.
         *
         * This does NOT prevent further MMOItems interactions, which is why this
         * flag set is located here and not in another event listener.
         */
        if (item.getBoolean("MMOITEMS_DISABLE_INTERACTION") || item.hasTag("MMOITEMS_UNIDENTIFIED_ITEM"))
            event.setUseItemInHand(Event.Result.DENY);

        // [WTF BUKKIT] Ignore interacts that are due to pressing Q (dropping items)
        if (playerData.getMMOPlayerData().lastDrop + 50 > System.currentTimeMillis()) return;

        // No interaction with air
        if (UtilityMethods.isAir(eventItem)) return;

        final Type itemType = Type.get(item);
        if (itemType == null) return;

        /*
         * Some consumables must be fully eaten through the vanilla eating
         * animation and are handled there {@link #handleVanillaEatenConsumables(PlayerItemConsumeEvent)}
         */
        final Player player = event.getPlayer();
        final UseItem useItem = itemType.toUseItem(playerData, item);
        if (useItem instanceof Consumable) {

            // Vanilla eating is handled within another event
            if (((Consumable) useItem).hasVanillaEating()) return;

            // Disable clicks on interactable blocks
            if (event.hasBlock()
                    && MMOItems.plugin.getLanguage().disableConsumableBlockClicks
                    && MMOUtils.isInteractable(event.getClickedBlock())) return;
        }

        // Disable most interactions (shield blocking, eating...)
        if (!useItem.checkItemRequirements()) {
            event.setUseItemInHand(Event.Result.DENY);
            return;
        }

        // Commands & Consumables
        final boolean rightClick = event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK;
        if (rightClick) {
            if (useItem.getPlayerData().getMMOPlayerData().getCooldownMap().isOnCooldown(useItem.getMMOItem())) {
                final double cd = useItem.getPlayerData().getMMOPlayerData().getCooldownMap().getCooldown(useItem.getMMOItem());
                Message.ITEM_ON_COOLDOWN.format(ChatColor.RED, "#left#", MythicLib.plugin.getMMOConfig().decimal.format(cd), "#s#", cd >= 2 ? "s" : "").send(player);
                event.setUseItemInHand(Event.Result.DENY);
                return;
            }

            // 冷却检查通过后，尝试自动绑定
            tryAutoBindOnUse(playerData, item);

            if (useItem instanceof Consumable) {
                event.setUseItemInHand(Event.Result.DENY);
                Consumable.ConsumableConsumeResult result = ((Consumable) useItem).useOnPlayer(event.getHand(), false);
                if (result == Consumable.ConsumableConsumeResult.CANCEL) return;

                else if (result == Consumable.ConsumableConsumeResult.CONSUME)
                    eventItem.setAmount(eventItem.getAmount() - 1);
            }

            useItem.getPlayerData().getMMOPlayerData().getCooldownMap().applyCooldown(useItem.getMMOItem(), useItem.getNBTItem().getStat("ITEM_COOLDOWN"));
            useItem.executeCommands();
        }

        // Target-free weapon effects
        if (useItem instanceof Weapon)
            ((Weapon) useItem).handleUntargetedAttack(rightClick, EquipmentSlot.fromBukkit(event.getHand()));
    }

    @Nullable
    private static ItemStack resolveEventItem(PlayerInteractEvent event) {
        if (event.hasItem()) return event.getItem();
        if (event.getHand() != null) return event.getPlayer().getInventory().getItem(event.getHand());
        return null;
    }

    /**
     * 当物品具有 AUTO_BIND_ON_USE 属性且尚未绑定时，在“使用”发生时自动绑定到玩家。
     * - 仅处理 MMOItems 物品；
     * - 跳过成组物品（与消耗品绑定逻辑一致）；
     * - 触发 ApplySoulboundEvent 以允许外部取消；
     * - 等级从 SOULBOUND_LEVEL 读取，默认为 1。
     */
    private static void tryAutoBindOnUse(@NotNull PlayerData playerData, @NotNull NBTItem item) {
        // 必须是 MMOItems 物品
        if (Type.get(item) == null) return;

        // 无该开关或已绑定则跳过
        if (!item.getBoolean("MMOITEMS_AUTO_BIND_ON_USE")) return;

        final MMOItem mmo = new VolatileMMOItem(item);
        if (mmo.hasData(ItemStats.SOULBOUND)) return;

        final Player player = playerData.getPlayer();

        // 禁止绑定成组物品（静默返回，不发送提示）
        if (item.getItem().getAmount() > 1) {
            return;
        }

        // 触发事件（可被取消）
        ApplySoulboundEvent called = new ApplySoulboundEvent(playerData, new VolatileMMOItem(item), item);
        Bukkit.getPluginManager().callEvent(called);
        if (called.isCancelled()) return;

        // 计算灵魂绑定等级：优先物品 SOULBOUND_LEVEL，否则回退到全局配置，最终至少为 1
        int configuredDefault = MMOItems.plugin.getLanguage().autoBindDefaultLevel;
        int levelFromItem = (int) Math.floor(item.getStat("SOULBOUND_LEVEL"));
        int level = levelFromItem > 0 ? levelFromItem : Math.max(1, configuredDefault);

        // 写入绑定数据并更新显示
        MMOItem live = new LiveMMOItem(item);
        live.setData(ItemStats.SOULBOUND, new SoulboundData(player.getUniqueId(), player.getName(), level));
        // 绑定成功后将自动绑定标记显式置为 false，避免后续仍为 true
        live.setData(ItemStats.AUTO_BIND_ON_USE, new BooleanData(false));
        // 为避免未初始化历史的可合并属性（如附魔）在重构时丢失，这里强制对所有 Mergeable 统计进行一次历史重算
        for (ItemStat<?, ?> stat : live.getStats()) {
            if (stat instanceof Mergeable) {
                // computeStatHistory 会在没有历史时基于当前数据创建历史，然后按升级等规则重算
                live.setData(stat, live.computeStatHistory(stat).recalculate(live.getUpgradeLevel()));
            }
        }
        item.getItem().setItemMeta(live.newBuilder().build().getItemMeta());

        // 发送可配置的成功绑定消息
        Message.SUCCESSFULLY_BIND_ITEM
                .format(ChatColor.YELLOW,
                        "#item#", MMOUtils.getDisplayName(item.getItem()),
                        "#level#", MMOUtils.intToRoman(level))
                .send(player);

        // 反馈（仅保留音效，不发送聊天提示）
        player.playSound(player.getLocation(), Sounds.ENTITY_PLAYER_LEVELUP, 1, 2);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void meleeAttacks(PlayerAttackEvent event) {

        // Make sure it's a melee attack
        if (!(event.getAttack() instanceof MeleeAttackMetadata)) return;

        final Player player = event.getPlayer();
        final ItemStack weaponUsed = player.getInventory().getItem(((MeleeAttackMetadata) event.getAttack()).getHand().toBukkit());
        final NBTItem item = MythicLib.plugin.getVersion().getWrapper().getNBTItem(weaponUsed);
        final Type itemType = Type.get(item);
        if (itemType == null || itemType == Type.BLOCK) return;

        // Prevent melee attacks with non-melee weapons
        if (!itemType.hasMeleeAttacks()) {
            event.setCancelled(true);
            return;
        }

        // Check item requirements
        final PlayerData playerData = PlayerData.get(player);
        final Weapon weapon = new Weapon(playerData, item);
        if (!weapon.checkItemRequirements()) {
            event.setCancelled(true);
            return;
        }

        // 近战命中前尝试自动绑定
        tryAutoBindOnUse(playerData, item);

        // Apply melee attack
        if (!weapon.handleTargetedAttack(event.getAttack(), event.getAttacker(), event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void specialToolAbilities(BlockBreakEvent event) {
        if (UtilityMethods.isFake(event)) return;

        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        NBTItem item = MythicLib.plugin.getVersion().getWrapper().getNBTItem(player.getInventory().getItemInMainHand());
        if (!item.hasType()) return;

        Tool tool = new Tool(PlayerData.get(player), item);
        if (!tool.checkItemRequirements()) {
            event.setCancelled(true);
            return;
        }

        // 工具挖掘触发时尝试自动绑定
        tryAutoBindOnUse(PlayerData.get(player), item);

        if (tool.miningEffects(block)) event.setCancelled(true);
    }

    @EventHandler
    public void rightClickWeaponInteractions(PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        if (!(event.getRightClicked() instanceof LivingEntity)) return;

        final NBTItem item = MythicLib.plugin.getVersion().getWrapper().getNBTItem(player.getInventory().getItem(event.getHand()));
        final Type itemType = Type.get(item);
        if (itemType == null) return;

        final LivingEntity target = (LivingEntity) event.getRightClicked();
        if (!UtilityMethods.canTarget(player, target, InteractionType.OFFENSE_ACTION)) return;

        /*
         * Checks for usability
         *
         * This is actually a silent check, because Spigot always calls PlayerInteractEvent at the same
         * time. If the item is not usable, this event will already send a message. Fixes MMOItems#1680
         */
        final UseItem usableItem = itemType.toUseItem(player, item);
        if (!usableItem.checkItemRequirements(false)) return;

        // Apply type-specific entity interactions
        // 在对实体交互前尝试自动绑定
        tryAutoBindOnUse(usableItem.getPlayerData(), item);
        final SkillHandler<?> onEntityInteract = usableItem.getMMOItem().getType().onEntityInteract();
        if (onEntityInteract != null) {
            SpecialWeaponAttackEvent called = new SpecialWeaponAttackEvent(usableItem.getPlayerData(), (Weapon) usableItem, target);
            Bukkit.getPluginManager().callEvent(called);
            if (!called.isCancelled())
                new SimpleSkill(onEntityInteract).cast(new TriggerMetadata(usableItem.getPlayerData().getMMOPlayerData(), TriggerType.API, target));
        }
    }

    // TODO: Rewrite this with a custom 'ApplyMMOItemEvent'?
    @EventHandler
    public void gemStonesAndItemStacks(InventoryClickEvent event) {
        final Player player = (Player) event.getWhoClicked();
        // 仅允许在玩家背包底部栏交换
        Inventory bottom = player.getInventory();
        if (event.getAction() != InventoryAction.SWAP_WITH_CURSOR) return;
        if (event.getClickedInventory() == null || event.getClickedInventory() != bottom) {
            event.setCancelled(true); // 非底部背包拒绝操作
            return;
        }
        final NBTItem item = MythicLib.plugin.getVersion().getWrapper().getNBTItem(event.getCursor());
        final Type type = Type.get(item);
        if (type == null) return;

        final UseItem useItem = type.toUseItem(player, item);
        if (!useItem.checkItemRequirements()) return;

        if (useItem instanceof ItemSkin) {
            NBTItem picked = MythicLib.plugin.getVersion().getWrapper().getNBTItem(event.getCurrentItem());
            if (!picked.hasType()) return;

            ItemSkin.ApplyResult result = ((ItemSkin) useItem).applyOntoItem(picked, Type.get(picked.getType()));
            if (result.getType() == ItemSkin.ResultType.NONE) return;

            event.setCancelled(true);
            item.getItem().setAmount(item.getItem().getAmount() - 1);

            if (result.getType() == ItemSkin.ResultType.FAILURE) return;

            event.setCurrentItem(result.getResult());
        }

        if (useItem instanceof GemStone) {
            NBTItem picked = MythicLib.plugin.getVersion().getWrapper().getNBTItem(event.getCurrentItem());
            if (!picked.hasType()) return;

            GemStone.ApplyResult result = ((GemStone) useItem).applyOntoItem(picked, Type.get(picked.getType()));
            if (result.getType() == GemStone.ResultType.NONE) return;

            event.setCancelled(true);
            item.getItem().setAmount(item.getItem().getAmount() - 1);

            if (result.getType() == GemStone.ResultType.FAILURE) return;

            event.setCurrentItem(result.getResult());
        }

        if (useItem instanceof Consumable && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR)
            if (((Consumable) useItem).useOnItem(event, MythicLib.plugin.getVersion().getWrapper().getNBTItem(event.getCurrentItem()))) {
                event.setCancelled(true);
                event.getCursor().setAmount(event.getCursor().getAmount() - 1);
            }
    }

    /**
     * This handler registers arrows from custom MMOItems bows
     */
    @EventHandler
    public void handleCustomBows(EntityShootBowEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow) || !(event.getEntity() instanceof Player)) return;

        final NBTItem item = NBTItem.get(event.getBow());
        final Type type = Type.get(item.getType());

        if (type != null) {
            final PlayerData playerData = PlayerData.get((Player) event.getEntity());
            final Weapon weapon = new Weapon(playerData, item);
            if (!weapon.checkItemRequirements() || !weapon.checkAndApplyWeaponCosts()) {
                event.setCancelled(true);
                return;
            }

            // 允许射箭后尝试自动绑定
            tryAutoBindOnUse(playerData, item);

            EquipmentSlot bowSlot = EquipmentSlot.fromBukkit(MMOUtils.getHand(event, playerData.getPlayer()));
            final ProjectileMetadata proj = ProjectileMetadata.create(playerData.getMMOPlayerData(), bowSlot, ProjectileType.ARROW, event.getProjectile());
            proj.setSourceItem(item);
            proj.setCustomDamage(true);
            proj.setDamageMultiplier(MMOUtils.getForce(event));
            if (item.hasTag("MMOITEMS_ARROW_PARTICLES"))
                new ArrowParticles((AbstractArrow) event.getProjectile(), item);
            final AbstractArrow arrow = (AbstractArrow) event.getProjectile();

            // Apply arrow velocity
            final double arrowVelocity = proj.getShooter().getStat("ARROW_VELOCITY");
            if (arrowVelocity > 0) arrow.setVelocity(arrow.getVelocity().multiply(arrowVelocity));
        }
    }

    /**
     * Consumables which can be eaten using the
     * vanilla eating animation are handled here.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void handleVanillaEatenConsumables(PlayerItemConsumeEvent event) {
        final NBTItem item = MythicLib.plugin.getVersion().getWrapper().getNBTItem(event.getItem());
        final Type itemType = Type.get(item);
        if (itemType == null) return;

        Player player = event.getPlayer();
        UseItem useItem = itemType.toUseItem(player, item);
        if (!useItem.checkItemRequirements()) {
            event.setCancelled(true);
            return;
        }

        if (useItem instanceof Consumable) {

            if (useItem.getPlayerData().getMMOPlayerData().getCooldownMap().isOnCooldown(useItem.getMMOItem())) {
                final double cd = useItem.getPlayerData().getMMOPlayerData().getCooldownMap().getCooldown(useItem.getMMOItem());
                Message.ITEM_ON_COOLDOWN.format(ChatColor.RED, "#left#", MythicLib.plugin.getMMOConfig().decimal.format(cd), "#s#", cd >= 2 ? "s" : "").send(player);
                event.setCancelled(true);
                return;
            }

            // 冷却检查通过后尝试自动绑定
            tryAutoBindOnUse(useItem.getPlayerData(), item);
 
            org.bukkit.inventory.EquipmentSlot consumeSlot = MMOUtils.getHand(event);
            Consumable.ConsumableConsumeResult result = ((Consumable) useItem).useOnPlayer(consumeSlot, true);

            // No effects are applied and not consumed
            if (result == Consumable.ConsumableConsumeResult.CANCEL) {
                event.setCancelled(true);
                return;
            }

            // Item is not consumed but its effects are applied anyways
            if (result == Consumable.ConsumableConsumeResult.NOT_CONSUME) event.setCancelled(true);

            useItem.getPlayerData().getMMOPlayerData().getCooldownMap().applyCooldown(useItem.getMMOItem(), useItem.getNBTItem().getStat("ITEM_COOLDOWN"));
            useItem.executeCommands();
        }
    }

    /**
     * 无目标武器使用事件：如法杖/拳套等左键或无目标技能释放时触发。
     * 在事件未被取消的情况下，尝试对当前手中武器执行自动绑定。
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onUntargetedWeaponUse(@NotNull UntargetedWeaponUseEvent event) {
        final Weapon weapon = event.getWeapon();
        final PlayerData playerData = event.getPlayerData();
        final NBTItem item = weapon.getNBTItem();
        // 无提示，仅尝试绑定
        tryAutoBindOnUse(playerData, item);
    }
}
