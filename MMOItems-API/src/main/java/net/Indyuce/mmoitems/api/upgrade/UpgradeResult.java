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

    // ===== 新增字段：辅料效果和保底机制 =====
    /**
     * 直达石额外升级的等级数
     */
    private final int directUpBonusLevels;

    /**
     * 是否触发了保底机制
     */
    private final boolean guaranteeTriggered;

    /**
     * 被保护拦截的惩罚类型（仅当 status 为 FAILURE_PROTECTED 或 penaltyResult 为 PROTECTED 时有意义）
     */
    private final PenaltyResult interceptedPenalty;

    /**
     * 私有构造函数，通过静态工厂方法创建实例
     */
    private UpgradeResult(Status status, MMOItem upgradedItem, PenaltyResult penaltyResult,
                          String message, int previousLevel, int newLevel, int consumedStones) {
        this(status, upgradedItem, penaltyResult, message, previousLevel, newLevel, consumedStones, 0, false, PenaltyResult.NONE);
    }

    /**
     * 完整私有构造函数（含辅料和保底字段）
     */
    private UpgradeResult(Status status, MMOItem upgradedItem, PenaltyResult penaltyResult,
                          String message, int previousLevel, int newLevel, int consumedStones,
                          int directUpBonusLevels, boolean guaranteeTriggered, PenaltyResult interceptedPenalty) {
        this.status = status;
        this.upgradedItem = upgradedItem;
        this.penaltyResult = penaltyResult;
        this.message = message;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
        this.consumedStones = consumedStones;
        this.directUpBonusLevels = directUpBonusLevels;
        this.guaranteeTriggered = guaranteeTriggered;
        this.interceptedPenalty = interceptedPenalty != null ? interceptedPenalty : PenaltyResult.NONE;
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
        return success(upgradedItem, previousLevel, newLevel, consumedStones, 0, false);
    }

    /**
     * 创建成功结果（含辅料和保底信息）
     *
     * @param upgradedItem        升级后的物品
     * @param previousLevel       升级前的等级
     * @param newLevel            升级后的等级
     * @param consumedStones      消耗的强化石数量
     * @param directUpBonusLevels 直达石额外升级等级数
     * @param guaranteeTriggered  是否触发保底
     * @return 成功结果实例
     */
    public static UpgradeResult success(@NotNull MMOItem upgradedItem, int previousLevel,
                                        int newLevel, int consumedStones,
                                        int directUpBonusLevels, boolean guaranteeTriggered) {
        StringBuilder msg = new StringBuilder("强化成功");
        if (guaranteeTriggered) {
            msg.append("（保底触发）");
        }
        if (directUpBonusLevels > 0) {
            msg.append("，直达石额外升级 ").append(directUpBonusLevels).append(" 级");
        }

        return new UpgradeResult(
                Status.SUCCESS,
                upgradedItem,
                PenaltyResult.NONE,
                msg.toString(),
                previousLevel,
                newLevel,
                consumedStones,
                directUpBonusLevels,
                guaranteeTriggered,
                PenaltyResult.NONE
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
                1,
                0,
                false,
                PenaltyResult.NONE
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
                consumedStones,
                0,
                false,
                PenaltyResult.NONE
        );
    }

    /**
     * 创建“保护物品拦截惩罚”的失败结果（可区分拦截类型）
     *
     * @param consumedStones      消耗的强化石数量
     * @param interceptedPenalty  被拦截的惩罚类型（BREAK/DOWNGRADE/DESTROY）
     * @return 失败结果实例
     */
    public static UpgradeResult failureProtected(int consumedStones, @NotNull PenaltyResult interceptedPenalty) {
        return new UpgradeResult(
                Status.FAILURE_PROTECTED,
                null,
                PenaltyResult.PROTECTED,
                "强化失败，但保护物品拦截了惩罚",
                -1,
                -1,
                consumedStones,
                0,
                false,
                interceptedPenalty
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
                consumedStones,
                0,
                false,
                PenaltyResult.NONE
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
                consumedStones,
                0,
                false,
                PenaltyResult.NONE
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
                0,
                0,
                false,
                PenaltyResult.NONE
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

    /**
     * 获取“被保护拦截的惩罚类型”
     * <p>
     * 当结果为 FAILURE_PROTECTED 或 {@link #getPenaltyResult()} 为 PROTECTED 时返回 BREAK/DOWNGRADE/DESTROY；
     * 其他情况返回 NONE。
     * </p>
     */
    @NotNull
    public PenaltyResult getInterceptedPenalty() {
        return interceptedPenalty != null ? interceptedPenalty : PenaltyResult.NONE;
    }

    // ===== 新增 Getter：辅料和保底相关 =====

    /**
     * 获取直达石额外升级的等级数
     * <p>
     * 当直达石效果触发时，会额外升级若干等级
     * </p>
     *
     * @return 额外升级等级数，0 表示未触发
     */
    public int getDirectUpBonusLevels() {
        return directUpBonusLevels;
    }

    /**
     * 检查是否有直达石额外升级
     *
     * @return 如果有额外升级返回 true
     */
    public boolean hasDirectUpBonus() {
        return directUpBonusLevels > 0;
    }

    /**
     * 检查是否触发了保底机制
     * <p>
     * 保底机制触发时，强化必定成功
     * </p>
     *
     * @return 如果触发保底返回 true
     */
    public boolean isGuaranteeTriggered() {
        return guaranteeTriggered;
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
                ", directUpBonusLevels=" + directUpBonusLevels +
                ", guaranteeTriggered=" + guaranteeTriggered +
                '}';
    }
}
