package net.Indyuce.mmoitems.api.upgrade;

/**
 * 强化失败惩罚结果枚举
 * <p>
 * 定义了强化失败后可能产生的各种惩罚结果：
 * <ul>
 *     <li>NONE - 无惩罚，仅失败</li>
 *     <li>DOWNGRADE - 掉级惩罚，物品等级降低</li>
 *     <li>BREAK - 碎裂惩罚，物品变为碎裂状态（需要修复）</li>
 *     <li>DESTROY - 销毁惩罚，物品被完全销毁</li>
 *     <li>PROTECTED - 被保护物品救回，消耗保护物品但物品完好</li>
 * </ul>
 * </p>
 * <p>
 * 惩罚优先级顺序：碎裂 → 掉级 → 销毁
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public enum PenaltyResult {

    /**
     * 无惩罚
     * <p>
     * 强化失败，但没有触发任何惩罚机制。
     * 物品保持原状，仅消耗强化石。
     * </p>
     */
    NONE("none", "无惩罚", false),

    /**
     * 掉级惩罚
     * <p>
     * 强化失败后触发掉级机制：
     * <ul>
     *     <li>物品等级降低（降级数量由配置决定）</li>
     *     <li>等级不会低于最小等级（min）</li>
     *     <li>掉级前会尝试消耗掉级保护物品</li>
     * </ul>
     * </p>
     */
    DOWNGRADE("downgrade", "掉级", true),

    /**
     * 碎裂惩罚
     * <p>
     * 强化失败后触发碎裂机制：
     * <ul>
     *     <li>物品进入碎裂状态</li>
     *     <li>碎裂的物品需要使用特定方式修复</li>
     *     <li>碎裂前会尝试消耗碎裂保护物品</li>
     * </ul>
     * </p>
     */
    BREAK("break", "碎裂", true),

    /**
     * 销毁惩罚
     * <p>
     * 强化失败后触发销毁机制：
     * <ul>
     *     <li>物品被完全销毁，从背包中移除</li>
     *     <li>这是最严重的惩罚结果</li>
     *     <li>仅在启用 destroyOnFail 配置时触发</li>
     * </ul>
     * </p>
     */
    DESTROY("destroy", "销毁", true),

    /**
     * 被保护物品救回
     * <p>
     * 原本应该触发惩罚，但被保护物品救回：
     * <ul>
     *     <li>消耗了背包中的保护物品</li>
     *     <li>物品保持原状，未受到任何损失</li>
     *     <li>适用于掉级保护和碎裂保护</li>
     * </ul>
     * </p>
     */
    PROTECTED("protected", "被保护", false);

    private final String id;
    private final String displayName;
    private final boolean severe;

    /**
     * 构造函数
     *
     * @param id          惩罚结果标识符
     * @param displayName 惩罚结果显示名称
     * @param severe      是否为严重惩罚（会对物品造成实质性损失）
     */
    PenaltyResult(String id, String displayName, boolean severe) {
        this.id = id;
        this.displayName = displayName;
        this.severe = severe;
    }

    /**
     * 获取惩罚结果的标识符
     *
     * @return 标识符（小写）
     */
    public String getId() {
        return id;
    }

    /**
     * 获取惩罚结果的显示名称
     *
     * @return 显示名称（用于消息提示）
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 检查是否为严重惩罚
     * <p>
     * 严重惩罚包括：掉级、碎裂、销毁
     * 非严重惩罚包括：无惩罚、被保护
     * </p>
     *
     * @return 如果是严重惩罚返回 true
     */
    public boolean isSevere() {
        return severe;
    }

    /**
     * 检查惩罚是否导致物品丢失
     * <p>
     * 物品丢失指物品被销毁或碎裂不可用的情况
     * </p>
     *
     * @return 如果导致物品丢失返回 true
     */
    public boolean causesItemLoss() {
        return this == DESTROY || this == BREAK;
    }

    /**
     * 检查惩罚是否导致等级变化
     *
     * @return 如果导致等级变化返回 true
     */
    public boolean causesLevelChange() {
        return this == DOWNGRADE;
    }

    /**
     * 检查是否有惩罚发生（无论是否被保护）
     *
     * @return 如果有惩罚发生返回 true
     */
    public boolean hasPenalty() {
        return this != NONE;
    }

    /**
     * 检查物品是否安全（未受损）
     * <p>
     * 安全状态包括：无惩罚、被保护
     * </p>
     *
     * @return 如果物品安全返回 true
     */
    public boolean isItemSafe() {
        return this == NONE || this == PROTECTED;
    }

    /**
     * 根据标识符解析惩罚结果
     *
     * @param id 惩罚结果标识符（不区分大小写）
     * @return 对应的惩罚结果，如果不匹配则返回 null
     */
    public static PenaltyResult fromId(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        for (PenaltyResult result : values()) {
            if (result.id.equalsIgnoreCase(id)) {
                return result;
            }
        }
        return null;
    }
}
