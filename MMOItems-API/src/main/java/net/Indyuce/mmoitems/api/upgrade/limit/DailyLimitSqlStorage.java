package net.Indyuce.mmoitems.api.upgrade.limit;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * MySQL 持久化实现（跨服共享，依赖数据库强一致）
 */
class DailyLimitSqlStorage {

    private final HikariDataSource dataSource;
    private final String table;

    DailyLimitSqlStorage(@NotNull String jdbcUrl,
                         @NotNull String user,
                         @NotNull String password,
                         @NotNull String table,
                         int poolSize) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(Math.max(2, poolSize));
        cfg.setPoolName("MMOItems-DailyLimit");
        cfg.setConnectionTimeout(10000);
        cfg.setLeakDetectionThreshold(60000);
        cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
        this.dataSource = new HikariDataSource(cfg);
        this.table = table;
        initTable();
    }

    void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private void initTable() {
        String ddl = "CREATE TABLE IF NOT EXISTS `" + table + "` (" +
                "`uuid` CHAR(36) NOT NULL," +
                "`used` INT NOT NULL," +
                "`last_reset` BIGINT NOT NULL," +
                "PRIMARY KEY (`uuid`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate(ddl);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[MMOItems] 无法初始化每日限制数据表：" + e.getMessage());
        }
    }

    /**
     * 加载并根据 resetHour 校验窗口（必要时自动重置），如果不存在则创建默认记录。
     */
    @NotNull
    DailyLimitData loadData(@NotNull UUID uuid, int resetHour) {
        long windowStart = currentWindowStart(resetHour);
        try (Connection conn = dataSource.getConnection()) {
            String select = "SELECT used, last_reset FROM `" + table + "` WHERE uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(select)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int used = rs.getInt("used");
                        long lastReset = rs.getLong("last_reset");
                        DailyLimitData data = new DailyLimitData(uuid, used, lastReset);
                        data.getUsedAttempts(resetHour); // 触发窗口校验
                        // 如窗口更新则写回
                        if (data.getLastResetEpochMillis() != lastReset || data.getUsedAttemptsRaw() != used) {
                            upsert(conn, uuid, data.getUsedAttemptsRaw(), data.getLastResetEpochMillis(), windowStart);
                        }
                        return data;
                    }
                }
            }
            // 不存在则创建
            DailyLimitData data = new DailyLimitData(uuid, 0, windowStart);
            upsert(conn, uuid, 0, windowStart, windowStart);
            return data;
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[MMOItems] 读取每日限制失败：" + e.getMessage());
            return new DailyLimitData(uuid, 0, windowStart);
        }
    }

    /**
     * 原子递增并根据窗口重置。
     */
    void increment(@NotNull UUID uuid, int resetHour) {
        long windowStart = currentWindowStart(resetHour);
        String sql = "INSERT INTO `" + table + "` (uuid, used, last_reset) VALUES (?, 1, ?)" +
                " ON DUPLICATE KEY UPDATE " +
                " used = IF(last_reset < VALUES(last_reset), 1, used + 1)," +
                " last_reset = IF(last_reset < VALUES(last_reset), VALUES(last_reset), last_reset)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, windowStart);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[MMOItems] 更新每日限制失败：" + e.getMessage());
        }
    }

    /**
     * 重置当前窗口。
     */
    void reset(@NotNull UUID uuid, int resetHour) {
        long windowStart = currentWindowStart(resetHour);
        String sql = "INSERT INTO `" + table + "` (uuid, used, last_reset) VALUES (?, 0, ?)" +
                " ON DUPLICATE KEY UPDATE used = 0, last_reset = VALUES(last_reset)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, windowStart);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[MMOItems] 重置每日限制失败：" + e.getMessage());
        }
    }

    private void upsert(Connection conn, UUID uuid, int used, long lastReset, long windowStart) throws SQLException {
        String sql = "INSERT INTO `" + table + "` (uuid, used, last_reset) VALUES (?, ?, ?)" +
                " ON DUPLICATE KEY UPDATE used = ?, last_reset = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, used);
            ps.setLong(3, lastReset);
            ps.setInt(4, used);
            ps.setLong(5, lastReset < windowStart ? windowStart : lastReset);
            ps.executeUpdate();
        }
    }

    private long currentWindowStart(int resetHour) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime start = now.withHour(resetHour).withMinute(0).withSecond(0).withNano(0);
        if (now.getHour() < resetHour) {
            start = start.minusDays(1);
        }
        return start.toInstant().toEpochMilli();
    }
}
