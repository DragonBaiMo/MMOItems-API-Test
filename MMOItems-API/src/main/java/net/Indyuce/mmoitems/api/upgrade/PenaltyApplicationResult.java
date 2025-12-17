package net.Indyuce.mmoitems.api.upgrade;

import org.jetbrains.annotations.NotNull;

/**
 * 惩罚应用结果（含“被保护拦截的惩罚类型”）
 * <p>
 * 用于在不改变原有惩罚语义的前提下，让调用方（例如 GUI）能够区分：
 * <ul>
 *     <li>保护物品拦截了碎裂</li>
 *     <li>保护物品拦截了掉级</li>
 *     <li>保护物品拦截了销毁（如未来支持）</li>
 * </ul>
 * </p>
 */
public class PenaltyApplicationResult {

    private final PenaltyResult result;
    private final PenaltyResult interceptedPenalty;

    private PenaltyApplicationResult(@NotNull PenaltyResult result, @NotNull PenaltyResult interceptedPenalty) {
        this.result = result;
        this.interceptedPenalty = interceptedPenalty;
    }

    @NotNull
    public static PenaltyApplicationResult of(@NotNull PenaltyResult result) {
        return new PenaltyApplicationResult(result, PenaltyResult.NONE);
    }

    @NotNull
    public static PenaltyApplicationResult protectedIntercept(@NotNull PenaltyResult interceptedPenalty) {
        return new PenaltyApplicationResult(PenaltyResult.PROTECTED, interceptedPenalty);
    }

    /**
     * 惩罚结果（NONE/DOWNGRADE/BREAK/DESTROY/PROTECTED）
     */
    @NotNull
    public PenaltyResult getResult() {
        return result;
    }

    /**
     * 当 {@link #getResult()} 为 {@link PenaltyResult#PROTECTED} 时，表示“原本将触发的惩罚类型”。
     * 其他情况下为 {@link PenaltyResult#NONE}。
     */
    @NotNull
    public PenaltyResult getInterceptedPenalty() {
        return interceptedPenalty;
    }
}

