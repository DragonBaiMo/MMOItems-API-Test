package net.Indyuce.mmoitems.api.upgrade.limit;

import org.jetbrains.annotations.NotNull;

/**
 * 每日限制权限分档配置
 * <p>
 * 表示一个权限分档，包含：
 * <ul>
 *     <li>分档ID（配置键名）</li>
 *     <li>权限节点（空字符串表示默认分档）</li>
 *     <li>每日最大强化次数</li>
 *     <li>优先级（数字越大优先级越高）</li>
 * </ul>
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public class DailyLimitTier implements Comparable<DailyLimitTier> {

    /**
     * 分档ID（配置键名）
     */
    private final String id;

    /**
     * 权限节点（空字符串表示默认/无权限要求）
     */
    private final String permission;

    /**
     * 每日最大强化次数
     */
    private final int maxAttempts;

    /**
     * 优先级（数字越大优先级越高）
     */
    private final int priority;

    /**
     * 创建权限分档配置
     *
     * @param id          分档ID
     * @param permission  权限节点
     * @param maxAttempts 每日最大次数
     * @param priority    优先级
     */
    public DailyLimitTier(@NotNull String id, @NotNull String permission, int maxAttempts, int priority) {
        this.id = id;
        this.permission = permission;
        this.maxAttempts = maxAttempts;
        this.priority = priority;
    }

    /**
     * 获取分档ID
     *
     * @return 分档ID
     */
    @NotNull
    public String getId() {
        return id;
    }

    /**
     * 获取权限节点
     *
     * @return 权限节点，空字符串表示默认分档
     */
    @NotNull
    public String getPermission() {
        return permission;
    }

    /**
     * 获取每日最大强化次数
     *
     * @return 最大次数
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * 获取优先级
     *
     * @return 优先级
     */
    public int getPriority() {
        return priority;
    }

    /**
     * 检查是否为默认分档（无权限要求）
     *
     * @return 如果是默认分档返回 true
     */
    public boolean isDefault() {
        return permission.isEmpty();
    }

    /**
     * 按优先级降序排列（高优先级在前）
     */
    @Override
    public int compareTo(@NotNull DailyLimitTier other) {
        return Integer.compare(other.priority, this.priority);
    }

    @Override
    public String toString() {
        return "DailyLimitTier{" +
                "id='" + id + '\'' +
                ", permission='" + permission + '\'' +
                ", maxAttempts=" + maxAttempts +
                ", priority=" + priority +
                '}';
    }
}
