package net.Indyuce.mmoitems.api.upgrade.auxiliary;

/**
 * 辅料类型枚举
 * <p>
 * 定义强化系统支持的三种辅料类型：
 * <ul>
 *     <li>{@link #CHANCE_BONUS} - 幸运石：提升强化成功率</li>
 *     <li>{@link #PROTECTION} - 保护石：降低失败惩罚概率</li>
 *     <li>{@link #DIRECT_UP} - 直达石：概率跳过等级</li>
 * </ul>
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public enum AuxiliaryType {

    /**
     * 幸运石 - 成功率加成
     * <p>
     * 使用时增加强化成功率，效果值为百分比加成。
     * 例如：效果值 10 表示成功率 +10%
     * </p>
     */
    CHANCE_BONUS("chance-bonus", "幸运石"),

    /**
     * 保护石 - 惩罚降低
     * <p>
     * 使用时降低失败惩罚（掉级/碎裂）的触发概率。
     * 效果值为惩罚概率的百分比降低。
     * 例如：效果值 50 表示惩罚概率降低 50%
     * </p>
     */
    PROTECTION("protection", "保护石"),

    /**
     * 直达石 - 跳级
     * <p>
     * 强化成功时有概率额外升级。
     * 包含两个参数：触发概率和跳级数量。
     * </p>
     */
    DIRECT_UP("direct-up", "直达石");

    private final String configKey;
    private final String displayName;

    AuxiliaryType(String configKey, String displayName) {
        this.configKey = configKey;
        this.displayName = displayName;
    }

    /**
     * 获取配置文件中使用的键名
     *
     * @return 配置键名
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * 获取显示名称（用于消息提示）
     *
     * @return 中文显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 根据配置键名查找辅料类型
     *
     * @param key 配置键名
     * @return 对应的辅料类型，未找到返回 null
     */
    public static AuxiliaryType fromConfigKey(String key) {
        if (key == null) return null;
        for (AuxiliaryType type : values()) {
            if (type.configKey.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }
}
