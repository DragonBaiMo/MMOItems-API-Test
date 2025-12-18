package net.Indyuce.mmoitems.api.upgrade.limit;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.manager.Reloadable;
import io.lumine.mythic.lib.MythicLib;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
     * 权限分档配置列表（按优先级排序）
     */
    private final List<DailyLimitTier> limitTiers = new ArrayList<>();

    /**
     * 持久化文件
     */
    private File storageFile;

    /**
     * 持久化配置
     */
    private YamlConfiguration storageConfig;

    /**
     * 是否启用持久化（配置开关）
     */
    private boolean persistEnabled;

    /**
     * 是否启用 MySQL 持久化
     */
    private boolean dbEnabled;
    private DailyLimitSqlStorage sqlStorage;

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
            this.persistEnabled = true;
            storageFile = new File(MMOItems.plugin.getDataFolder(), "upgrade-daily-limit.yml");
            loadStorage();
            return;
        }

        this.enabled = config.getBoolean("enabled", false);
        this.defaultMax = config.getInt("default-max", 50);
        this.resetHour = config.getInt("reset-hour", 0);
        this.bypassPermission = config.getString("bypass-permission", "mmoitems.upgrade.bypass-daily");
        this.persistEnabled = config.getBoolean("persist-enabled", true);
        this.dbEnabled = false;

        // 解析权限分档配置
        limitTiers.clear();
        ConfigurationSection tiersSection = config.getConfigurationSection("tiers");
        if (tiersSection != null) {
            for (String tierId : tiersSection.getKeys(false)) {
                ConfigurationSection tierConfig = tiersSection.getConfigurationSection(tierId);
                if (tierConfig == null) continue;

                String permission = tierConfig.getString("permission", "");
                int max = tierConfig.getInt("max", defaultMax);
                int priority = tierConfig.getInt("priority", 0);

                limitTiers.add(new DailyLimitTier(tierId, permission, max, priority));
            }
            // 按优先级降序排列（高优先级在前）
            Collections.sort(limitTiers);
        }

        // 关闭旧数据源
        if (sqlStorage != null) {
            sqlStorage.close();
            sqlStorage = null;
        }

        // 数据库配置（跨服共享）
        ConfigurationSection dbSec = MMOItems.plugin.getConfig().getConfigurationSection("database.daily-limit");
        if (dbSec != null && dbSec.getBoolean("enabled", false)) {
            this.dbEnabled = true;
            sqlStorage = buildSqlStorageFromSection(dbSec);
        } else {
            // 尝试使用 MythicLib 的 mysql 配置（跨服共享）
            ConfigurationSection mlMysql = MythicLib.plugin.getConfig().getConfigurationSection("mysql");
            if (MythicLib.plugin.getConfig().getBoolean("mysql.enabled") && mlMysql != null) {
                this.dbEnabled = true;
                sqlStorage = buildSqlStorageFromMythicLib(mlMysql);
            }
        }

        // 清理缓存，重新加载时重置
        cache.clear();

        // 初始化持久化文件
        storageFile = new File(MMOItems.plugin.getDataFolder(), "upgrade-daily-limit.yml");
        loadStorage();
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
        if (dbEnabled && sqlStorage != null) {
            return sqlStorage.loadData(player.getUniqueId(), resetHour);
        }
        return cache.computeIfAbsent(player.getUniqueId(), this::loadFromStorageOrNew);
    }

    /**
     * 获取玩家的每日最大强化次数
     * <p>
     * 按权限分档配置，优先级高的先匹配。
     * 匹配逻辑：
     * <ol>
     *     <li>遍历已排序的分档列表（高优先级在前）</li>
     *     <li>如果分档无权限要求（默认分档）或玩家有对应权限，返回该分档的上限</li>
     *     <li>如果没有匹配的分档，返回默认值</li>
     * </ol>
     * </p>
     *
     * @param player 玩家
     * @return 每日最大次数
     */
    public int getMaxAttempts(@NotNull Player player) {
        // 遍历权限分档（已按优先级降序排列）
        for (DailyLimitTier tier : limitTiers) {
            // 默认分档（无权限要求）或玩家有对应权限
            if (tier.isDefault() || player.hasPermission(tier.getPermission())) {
                return tier.getMaxAttempts();
            }
        }
        // 没有匹配的分档，返回默认值
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
        return data.canUpgrade(getMaxAttempts(player), resetHour);
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

        if (dbEnabled && sqlStorage != null) {
            sqlStorage.increment(player.getUniqueId(), resetHour);
        } else {
            getData(player).incrementUsed(resetHour);
            saveStorage();
        }
    }

    /**
     * 获取玩家今日已使用次数
     *
     * @param player 玩家
     * @return 已使用次数
     */
    public int getUsedAttempts(@NotNull Player player) {
        return getData(player).getUsedAttempts(resetHour);
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

        return getData(player).getRemainingAttempts(getMaxAttempts(player), resetHour);
    }

    /**
     * 手动重置玩家的每日次数
     *
     * @param player 玩家
     */
    public void resetPlayer(@NotNull Player player) {
        if (dbEnabled && sqlStorage != null) {
            sqlStorage.reset(player.getUniqueId(), resetHour);
        } else {
            getData(player).reset(resetHour);
            saveStorage();
        }
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

    /**
     * 持久化：加载存档
     */
    private void loadStorage() {
        if (!persistEnabled || dbEnabled) {
            storageConfig = new YamlConfiguration();
            return;
        }
        storageConfig = new YamlConfiguration();
        if (storageFile.exists()) {
            try {
                storageConfig.load(storageFile);
            } catch (Exception ignored) {
                storageConfig = new YamlConfiguration();
            }
        }
    }

    /**
     * 持久化：保存当前缓存到文件
     */
    private synchronized void saveStorage() {
        if (!persistEnabled || dbEnabled) {
            return;
        }
        if (storageConfig == null) {
            storageConfig = new YamlConfiguration();
        }
        ConfigurationSection playersSec = storageConfig.getConfigurationSection("players");
        if (playersSec == null) {
            playersSec = storageConfig.createSection("players");
        }

        for (Map.Entry<UUID, DailyLimitData> entry : cache.entrySet()) {
            UUID uuid = entry.getKey();
            DailyLimitData data = entry.getValue();
            String path = "players." + uuid;
            playersSec.set(path + ".used", data.getUsedAttemptsRaw());
            playersSec.set(path + ".last-reset", data.getLastResetEpochMillis());
        }

        try {
            storageConfig.save(storageFile);
        } catch (IOException ignored) {
        }
    }

    /**
     * 从存档或默认创建 DailyLimitData
     */
    private DailyLimitData loadFromStorageOrNew(@NotNull UUID uuid) {
        if (storageConfig == null) {
            loadStorage();
        }
        ConfigurationSection playersSec = storageConfig.getConfigurationSection("players");
        if (playersSec != null) {
            ConfigurationSection node = playersSec.getConfigurationSection(uuid.toString());
            if (node != null) {
                int used = node.getInt("used", 0);
                long lastReset = node.getLong("last-reset", System.currentTimeMillis());
                DailyLimitData data = new DailyLimitData(uuid, used, lastReset);
                // 在获取时检查是否跨窗口，避免旧数据过期
                data.getUsedAttempts(resetHour);
                return data;
            }
        }
        return new DailyLimitData(uuid);
    }

    private DailyLimitSqlStorage buildSqlStorageFromSection(ConfigurationSection dbSec) {
        String host = dbSec.getString("host", "127.0.0.1");
        int port = dbSec.getInt("port", 3306);
        String database = dbSec.getString("database", "mmoitems");
        String user = dbSec.getString("user", "root");
        String password = dbSec.getString("password", "");
        String table = dbSec.getString("table", "mmoitems_daily_limit");
        int poolSize = dbSec.getInt("pool-size", 5);
        String params = dbSec.getString("params", "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?" + params;
        return new DailyLimitSqlStorage(jdbcUrl, user, password, table, poolSize);
    }

    private DailyLimitSqlStorage buildSqlStorageFromMythicLib(ConfigurationSection mlMysql) {
        String host = mlMysql.getString("host", "127.0.0.1");
        int port = mlMysql.getInt("port", 3306);
        String database = mlMysql.getString("database", "minecraft");
        String user = mlMysql.getString("user", "root");
        String password = mlMysql.getString("pass", "");
        int poolSize = mlMysql.getInt("maxPoolSize", 10);
        String extraParams = "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?" + extraParams;
        // 复用 MMOItems 默认表名
        String table = "mmoitems_daily_limit";
        DailyLimitSqlStorage storage = new DailyLimitSqlStorage(jdbcUrl, user, password, table, poolSize);
        return storage;
    }
}
