package net.Indyuce.mmoitems.api.upgrade.penalty;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.manager.Reloadable;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 全局惩罚梯度配置管理器
 * <p>
 * 提供按等级段配置的全局惩罚规则：
 * <ul>
 *     <li>当物品未配置惩罚规则时，使用全局配置</li>
 *     <li>支持按等级段配置不同的惩罚类型和概率</li>
 *     <li>优先级：物品配置 > 全局配置</li>
 * </ul>
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public class GlobalPenaltyConfig implements Reloadable {

    /**
     * 是否启用全局惩罚梯度
     */
    private boolean enabled;

    /**
     * 惩罚梯度列表（按优先级排序）
     */
    private final List<PenaltyTier> tiers = new ArrayList<>();

    /**
     * 默认惩罚配置
     */
    private PenaltyTier defaultTier;

    /**
     * 创建并加载配置
     */
    public GlobalPenaltyConfig() {
        reload();
    }

    @Override
    public void reload() {
        tiers.clear();
        defaultTier = new PenaltyTier("default", PenaltyType.NONE, 0, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);

        ConfigurationSection config = MMOItems.plugin.getConfig().getConfigurationSection("item-upgrading.global-penalty");
        if (config == null) {
            this.enabled = false;
            return;
        }

        this.enabled = config.getBoolean("enabled", false);
        if (!enabled) {
            return;
        }

        // 解析等级段配置
        ConfigurationSection tiersSection = config.getConfigurationSection("tiers");
        if (tiersSection == null) {
            return;
        }

        for (String tierKey : tiersSection.getKeys(false)) {
            ConfigurationSection tierConfig = tiersSection.getConfigurationSection(tierKey);
            if (tierConfig == null) continue;

            // 解析等级范围
            int[] levelRange = parseLevelRange(tierKey);
            int minLevel = levelRange[0];
            int maxLevel = levelRange[1];

            // 解析惩罚类型
            String typeStr = tierConfig.getString("type", "none").toUpperCase(Locale.ROOT);
            PenaltyType type;
            try {
                type = PenaltyType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                type = PenaltyType.NONE;
            }

            // 解析惩罚参数
            double chance = tierConfig.getDouble("chance", 0);
            int amount = tierConfig.getInt("amount", 1);
            double breakChance = tierConfig.getDouble("break-chance", 0);

            PenaltyTier tier = new PenaltyTier(tierKey, type, chance, amount, breakChance, minLevel, maxLevel);

            if ("default".equalsIgnoreCase(tierKey)) {
                defaultTier = tier;
            } else {
                tiers.add(tier);
            }
        }

        // 按最小等级排序（升序）
        Collections.sort(tiers);
    }

    /**
     * 检查是否启用全局惩罚
     *
     * @return 如果启用返回 true
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 根据等级获取对应的惩罚配置
     *
     * @param level 强化等级
     * @return 惩罚配置，未匹配返回默认配置
     */
    @NotNull
    public PenaltyTier getTierForLevel(int level) {
        for (PenaltyTier tier : tiers) {
            if (level >= tier.getMinLevel() && level <= tier.getMaxLevel()) {
                return tier;
            }
        }
        return defaultTier;
    }

    /**
     * 解析等级范围字符串
     * <p>
     * 支持格式：
     * <ul>
     *     <li>"0-5" → [0, 5]</li>
     *     <li>"6-10" → [6, 10]</li>
     *     <li>"21+" → [21, Integer.MAX_VALUE]</li>
     *     <li>"default" → [Integer.MIN_VALUE, Integer.MAX_VALUE]</li>
     * </ul>
     * </p>
     *
     * @param rangeStr 等级范围字符串
     * @return [最小等级, 最大等级]
     */
    private int[] parseLevelRange(@NotNull String rangeStr) {
        if ("default".equalsIgnoreCase(rangeStr)) {
            return new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE};
        }

        // 处理 "21+" 格式
        if (rangeStr.endsWith("+")) {
            try {
                int min = Integer.parseInt(rangeStr.substring(0, rangeStr.length() - 1));
                return new int[]{min, Integer.MAX_VALUE};
            } catch (NumberFormatException e) {
                return new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE};
            }
        }

        // 处理 "0-5" 格式
        String[] parts = rangeStr.split("-");
        if (parts.length == 2) {
            try {
                int min = Integer.parseInt(parts[0]);
                int max = Integer.parseInt(parts[1]);
                return new int[]{min, max};
            } catch (NumberFormatException e) {
                return new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE};
            }
        }

        // 单个数字
        try {
            int level = Integer.parseInt(rangeStr);
            return new int[]{level, level};
        } catch (NumberFormatException e) {
            return new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE};
        }
    }

    /**
     * 惩罚类型枚举
     */
    public enum PenaltyType {
        /** 无惩罚 */
        NONE,
        /** 降级 */
        DOWNGRADE,
        /** 碎裂（物品消失） */
        BREAK,
        /** 销毁（与碎裂相同，兼容旧配置） */
        DESTROY
    }

    /**
     * 惩罚梯度配置
     */
    public static class PenaltyTier implements Comparable<PenaltyTier> {
        private final String id;
        private final PenaltyType type;
        private final double chance;
        private final int amount;
        private final double breakChance;
        private final int minLevel;
        private final int maxLevel;

        public PenaltyTier(@NotNull String id, @NotNull PenaltyType type, double chance, int amount,
                          double breakChance, int minLevel, int maxLevel) {
            this.id = id;
            this.type = type;
            this.chance = chance;
            this.amount = amount;
            this.breakChance = breakChance;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
        }

        @NotNull
        public String getId() {
            return id;
        }

        @NotNull
        public PenaltyType getType() {
            return type;
        }

        /**
         * 获取惩罚触发概率（0-1）
         */
        public double getChance() {
            return chance;
        }

        /**
         * 获取降级数量
         */
        public int getAmount() {
            return amount;
        }

        /**
         * 获取碎裂概率（0-1）
         */
        public double getBreakChance() {
            return breakChance;
        }

        public int getMinLevel() {
            return minLevel;
        }

        public int getMaxLevel() {
            return maxLevel;
        }

        /**
         * 检查等级是否在此梯度范围内
         */
        public boolean isInRange(int level) {
            return level >= minLevel && level <= maxLevel;
        }

        @Override
        public int compareTo(@NotNull PenaltyTier other) {
            return Integer.compare(this.minLevel, other.minLevel);
        }

        @Override
        public String toString() {
            return "PenaltyTier{" +
                    "id='" + id + '\'' +
                    ", type=" + type +
                    ", chance=" + chance +
                    ", amount=" + amount +
                    ", breakChance=" + breakChance +
                    ", range=[" + minLevel + "-" + maxLevel + "]" +
                    '}';
        }
    }
}
