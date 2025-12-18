package net.Indyuce.mmoitems.api.upgrade.log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 强化日志条目
 * <p>
 * 记录一次强化操作的详细信息
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public class UpgradeLogEntry {

    /**
     * 日志ID（唯一标识）
     */
    private final String id;

    /**
     * 玩家UUID
     */
    private final UUID playerUuid;

    /**
     * 玩家名称
     */
    private final String playerName;

    /**
     * 物品类型
     */
    private final String itemType;

    /**
     * 物品ID
     */
    private final String itemId;

    /**
     * 物品名称
     */
    private final String itemName;

    /**
     * 强化前等级
     */
    private final int levelBefore;

    /**
     * 强化后等级
     */
    private final int levelAfter;

    /**
     * 是否成功
     */
    private final boolean success;

    /**
     * 惩罚类型（失败时）
     */
    private final String penaltyType;

    /**
     * 使用的强化石数量
     */
    private final int stonesUsed;

    /**
     * 经济消耗
     */
    private final double economyCost;

    /**
     * 时间戳（毫秒）
     */
    private final long timestamp;

    /**
     * 是否触发保底
     */
    private final boolean guaranteeTriggered;

    /**
     * 创建日志条目
     */
    public UpgradeLogEntry(@NotNull String id,
                           @NotNull UUID playerUuid,
                           @NotNull String playerName,
                           @NotNull String itemType,
                           @NotNull String itemId,
                           @NotNull String itemName,
                           int levelBefore,
                           int levelAfter,
                           boolean success,
                           @Nullable String penaltyType,
                           int stonesUsed,
                           double economyCost,
                           long timestamp,
                           boolean guaranteeTriggered) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.itemType = itemType;
        this.itemId = itemId;
        this.itemName = itemName;
        this.levelBefore = levelBefore;
        this.levelAfter = levelAfter;
        this.success = success;
        this.penaltyType = penaltyType;
        this.stonesUsed = stonesUsed;
        this.economyCost = economyCost;
        this.timestamp = timestamp;
        this.guaranteeTriggered = guaranteeTriggered;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    @NotNull
    public String getPlayerName() {
        return playerName;
    }

    @NotNull
    public String getItemType() {
        return itemType;
    }

    @NotNull
    public String getItemId() {
        return itemId;
    }

    @NotNull
    public String getItemName() {
        return itemName;
    }

    public int getLevelBefore() {
        return levelBefore;
    }

    public int getLevelAfter() {
        return levelAfter;
    }

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public String getPenaltyType() {
        return penaltyType;
    }

    public int getStonesUsed() {
        return stonesUsed;
    }

    public double getEconomyCost() {
        return economyCost;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isGuaranteeTriggered() {
        return guaranteeTriggered;
    }

    /**
     * 构建器
     */
    public static class Builder {
        private String id;
        private UUID playerUuid;
        private String playerName;
        private String itemType;
        private String itemId;
        private String itemName;
        private int levelBefore;
        private int levelAfter;
        private boolean success;
        private String penaltyType;
        private int stonesUsed;
        private double economyCost;
        private long timestamp;
        private boolean guaranteeTriggered;

        public Builder() {
            this.id = UUID.randomUUID().toString().substring(0, 8);
            this.timestamp = System.currentTimeMillis();
        }

        public Builder id(@NotNull String id) {
            this.id = id;
            return this;
        }

        public Builder player(@NotNull UUID uuid, @NotNull String name) {
            this.playerUuid = uuid;
            this.playerName = name;
            return this;
        }

        public Builder item(@NotNull String type, @NotNull String id, @NotNull String name) {
            this.itemType = type;
            this.itemId = id;
            this.itemName = name;
            return this;
        }

        public Builder levels(int before, int after) {
            this.levelBefore = before;
            this.levelAfter = after;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder penalty(@Nullable String type) {
            this.penaltyType = type;
            return this;
        }

        public Builder stonesUsed(int count) {
            this.stonesUsed = count;
            return this;
        }

        public Builder economyCost(double cost) {
            this.economyCost = cost;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder guaranteeTriggered(boolean triggered) {
            this.guaranteeTriggered = triggered;
            return this;
        }

        public UpgradeLogEntry build() {
            return new UpgradeLogEntry(id, playerUuid, playerName, itemType, itemId, itemName,
                    levelBefore, levelAfter, success, penaltyType, stonesUsed, economyCost,
                    timestamp, guaranteeTriggered);
        }
    }
}
