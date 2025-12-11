package net.Indyuce.mmoitems.api.upgrade;

import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 强化操作结果
 * <p>
 * 封装强化操作执行后的结果信息，包括：
 * <ul>
 *     <li>操作状态（成功、失败、错误等）</li>
 *     <li>升级后的物品（成功时）</li>
 *     <li>惩罚结果（失败时）</li>
 *     <li>相关消息</li>
 *     <li>等级变化信息</li>
 * </ul>
 * </p>
 * <p>
 * 使用静态工厂方法创建不同类型的结果实例。
 * </p>
 *
 * @author MMOItems Team
 * @since 强化命令系统
 */
public class UpgradeResult {

    /**
     * 强化操作状态枚举
     */
    public enum Status {
        /**
         * 强化成功
         */
        SUCCESS,

        /**
         * 强化失败，受到防护模式保护
         */
        FAILURE_PROTECTED,

        /**
         * 强化失败，触发了惩罚
         */
        FAILURE_WITH_PENALTY,

        /**
         * 强化失败，无惩罚
         */
        FAILURE_NO_PENALTY,

        /**
         * 操作错误（参数错误、资源不足等）
         */
        ERROR
    }

    private final Status status;
    private final MMOItem upgradedItem;
    private final PenaltyResult penaltyResult;
    private final String message;
    private final int previousLevel;
    private final int newLevel;
    private final int consumedStones;

