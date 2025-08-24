package net.Indyuce.mmoitems.util;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.event.item.ApplySoulboundEvent;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.VolatileMMOItem;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.stat.data.BooleanData;
import net.Indyuce.mmoitems.stat.data.SoulboundData;
import net.Indyuce.mmoitems.stat.data.type.Mergeable;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import net.Indyuce.mmoitems.util.MMOUtils;
import io.lumine.mythic.lib.version.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 自动绑定工具：在“使用”或“放入饰品槽”等场景下，为具有 AUTO_BIND_ON_USE 的 MMOItems 物品执行一次性灵魂绑定。
 */
public final class AutoBindUtil {
    private AutoBindUtil() {}

    public static boolean applyAutoBindIfNeeded(@NotNull PlayerData playerData, @Nullable ItemStack stack) {
        if (stack == null) return false;
        return applyAutoBindIfNeeded(playerData, NBTItem.get(stack), null);
    }

    /**
     * 新增：支持传入需要更新的 Bukkit 槽位，仅在实际发生绑定时写回对应槽位，减少无效 setItem 调用。
     */
    public static boolean applyAutoBindIfNeeded(@NotNull PlayerData playerData, @Nullable ItemStack stack, @Nullable EquipmentSlot slotToUpdate) {
        if (stack == null) return false;
        return applyAutoBindIfNeeded(playerData, NBTItem.get(stack), slotToUpdate);
    }

    public static boolean applyAutoBindIfNeeded(@NotNull PlayerData playerData, @NotNull NBTItem item) {
        return applyAutoBindIfNeeded(playerData, item, null);
    }

    /**
     * 新增：带槽位的绑定方法。
     * 当且仅当发生绑定成功时，尝试将更新后的物品写回指定槽位（当前仅支持主手/副手）。
     */
    public static boolean applyAutoBindIfNeeded(@NotNull PlayerData playerData, @NotNull NBTItem item, @Nullable EquipmentSlot slotToUpdate) {
        // 必须是 MMOItems 物品
        if (Type.get(item) == null) return false;

        // 无该开关或已绑定则跳过
        if (!item.getBoolean("MMOITEMS_AUTO_BIND_ON_USE")) return false;
        final MMOItem mmo = new VolatileMMOItem(item);
        if (mmo.hasData(ItemStats.SOULBOUND)) return false;

        final Player player = playerData.getPlayer();

        // 禁止绑定成组物品（静默返回）
        if (item.getItem().getAmount() > 1) return false;

        // 触发事件（可被取消）
        ApplySoulboundEvent called = new ApplySoulboundEvent(playerData, new VolatileMMOItem(item), item);
        Bukkit.getPluginManager().callEvent(called);
        if (called.isCancelled()) return false;

        // 计算灵魂绑定等级：优先物品 SOULBOUND_LEVEL，否则回退到全局配置，最终至少为 1
        int configuredDefault = MMOItems.plugin.getLanguage().autoBindDefaultLevel;
        int levelFromItem = (int) Math.floor(item.getStat("SOULBOUND_LEVEL"));
        int level = levelFromItem > 0 ? levelFromItem : Math.max(1, configuredDefault);

        // 写入绑定数据并更新显示
        MMOItem live = new LiveMMOItem(item);
        live.setData(ItemStats.SOULBOUND, new SoulboundData(player.getUniqueId(), player.getName(), level));
        // 绑定成功后将自动绑定标记显式置为 false，避免后续仍为 true
        live.setData(ItemStats.AUTO_BIND_ON_USE, new BooleanData(false));
        // 强制对所有 Mergeable 统计进行一次历史重算，避免未初始化导致的丢失
        for (ItemStat<?, ?> stat : live.getStats()) {
            if (stat instanceof Mergeable) {
                live.setData(stat, live.computeStatHistory(stat).recalculate(live.getUpgradeLevel()));
            }
        }
        item.getItem().setItemMeta(live.newBuilder().build().getItemMeta());

        // 槽位写回：仅在绑定成功后进行，且仅对主手/副手写回
        if (slotToUpdate != null) {
            try {
                if (slotToUpdate == EquipmentSlot.HAND) {
                    player.getInventory().setItemInMainHand(item.getItem());
                } else if (slotToUpdate == EquipmentSlot.OFF_HAND) {
                    player.getInventory().setItemInOffHand(item.getItem());
                }
            } catch (Throwable ignored) {
                // 兼容性保护：不因写回失败影响绑定流程
            }
        }

        // 提示与音效（遵循原实现）
        net.Indyuce.mmoitems.api.util.message.Message.SUCCESSFULLY_BIND_ITEM
                .format(ChatColor.YELLOW,
                        "#item#", MMOUtils.getDisplayName(item.getItem()),
                        "#level#", MMOUtils.intToRoman(level))
                .send(player);
        player.playSound(player.getLocation(), Sounds.ENTITY_PLAYER_LEVELUP, 1, 2);
        return true;
    }
}
