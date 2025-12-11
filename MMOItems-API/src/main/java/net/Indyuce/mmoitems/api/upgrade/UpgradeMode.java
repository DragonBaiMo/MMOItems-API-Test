package net.Indyuce.mmoitems.api.upgrade;

/**
 * 强化命令的工作模式枚举
 * <p>
 * 定义了两种强化模式：
 * <ul>
 *     <li>COMMON - 普通模式：与背包点击强化行为一致，失败时触发惩罚</li>
 *     <li>PROTECT - 防护模式：失败时跳过所有惩罚（掉级、碎裂），仅限管理员使用</li>
 * </ul>
 * </p>
 *
 * @author MMOItems Team
 * @since 强化命令系统扩展
 */
public enum UpgradeMode {

    /**
     * 普通强化模式
     * <p>
     * 行为说明：
     * <ul>
     *     <li>遍历背包查找匹配的强化石并消耗</li>
     *     <li>成功时升级物品</li>
     *     <li>失败时按照物品配置触发惩罚（碎裂 → 掉级 → 销毁）</li>
     * </ul>
     * </p>
     */
    COMMON("common", "普通模式"),

    /**
     * 防护强化模式
     * <p>
     * 行为说明：
     * <ul>
     *     <li>遍历背包查找匹配的强化石并消耗</li>
     *     <li>成功时升级物品</li>
     *     <li>失败时跳过所有惩罚判定，物品保持原状</li>
     * </ul>
     * 权限要求：mmoitems.command.item.upgrade.protect（默认 op）
     * </p>
     */
    PROTECT("protect", "防护模式");

    private final String id;
    private final String displayName;

    UpgradeMode(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    /**
     * 获取模式的标识符（用于命令解析）
     *
     * @return 模式标识符（小写）
     */
    public String getId() {
        return id;
    }

    /**
     * 获取模式的显示名称（用于消息提示）
     *
     * @return 模式显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 根据标识符解析强化模式
     *
     * @param id 模式标识符（不区分大小写）
     * @return 对应的强化模式，如果不匹配则返回 null
     */
    public static UpgradeMode fromId(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        for (UpgradeMode mode : values()) {
            if (mode.id.equalsIgnoreCase(id)) {
                return mode;
            }
        }
        return null;
    }

    /**
     * 检查是否为防护模式
     *
     * @return 如果是 PROTECT 模式返回 true
     */
    public boolean isProtect() {
        return this == PROTECT;
    }
}
