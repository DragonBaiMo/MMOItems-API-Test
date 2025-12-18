package net.Indyuce.mmoitems.api.upgrade.effects;

import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * 强化特效播放器
 * <p>
 * 提供强化成功/失败/碎裂/保底等特效的播放功能：
 * <ul>
 *     <li>粒子效果（可配置）</li>
 *     <li>音效（可配置）</li>
 *     <li>权限控制（可选）</li>
 *     <li>等级段额外特效</li>
 * </ul>
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public class UpgradeEffectsPlayer {

    /**
     * 特效类型枚举
     */
    public enum EffectType {
        SUCCESS("success"),
        FAILURE("failure"),
        BREAK("break"),
        GUARANTEE("guarantee");

        private final String configKey;

        EffectType(String configKey) {
            this.configKey = configKey;
        }

        public String getConfigKey() {
            return configKey;
        }
    }

    /**
     * 检查特效是否启用
     *
     * @return 是否启用
     */
    public static boolean isEnabled() {
        return MMOItems.plugin.getConfig().getBoolean("item-upgrading.effects.enabled", true);
    }

    /**
     * 获取粒子特效权限
     *
     * @return 权限节点（空字符串表示无权限限制）
     */
    @NotNull
    public static String getParticlePermission() {
        return MMOItems.plugin.getConfig().getString("item-upgrading.effects.particle-permission", "");
    }

    /**
     * 播放强化特效
     *
     * @param player 玩家
     * @param type   特效类型
     */
    public static void playEffect(@NotNull Player player, @NotNull EffectType type) {
        playEffect(player, type, -1);
    }

    /**
     * 播放强化特效（支持等级段额外特效）
     *
     * @param player      玩家
     * @param type        特效类型
     * @param upgradeLevel 强化等级（用于等级段额外特效，-1 表示不使用）
     */
    public static void playEffect(@NotNull Player player, @NotNull EffectType type, int upgradeLevel) {
        if (!isEnabled()) {
            return;
        }

        ConfigurationSection effectsConfig = MMOItems.plugin.getConfig().getConfigurationSection("item-upgrading.effects");
        if (effectsConfig == null) {
            return;
        }

        // 获取特效配置
        ConfigurationSection typeConfig = effectsConfig.getConfigurationSection(type.getConfigKey());
        if (typeConfig == null) {
            return;
        }

        Location location = player.getLocation().add(0, 1, 0);

        // 播放基础特效
        playParticleAndSound(player, location, typeConfig);

        // 如果是成功类型且有等级段额外特效配置，播放额外特效
        if (type == EffectType.SUCCESS && upgradeLevel > 0) {
            playLevelEffects(player, location, effectsConfig, upgradeLevel);
        }
    }

    /**
     * 播放粒子和音效
     *
     * @param player   玩家
     * @param location 位置
     * @param config   配置段
     */
    private static void playParticleAndSound(@NotNull Player player, @NotNull Location location,
                                              @NotNull ConfigurationSection config) {
        // 播放粒子效果（检查权限）
        String particlePermission = getParticlePermission();
        boolean canShowParticle = particlePermission.isEmpty() || player.hasPermission(particlePermission);

        if (canShowParticle) {
            String particleName = config.getString("particle", "VILLAGER_HAPPY");
            int particleCount = config.getInt("particle-count", 30);

            try {
                Particle particle = Particle.valueOf(particleName.toUpperCase());
                player.getWorld().spawnParticle(particle, location, particleCount, 0.5, 0.5, 0.5, 0.1);
            } catch (IllegalArgumentException e) {
                MMOItems.plugin.getLogger().log(Level.WARNING, "无效的粒子效果: " + particleName);
            }
        }

        // 播放音效
        String soundName = config.getString("sound", "ENTITY_PLAYER_LEVELUP");
        float volume = (float) config.getDouble("volume", 1.0);
        float pitch = (float) config.getDouble("pitch", 1.0);

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            MMOItems.plugin.getLogger().log(Level.WARNING, "无效的音效: " + soundName);
        }
    }

    /**
     * 播放等级段额外特效
     *
     * @param player       玩家
     * @param location     位置
     * @param effectsConfig 特效配置
     * @param upgradeLevel 强化等级
     */
    private static void playLevelEffects(@NotNull Player player, @NotNull Location location,
                                          @NotNull ConfigurationSection effectsConfig, int upgradeLevel) {
        ConfigurationSection levelEffects = effectsConfig.getConfigurationSection("level-effects");
        if (levelEffects == null) {
            return;
        }

        // 权限检查
        String particlePermission = getParticlePermission();
        boolean canShowParticle = particlePermission.isEmpty() || player.hasPermission(particlePermission);
        if (!canShowParticle) {
            return;
        }

        // 按优先级查找匹配的等级段
        Map<Integer, ConfigurationSection> matchedEffects = new HashMap<>();

        for (String key : levelEffects.getKeys(false)) {
            ConfigurationSection levelConfig = levelEffects.getConfigurationSection(key);
            if (levelConfig == null) continue;

            // 解析等级段格式（如 "15+"、"20+"）
            int threshold = parseLevelThreshold(key);
            if (threshold > 0 && upgradeLevel >= threshold) {
                matchedEffects.put(threshold, levelConfig);
            }
        }

        // 播放所有匹配的等级段特效（按阈值从低到高）
        matchedEffects.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    ConfigurationSection levelConfig = entry.getValue();
                    String particleName = levelConfig.getString("particle");
                    int particleCount = levelConfig.getInt("particle-count", 50);

                    if (particleName != null) {
                        try {
                            Particle particle = Particle.valueOf(particleName.toUpperCase());
                            player.getWorld().spawnParticle(particle, location, particleCount, 0.8, 0.8, 0.8, 0.1);
                        } catch (IllegalArgumentException e) {
                            MMOItems.plugin.getLogger().log(Level.WARNING, "无效的粒子效果: " + particleName);
                        }
                    }
                });
    }

    /**
     * 解析等级阈值（如 "15+" -> 15）
     *
     * @param key 配置键
     * @return 等级阈值（无效返回 -1）
     */
    private static int parseLevelThreshold(@NotNull String key) {
        try {
            if (key.endsWith("+")) {
                return Integer.parseInt(key.substring(0, key.length() - 1));
            }
            return Integer.parseInt(key);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * 播放成功特效
     *
     * @param player       玩家
     * @param upgradeLevel 强化等级
     */
    public static void playSuccess(@NotNull Player player, int upgradeLevel) {
        playEffect(player, EffectType.SUCCESS, upgradeLevel);
    }

    /**
     * 播放失败特效
     *
     * @param player 玩家
     */
    public static void playFailure(@NotNull Player player) {
        playEffect(player, EffectType.FAILURE);
    }

    /**
     * 播放碎裂特效
     *
     * @param player 玩家
     */
    public static void playBreak(@NotNull Player player) {
        playEffect(player, EffectType.BREAK);
    }

    /**
     * 播放保底触发特效
     *
     * @param player 玩家
     */
    public static void playGuarantee(@NotNull Player player) {
        playEffect(player, EffectType.GUARANTEE);
    }
}
