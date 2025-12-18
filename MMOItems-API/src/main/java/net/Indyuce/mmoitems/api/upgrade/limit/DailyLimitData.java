package net.Indyuce.mmoitems.api.upgrade.limit;

import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 玩家每日强化限制数据（支持指定小时重置 + 持久化时间戳）
 */
public class DailyLimitData {

    private final UUID playerUuid;
    private int usedAttempts;
    private long lastResetEpochMillis;

    /**
     * 创建新的每日限制数据（默认从当前窗口开始计数）
     */
    public DailyLimitData(@NotNull UUID playerUuid) {
        this(playerUuid, 0, System.currentTimeMillis());
    }

    /**
     * 从持久化数据恢复
     */
    public DailyLimitData(@NotNull UUID playerUuid, int usedAttempts, long lastResetEpochMillis) {
        this.playerUuid = playerUuid;
        this.usedAttempts = usedAttempts;
        this.lastResetEpochMillis = lastResetEpochMillis;
    }

    @NotNull
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    /**
     * 获取今日已使用次数（会按 resetHour 检查并自动重置）
     */
    public int getUsedAttempts(int resetHour) {
        checkAndResetIfNeeded(resetHour);
        return usedAttempts;
    }

    /**
     * 原始已用次数（不触发重置），用于持久化。
     */
    public int getUsedAttemptsRaw() {
        return usedAttempts;
    }

    /**
     * 上次重置时间戳（毫秒）
     */
    public long getLastResetEpochMillis() {
        return lastResetEpochMillis;
    }

    /**
     * 增加使用次数
     */
    public void incrementUsed(int resetHour) {
        checkAndResetIfNeeded(resetHour);
        this.usedAttempts++;
    }

    /**
     * 设置使用次数
     */
    public void setUsedAttempts(int attempts, int resetHour) {
        checkAndResetIfNeeded(resetHour);
        this.usedAttempts = Math.max(0, attempts);
    }

    /**
     * 手动重置到当前窗口
     */
    public void reset(int resetHour) {
        this.usedAttempts = 0;
        this.lastResetEpochMillis = currentWindowStart(resetHour);
    }

    /**
     * 是否还有剩余次数
     */
    public boolean canUpgrade(int maxAttempts, int resetHour) {
        return getUsedAttempts(resetHour) < maxAttempts;
    }

    /**
     * 获取剩余次数
     */
    public int getRemainingAttempts(int maxAttempts, int resetHour) {
        return Math.max(0, maxAttempts - getUsedAttempts(resetHour));
    }

    /**
     * 距离下次重置的秒数
     */
    public long getSecondsUntilReset(int resetHour) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime nextReset = computeWindowStart(now, resetHour).plusDays(1);
        return nextReset.toEpochSecond() - now.toEpochSecond();
    }

    @Override
    public String toString() {
        return "DailyLimitData{" +
                "playerUuid=" + playerUuid +
                ", usedAttempts=" + usedAttempts +
                ", lastResetEpochMillis=" + lastResetEpochMillis +
                '}';
    }

    private void checkAndResetIfNeeded(int resetHour) {
        long windowStart = currentWindowStart(resetHour);
        if (lastResetEpochMillis < windowStart) {
            usedAttempts = 0;
            lastResetEpochMillis = windowStart;
        }
    }

    /**
     * 当前重置窗口起点（毫秒）
     */
    private long currentWindowStart(int resetHour) {
        return computeWindowStart(ZonedDateTime.now(), resetHour).toInstant().toEpochMilli();
    }

    /**
     * 计算给定时间点所在的窗口起点：
     * 若当前时刻早于当日 resetHour，则窗口起点为前一日的 resetHour，否则为当日 resetHour。
     */
    private ZonedDateTime computeWindowStart(ZonedDateTime now, int resetHour) {
        ZonedDateTime start = now.withHour(resetHour).withMinute(0).withSecond(0).withNano(0);
        if (now.getHour() < resetHour) {
            start = start.minusDays(1);
        }
        return start;
    }
}
