package net.Indyuce.mmoitems.api.upgrade.transfer;

import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 强化等级转移结果
 * <p>
 * 封装转移操作的结果，包括：成功/失败状态、转移等级、错误信息等
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public class TransferResult {

    /**
     * 转移状态枚举
     */
    public enum Status {
        /**
         * 转移成功
         */
        SUCCESS,
        /**
         * 转移失败（通用错误）
         */
        ERROR
    }

    private final Status status;
    private final String message;
    private final int sourceOriginalLevel;
    private final int targetOriginalLevel;
    private final int transferredLevel;
    private final MMOItem sourceItem;
    private final MMOItem targetItem;

    private TransferResult(Status status, String message, int sourceOriginalLevel,
                           int targetOriginalLevel, int transferredLevel,
                           MMOItem sourceItem, MMOItem targetItem) {
        this.status = status;
        this.message = message;
        this.sourceOriginalLevel = sourceOriginalLevel;
        this.targetOriginalLevel = targetOriginalLevel;
        this.transferredLevel = transferredLevel;
        this.sourceItem = sourceItem;
        this.targetItem = targetItem;
    }

    /**
     * 创建成功结果
     *
     * @param sourceOriginalLevel 源物品原始等级
     * @param targetOriginalLevel 目标物品原始等级
     * @param transferredLevel    转移后目标等级
     * @param sourceItem          处理后的源物品
     * @param targetItem          处理后的目标物品
     * @return 成功结果
     */
    @NotNull
    public static TransferResult success(int sourceOriginalLevel, int targetOriginalLevel,
                                          int transferredLevel, MMOItem sourceItem, MMOItem targetItem) {
        return new TransferResult(Status.SUCCESS, null,
                sourceOriginalLevel, targetOriginalLevel, transferredLevel, sourceItem, targetItem);
    }

    /**
     * 创建错误结果
     *
     * @param message 错误消息
     * @return 错误结果
     */
    @NotNull
    public static TransferResult error(@NotNull String message) {
        return new TransferResult(Status.ERROR, message, 0, 0, 0, null, null);
    }

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * 是否失败
     */
    public boolean isError() {
        return status == Status.ERROR;
    }

    @NotNull
    public Status getStatus() {
        return status;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    /**
     * 获取源物品原始等级
     */
    public int getSourceOriginalLevel() {
        return sourceOriginalLevel;
    }

    /**
     * 获取目标物品原始等级
     */
    public int getTargetOriginalLevel() {
        return targetOriginalLevel;
    }

    /**
     * 获取转移后目标等级
     */
    public int getTransferredLevel() {
        return transferredLevel;
    }

    /**
     * 获取处理后的源物品
     */
    @Nullable
    public MMOItem getSourceItem() {
        return sourceItem;
    }

    /**
     * 获取处理后的目标物品
     */
    @Nullable
    public MMOItem getTargetItem() {
        return targetItem;
    }
}
