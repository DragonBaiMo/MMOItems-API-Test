package net.Indyuce.mmoitems.api.upgrade.limit;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.manager.Reloadable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每日强化次数限制管理器
 * <p>
 * 管理玩家每日强化次数限制功能。主要特性：
 * <ul>
 *     <li>每日自动重置强化次数</li>
 *     <li>支持权限绕过限制</li>
 *     <li>支持按权限配置不同上限</li>
 * </ul>
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public class DailyLimitManager implements Reloadable {

    /**
     * 玩家每日限制数据缓存
     */
    private final Map<UUID, DailyLimitData> cache = new ConcurrentHashMap<>();

    /**
     * 是否启用每日限制
     */
    private boolean enabled;

    /**
     * 默认每日最大强化次数
     */
    private int defaultMax;

    /**
     * 每日重置时间（小时，0-23）
     */
    private int resetHour;

    /**
     * 绕过限制的权限节点
     */
    private String bypassPermission;

    /**
     * 创建每日限制管理器并加载配置
     */
    public DailyLimitManager() {
        reload();
    }

    @Override
    public void reload() {
        ConfigurationSection config = MMOItems.plugin.getConfig().getConfigurationSection("item-upgrading.daily-limit");

        if (config == null) {
            // 默认配置
            this.enabled = false;
            this.defaultMax = 50;
            this.resetHour = 0;
            this.bypassPermission = "mmoitems.upgrade.bypass-daily";
            return;
        }

        this.enabled = config.getBoolean("enabled", false);
        this.defaultMax = config.getInt("default-max", 50);
        this.resetHour = config.getInt("reset-hour", 0);
        this.bypassPermission = config.getString("bypass-permission", "mmoitems.upgrade.bypass-daily");

        // 清理缓存，重新加载时重置
        cache.clear();
    }

    /**
     * 检查每日限制是否启用
     *
     * @return 如果启用返回 true
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取默认每日最大次数
     *
     * @return 默认最大次数
     */
    public int getDefaultMax() {
        return defaultMax;
    }

    /**
     * 获取每日重置时间
     *
     * @return 重置时间（小时）
     */
    public int getResetHour() {
        return resetHour;
    }

    /**
     * 获取绕过权限节点
     *
     * @return 权限节点字符串
     */
    public String getBypassPermission() {
        return bypassPermission;
    }

    /**
     * 获取玩家的每日限制数据
     * <p>
     * 如果缓存中不存在，会创建新的数据
     * </p>
     *
     * @param player 玩家
     * @return 每日限制数据
     */
    @NotNull
    public DailyLimitData getData(@NotNull Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), DailyLimitData::new);
    }

    /**
     * 获取玩家的每日最大强化次数
     * <p>
     * 可以通过权限配置不同的上限（未来扩展）
     * </p>
     *
     * @param player 玩家
     * @return 每日最大次数
     */
    public int getMaxAttempts(@NotNull Player player) {
        // TODO: 未来可以添加按权限配置不同上限
        return defaultMax;
    }

    /**
     * 检查玩家是否可以继续强化
     * <p>
     * 检查顺序：
     * <ol>
     *     <li>每日限制是否启用</li>
     *     <li>玩家是否有绕过权限</li>
     *     <li>是否还有剩余次数</li>
     * </ol>
     * </p>
     *
     * @param player 玩家
     * @return 如果可以强化返回 true
     */
    public boolean canUpgrade(@NotNull Player player) {
        // 功能未启用
        if (!enabled) {
            return true;
        }

        // 检查绕过权限
        if (player.hasPermission(bypassPermission)) {
            return true;
        }

        // 检查剩余次数
        DailyLimitData data = getData(player);
        return data.canUpgrade(getMaxAttempts(player));
    }

    /**
     * 记录一次强化操作
     *
     * @param player 玩家
     */
    public void recordAttempt(@NotNull Player player) {
        if (!enabled) {
            return;
        }

        // 有绕过权限的玩家不计数
        if (player.hasPermission(bypassPermission)) {
            return;
        }

        getData(player).incrementUsed();
    }

    /**
     * 获取玩家今日已使用次数
     *
     * @param player 玩家
     * @return 已使用次数
     */
    public int getUsedAttempts(@NotNull Player player) {
        return getData(player).getUsedAttempts();
    }

    /**
     * 获取玩家今日剩余次数
     *
     * @param player 玩家
     * @return 剩余次数
     */
    public int getRemainingAttempts(@NotNull Player player) {
        if (!enabled) {
            return Integer.MAX_VALUE;
        }

        if (player.hasPermission(bypassPermission)) {
            return Integer.MAX_VALUE;
        }

        return getData(player).getRemainingAttempts(getMaxAttempts(player));
    }

    /**
     * 手动重置玩家的每日次数
     *
     * @param player 玩家
     */
    public void resetPlayer(@NotNull Player player) {
        getData(player).reset();
    }

    /**
     * 清除玩家缓存（玩家离线时调用）
     *
     * @param playerUuid 玩家 UUID
     */
    public void clearCache(@NotNull UUID playerUuid) {
        cache.remove(playerUuid);
    }

    /**
     * 获取距离下次重置的秒数
     *
     * @param player 玩家
     * @return 距离下次重置的秒数
     */
    public long getSecondsUntilReset(@NotNull Player player) {
        return getData(player).getSecondsUntilReset(resetHour);
    }
}
