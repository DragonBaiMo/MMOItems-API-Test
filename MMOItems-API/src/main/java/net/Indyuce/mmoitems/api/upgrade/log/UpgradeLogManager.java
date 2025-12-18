package net.Indyuce.mmoitems.api.upgrade.log;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.manager.Reloadable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

/**
 * 强化日志管理器
 * <p>
 * 提供强化日志的记录、查询和持久化功能：
 * <ul>
 *     <li>异步写入日志</li>
 *     <li>支持 YAML 存储</li>
 *     <li>支持按玩家/时间查询</li>
 *     <li>自动清理过期日志</li>
 * </ul>
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public class UpgradeLogManager implements Reloadable {

    /**
     * 是否启用日志
     */
    private boolean enabled;

    /**
     * 日志保留天数
     */
    private int retentionDays;

    /**
     * 日志存储目录
     */
    private File logDirectory;

    /**
     * 日期格式化器
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    /**
     * 待写入的日志队列
     */
    private final ConcurrentLinkedQueue<UpgradeLogEntry> pendingLogs = new ConcurrentLinkedQueue<>();

    /**
     * 创建并加载配置
     */
    public UpgradeLogManager() {
        reload();
    }

    @Override
    public void reload() {
        ConfigurationSection config = MMOItems.plugin.getConfig().getConfigurationSection("item-upgrading.upgrade-log");
        if (config == null) {
            this.enabled = false;
            this.retentionDays = 30;
            return;
        }

        this.enabled = config.getBoolean("enabled", false);
        this.retentionDays = config.getInt("retention-days", 30);

        // 初始化日志目录
        logDirectory = new File(MMOItems.plugin.getDataFolder(), "upgrade-logs");
        if (enabled && !logDirectory.exists()) {
            logDirectory.mkdirs();
        }

        // 启动时清理过期日志
        if (enabled) {
            cleanupOldLogs();
        }
    }

    /**
     * 检查是否启用日志
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 记录一条强化日志
     *
     * @param entry 日志条目
     */
    public void log(@NotNull UpgradeLogEntry entry) {
        if (!enabled) return;
        pendingLogs.offer(entry);
        // 异步写入
        flushAsync();
    }

    /**
     * 异步写入待处理的日志
     */
    private void flushAsync() {
        if (pendingLogs.isEmpty()) return;

        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(MMOItems.plugin, () -> {
            UpgradeLogEntry entry;
            Map<String, List<UpgradeLogEntry>> byDate = new HashMap<>();

            while ((entry = pendingLogs.poll()) != null) {
                String date = dateFormat.format(new Date(entry.getTimestamp()));
                byDate.computeIfAbsent(date, k -> new ArrayList<>()).add(entry);
            }

            for (Map.Entry<String, List<UpgradeLogEntry>> dateEntry : byDate.entrySet()) {
                writeToFile(dateEntry.getKey(), dateEntry.getValue());
            }
        });
    }

    /**
     * 写入日志文件
     */
    private void writeToFile(@NotNull String date, @NotNull List<UpgradeLogEntry> entries) {
        File logFile = new File(logDirectory, date + ".yml");
        YamlConfiguration config;

        if (logFile.exists()) {
            config = YamlConfiguration.loadConfiguration(logFile);
        } else {
            config = new YamlConfiguration();
        }

        for (UpgradeLogEntry entry : entries) {
            String path = "logs." + entry.getId();
            config.set(path + ".player-uuid", entry.getPlayerUuid().toString());
            config.set(path + ".player-name", entry.getPlayerName());
            config.set(path + ".item-type", entry.getItemType());
            config.set(path + ".item-id", entry.getItemId());
            config.set(path + ".item-name", entry.getItemName());
            config.set(path + ".level-before", entry.getLevelBefore());
            config.set(path + ".level-after", entry.getLevelAfter());
            config.set(path + ".success", entry.isSuccess());
            config.set(path + ".penalty-type", entry.getPenaltyType());
            config.set(path + ".stones-used", entry.getStonesUsed());
            config.set(path + ".economy-cost", entry.getEconomyCost());
            config.set(path + ".timestamp", entry.getTimestamp());
            config.set(path + ".time", timeFormat.format(new Date(entry.getTimestamp())));
            config.set(path + ".guarantee-triggered", entry.isGuaranteeTriggered());
        }

        try {
            config.save(logFile);
        } catch (IOException e) {
            MMOItems.plugin.getLogger().log(Level.WARNING, "无法保存强化日志文件: " + logFile.getName(), e);
        }
    }

    /**
     * 查询玩家的强化日志
     *
     * @param playerUuid 玩家UUID
     * @param days       查询最近多少天
     * @param limit      最多返回多少条
     * @return 日志列表（按时间倒序）
     */
    @NotNull
    public List<UpgradeLogEntry> queryByPlayer(@NotNull UUID playerUuid, int days, int limit) {
        if (!enabled || !logDirectory.exists()) {
            return Collections.emptyList();
        }

        List<UpgradeLogEntry> results = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < days && results.size() < limit; i++) {
            String date = dateFormat.format(cal.getTime());
            File logFile = new File(logDirectory, date + ".yml");

            if (logFile.exists()) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(logFile);
                ConfigurationSection logs = config.getConfigurationSection("logs");
                if (logs != null) {
                    for (String id : logs.getKeys(false)) {
                        ConfigurationSection entry = logs.getConfigurationSection(id);
                        if (entry == null) continue;

                        String uuid = entry.getString("player-uuid", "");
                        if (uuid.equals(playerUuid.toString())) {
                            results.add(parseEntry(id, entry));
                            if (results.size() >= limit) break;
                        }
                    }
                }
            }

            cal.add(Calendar.DAY_OF_MONTH, -1);
        }

        // 按时间倒序排列
        results.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        return results.subList(0, Math.min(results.size(), limit));
    }

    /**
     * 解析日志条目
     */
    private UpgradeLogEntry parseEntry(@NotNull String id, @NotNull ConfigurationSection section) {
        return new UpgradeLogEntry.Builder()
                .id(id)
                .player(UUID.fromString(section.getString("player-uuid", "")),
                        section.getString("player-name", ""))
                .item(section.getString("item-type", ""),
                        section.getString("item-id", ""),
                        section.getString("item-name", ""))
                .levels(section.getInt("level-before", 0),
                        section.getInt("level-after", 0))
                .success(section.getBoolean("success", false))
                .penalty(section.getString("penalty-type"))
                .stonesUsed(section.getInt("stones-used", 0))
                .economyCost(section.getDouble("economy-cost", 0))
                .timestamp(section.getLong("timestamp", System.currentTimeMillis()))
                .guaranteeTriggered(section.getBoolean("guarantee-triggered", false))
                .build();
    }

    /**
     * 清理过期日志
     */
    private void cleanupOldLogs() {
        if (!logDirectory.exists()) return;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -retentionDays);
        long cutoffTime = cal.getTimeInMillis();

        File[] files = logDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                String datePart = file.getName().replace(".yml", "");
                Date fileDate = dateFormat.parse(datePart);
                if (fileDate.getTime() < cutoffTime) {
                    file.delete();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 强制刷新待写入的日志
     */
    public void flush() {
        if (pendingLogs.isEmpty()) return;

        UpgradeLogEntry entry;
        Map<String, List<UpgradeLogEntry>> byDate = new HashMap<>();

        while ((entry = pendingLogs.poll()) != null) {
            String date = dateFormat.format(new Date(entry.getTimestamp()));
            byDate.computeIfAbsent(date, k -> new ArrayList<>()).add(entry);
        }

        for (Map.Entry<String, List<UpgradeLogEntry>> dateEntry : byDate.entrySet()) {
            writeToFile(dateEntry.getKey(), dateEntry.getValue());
        }
    }
}