    /**
     * 私有构造函数，通过静态工厂方法创建实例
     */
    private UpgradeResult(Status status, MMOItem upgradedItem, PenaltyResult penaltyResult,
                          String message, int previousLevel, int newLevel, int consumedStones) {
        this.status = status;
        this.upgradedItem = upgradedItem;
        this.penaltyResult = penaltyResult;
        this.message = message;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
        this.consumedStones = consumedStones;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建成功结果
     *
     * @param upgradedItem   升级后的物品
     * @param previousLevel  升级前的等级
     * @param newLevel       升级后的等级
     * @param consumedStones 消耗的强化石数量
     * @return 成功结果实例
     */
    public static UpgradeResult success(@NotNull MMOItem upgradedItem, int previousLevel,
                                        int newLevel, int consumedStones) {
        return new UpgradeResult(
                Status.SUCCESS,
                upgradedItem,
                PenaltyResult.NONE,
                "强化成功",
                previousLevel,
                newLevel,
                consumedStones
        );
    }

    /**
     * 创建成功结果（简化版，单次升级）
     *
     * @param upgradedItem 升级后的物品
     * @return 成功结果实例
     */
    public static UpgradeResult success(@NotNull MMOItem upgradedItem) {
        return new UpgradeResult(
                Status.SUCCESS,
                upgradedItem,
                PenaltyResult.NONE,
                "强化成功",
                -1,
                -1,
                1
        );
    }

    /**
     * 创建防护模式保护的失败结果
     * <p>
     * 强化失败，但因为使用了 protect 模式，物品未受到任何惩罚
     * </p>
     *
     * @param consumedStones 消耗的强化石数量
     * @return 失败结果实例
     */
    public static UpgradeResult failureProtected(int consumedStones) {
        return new UpgradeResult(
                Status.FAILURE_PROTECTED,
                null,
                PenaltyResult.PROTECTED,
                "强化失败，但物品受到防护模式保护",
                -1,
                -1,
                consumedStones
        );
    }

    /**
     * 创建防护模式保护的失败结果（简化版）
     *
     * @return 失败结果实例
     */
    public static UpgradeResult failureProtected() {
        return failureProtected(1);
    }

    /**
     * 创建带惩罚的失败结果
     *
     * @param penaltyResult  惩罚结果
     * @param previousLevel  惩罚前的等级
     * @param newLevel       惩罚后的等级（掉级时有效）
     * @param consumedStones 消耗的强化石数量
     * @return 失败结果实例
     */
    public static UpgradeResult failureWithPenalty(@NotNull PenaltyResult penaltyResult,
                                                   int previousLevel, int newLevel,
                                                   int consumedStones) {
        String message;
        switch (penaltyResult) {
            case DOWNGRADE:
                int downgradeAmount = previousLevel - newLevel;
                message = "强化失败，物品掉级 " + downgradeAmount + " 级";
                break;
            case BREAK:
                message = "强化失败，物品已碎裂";
                break;
            case DESTROY:
                message = "强化失败，物品已被销毁";
                break;
            case PROTECTED:
                message = "强化失败，但保护物品救回了物品";
                break;
            default:
                message = "强化失败";
                break;
        }

        return new UpgradeResult(
                Status.FAILURE_WITH_PENALTY,
                null,
                penaltyResult,
                message,
                previousLevel,
                newLevel,
                consumedStones
        );
    }

    /**
     * 创建带惩罚的失败结果（简化版）
     *
     * @param penaltyResult 惩罚结果
     * @return 失败结果实例
     */
    public static UpgradeResult failureWithPenalty(@NotNull PenaltyResult penaltyResult) {
        return failureWithPenalty(penaltyResult, -1, -1, 1);
    }

    /**
     * 创建无惩罚的失败结果
     *
     * @param consumedStones 消耗的强化石数量
     * @return 失败结果实例
     */
    public static UpgradeResult failureNoPenalty(int consumedStones) {
        return new UpgradeResult(
                Status.FAILURE_NO_PENALTY,
                null,
                PenaltyResult.NONE,
                "强化失败",
                -1,
                -1,
                consumedStones
        );
    }

    /**
     * 创建无惩罚的失败结果（简化版）
     *
     * @return 失败结果实例
     */
    public static UpgradeResult failureNoPenalty() {
        return failureNoPenalty(1);
    }

    /**
     * 创建错误结果
     *
     * @param message 错误消息
     * @return 错误结果实例
     */
    public static UpgradeResult error(@NotNull String message) {
        return new UpgradeResult(
                Status.ERROR,
                null,
                PenaltyResult.NONE,
                message,
                -1,
                -1,
                0
        );
    }

    // ==================== 状态查询方法 ====================

    /**
     * 获取操作状态
     *
     * @return 状态枚举
     */
    @NotNull
    public Status getStatus() {
        return status;
    }

    /**
     * 检查是否成功
     *
     * @return 如果强化成功返回 true
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * 检查是否失败
     *
     * @return 如果强化失败返回 true（不包括错误）
     */
    public boolean isFailure() {
        return status == Status.FAILURE_PROTECTED
                || status == Status.FAILURE_WITH_PENALTY
                || status == Status.FAILURE_NO_PENALTY;
    }

    /**
     * 检查是否发生错误
     *
     * @return 如果发生错误返回 true
     */
    public boolean isError() {
        return status == Status.ERROR;
    }

    /**
     * 检查物品是否受到损失
     * <p>
     * 物品损失包括：掉级、碎裂、销毁
     * </p>
     *
     * @return 如果物品受到损失返回 true
     */
    public boolean hasItemLoss() {
        return penaltyResult != null && penaltyResult.isSevere();
    }

    // ==================== 数据访问方法 ====================

    /**
     * 获取升级后的物品
     *
     * @return 升级后的 MMOItem，失败时为 null
     */
    @Nullable
    public MMOItem getUpgradedItem() {
        return upgradedItem;
    }

    /**
     * 获取惩罚结果
     *
     * @return 惩罚结果枚举
     */
    @NotNull
    public PenaltyResult getPenaltyResult() {
        return penaltyResult != null ? penaltyResult : PenaltyResult.NONE;
    }

    /**
     * 获取结果消息
     *
     * @return 描述结果的消息文本
     */
    @NotNull
    public String getMessage() {
        return message != null ? message : "";
    }

    /**
     * 获取操作前的等级
     *
     * @return 操作前等级，-1 表示未记录
     */
    public int getPreviousLevel() {
        return previousLevel;
    }

    /**
     * 获取操作后的等级
     *
     * @return 操作后等级，-1 表示未记录
     */
    public int getNewLevel() {
        return newLevel;
    }

    /**
     * 获取等级变化量
     *
     * @return 等级变化量（正数表示升级，负数表示掉级），0 表示无变化或未记录
     */
    public int getLevelChange() {
        if (previousLevel < 0 || newLevel < 0) {
            return 0;
        }
        return newLevel - previousLevel;
    }

    /**
     * 获取消耗的强化石数量
     *
     * @return 消耗的强化石数量
     */
    public int getConsumedStones() {
        return consumedStones;
    }

    @Override
    public String toString() {
        return "UpgradeResult{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", penaltyResult=" + penaltyResult +
                ", previousLevel=" + previousLevel +
                ", newLevel=" + newLevel +
                ", consumedStones=" + consumedStones +
                '}';
    }
}
