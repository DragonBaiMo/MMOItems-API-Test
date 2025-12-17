package net.Indyuce.mmoitems.api.upgrade.limit;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 玩家每日强化限制数据
 * <p>
 * 存储玩家当天的强化使用次数，每日自动重置。
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public class DailyLimitData {

    private final UUID playerUuid;
    private int usedAttempts;
    private LocalDate lastResetDate;

    /**
     * 创建新的每日限制数据
     *
     * @param playerUuid 玩家 UUID
     */
    public DailyLimitData(@NotNull UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.usedAttempts = 0;
        this.lastResetDate = LocalDate.now();
    }

    /**
     * 从已有数据恢复
     *
     * @param playerUuid   玩家 UUID
     * @param usedAttempts 已使用次数
     * @param resetDate    上次重置日期
     */
    public DailyLimitData(@NotNull UUID playerUuid, int usedAttempts, @NotNull LocalDate resetDate) {
        this.playerUuid = playerUuid;
        this.usedAttempts = usedAttempts;
        this.lastResetDate = resetDate;
    }

    /**
     * 获取玩家 UUID
     *
     * @return 玩家 UUID
     */
    @NotNull
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    /**
     * 获取今日已使用的强化次数
     * <p>
     * 如果日期已变更，会自动重置计数
     * </p>
     *
     * @return 已使用次数
     */
    public int getUsedAttempts() {
        checkAndResetIfNeeded();
        return usedAttempts;
    }

    /**
     * 获取上次重置日期
     *
     * @return 上次重置的日期
     */
    @NotNull
    public LocalDate getLastResetDate() {
        return lastResetDate;
    }

    /**
     * 增加使用次数
     */
    public void incrementUsed() {
        checkAndResetIfNeeded();
        this.usedAttempts++;
    }

    /**
     * 设置使用次数
     *
     * @param attempts 使用次数
     */
    public void setUsedAttempts(int attempts) {
        this.usedAttempts = Math.max(0, attempts);
    }

    /**
     * 手动重置今日次数
     */
    public void reset() {
        this.usedAttempts = 0;
        this.lastResetDate = LocalDate.now();
    }

    /**
     * 检查是否需要自动重置（跨天）
     */
    private void checkAndResetIfNeeded() {
        LocalDate today = LocalDate.now();
        if (!today.equals(lastResetDate)) {
            reset();
        }
    }

    /**
     * 检查是否可以继续强化
     *
     * @param maxAttempts 每日最大次数
     * @return 如果还有剩余次数返回 true
     */
    public boolean canUpgrade(int maxAttempts) {
        return getUsedAttempts() < maxAttempts;
    }

    /**
     * 获取剩余可用次数
     *
     * @param maxAttempts 每日最大次数
     * @return 剩余次数
     */
    public int getRemainingAttempts(int maxAttempts) {
        return Math.max(0, maxAttempts - getUsedAttempts());
    }

    /**
     * 获取距离下次重置的秒数
     *
     * @param resetHour 重置时间（小时，0-23）
     * @return 距离下次重置的秒数
     */
    public long getSecondsUntilReset(int resetHour) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime nextReset = now.toLocalDate()
                .atTime(resetHour, 0)
                .atZone(ZoneId.systemDefault());

        // 如果今天的重置时间已过，计算到明天的重置时间
        if (now.isAfter(nextReset)) {
            nextReset = nextReset.plusDays(1);
        }

        return nextReset.toEpochSecond() - now.toEpochSecond();
    }

    @Override
    public String toString() {
        return "DailyLimitData{" +
                "playerUuid=" + playerUuid +
                ", usedAttempts=" + usedAttempts +
                ", lastResetDate=" + lastResetDate +
                '}';
    }
}
