package net.Indyuce.mmoitems.inventory;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.api.stat.StatInstance;
import io.lumine.mythic.lib.api.stat.StatMap;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.player.PlayerData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * 登录刷新阶段的属性批处理会话。用于缓存装备栏与饰品栏的扫描结果，
 * 待两侧均完成后再统一写入 MythicLib，避免属性缺失或重复叠加。
 */
public final class LoginRefreshSession {

    private static final Map<UUID, LoginRefreshSession> SESSIONS = new ConcurrentHashMap<>();
    private static final long ACCESSORY_WAIT_LOG_INTERVAL = 5_000L;

    public enum Phase {
        EQUIPMENT,
        ACCESSORY;

        @NotNull
        public static Phase resolve(@NotNull InventoryWatcher watcher) {
            final String className = watcher.getClass().getName();
            if (className.contains("MMOInventorySupplier") || className.contains("OrnamentInventorySupplier")) {
                return ACCESSORY;
            }
            return EQUIPMENT;
        }
    }

    @NotNull
    public static LoginRefreshSession open(@NotNull PlayerData playerData, @NotNull String triggerReason) {
        Objects.requireNonNull(playerData, "playerData");
        Objects.requireNonNull(triggerReason, "triggerReason");

        final UUID uniqueId = playerData.getMMOPlayerData().getUniqueId();
        return SESSIONS.compute(uniqueId, (uuid, existing) -> {
            if (existing != null) {
                existing.lastTrigger = triggerReason;
                existing.touch();
                return existing;
            }
            final LoginRefreshSession created = new LoginRefreshSession(playerData, triggerReason);
            MMOItems.plugin.getLogger().log(Level.FINER,
                    "调试: 创建玩家 " + created.playerName + " 的登录属性刷新会话，触发源：" + triggerReason + "。");
            return created;
        });
    }

    public static void close(@NotNull PlayerData playerData, @NotNull String reason) {
        Objects.requireNonNull(playerData, "playerData");
        final UUID uniqueId = playerData.getMMOPlayerData().getUniqueId();
        final LoginRefreshSession removed = SESSIONS.remove(uniqueId);
        if (removed != null) {
            MMOItems.plugin.getLogger().log(Level.FINER,
                    "调试: 销毁玩家 " + removed.playerName + " 的登录属性刷新会话，原因：" + reason + "。");
        }
    }

    public static LoginRefreshSession get(@NotNull PlayerData playerData) {
        return SESSIONS.get(playerData.getMMOPlayerData().getUniqueId());
    }

    private final PlayerData playerData;
    private final String playerName;
    private final long createdAt;

    private String lastTrigger;
    private long lastWaitLogAt;

    private final Map<Phase, List<ItemUpdate>> pendingUpdates = new EnumMap<>(Phase.class);
    private final Map<Phase, Integer> pendingCounts = new EnumMap<>(Phase.class);
    private final Map<Phase, Boolean> phaseTouched = new EnumMap<>(Phase.class);
    private final Map<Phase, Boolean> phaseReady = new EnumMap<>(Phase.class);

    private boolean committed;

    private LoginRefreshSession(@NotNull PlayerData playerData, @NotNull String triggerReason) {
        this.playerData = playerData;
        this.playerName = playerData.getPlayer() != null ? playerData.getPlayer().getName() : playerData.getMMOPlayerData().getUniqueId().toString();
        this.lastTrigger = triggerReason;
        this.createdAt = System.currentTimeMillis();
        this.lastWaitLogAt = this.createdAt;
        for (Phase phase : Phase.values()) {
            pendingUpdates.put(phase, new ArrayList<>());
            pendingCounts.put(phase, 0);
            phaseTouched.put(phase, false);
            phaseReady.put(phase, false);
        }
    }

    private void touch() {
        this.lastWaitLogAt = System.currentTimeMillis();
    }

    public void beginCycle() {
        for (Phase phase : Phase.values()) {
            pendingUpdates.get(phase).clear();
            pendingCounts.put(phase, 0);
            phaseReady.put(phase, false);
            phaseTouched.put(phase, false);
        }
    }

    public Consumer<ItemUpdate> wrapConsumer(@NotNull Phase phase) {
        phaseTouched.put(phase, true);
        return update -> {
            pendingUpdates.get(phase).add(update);
            pendingCounts.compute(phase, (p, count) -> count == null ? 1 : count + 1);
        };
    }

    public void markPhaseReady(@NotNull Phase phase) {
        phaseReady.put(phase, true);
    }

    public void tryCommit(@NotNull InventoryResolver resolver) {
        if (committed) {
            return;
        }

        final boolean hasAccessoryPhase = Boolean.TRUE.equals(phaseTouched.get(Phase.ACCESSORY));
        final boolean equipmentReady = Boolean.TRUE.equals(phaseReady.get(Phase.EQUIPMENT));
        final boolean accessoryReady = !hasAccessoryPhase || Boolean.TRUE.equals(phaseReady.get(Phase.ACCESSORY));

        if (!equipmentReady || !accessoryReady) {
            final long now = System.currentTimeMillis();
            if (equipmentReady && !accessoryReady && now - lastWaitLogAt >= ACCESSORY_WAIT_LOG_INTERVAL) {
                lastWaitLogAt = now;
                MMOItems.plugin.getLogger().log(Level.FINER,
                        "调试: 玩家 " + playerName + " 登录属性刷新等待 MMOInventory 数据... 触发源：" + lastTrigger + "。");
            }
            if (!equipmentReady && now - createdAt >= ACCESSORY_WAIT_LOG_INTERVAL) {
                MMOItems.plugin.getLogger().log(Level.FINER,
                        "调试: 玩家 " + playerName + " 登录属性刷新仍在等待基础装备扫描完成，触发源：" + lastTrigger + "。");
            }
            return;
        }

        committed = true;

        final MMOPlayerData mmoPlayer = playerData.getMMOPlayerData();
        final StatMap statMap = mmoPlayer.getStatMap();

        statMap.bufferUpdates(() -> {
            final int removed = resolver.unapplyAllItemModifiers();
            final Consumer<ItemUpdate> dispatcher = resolver::processUpdate;
            pendingUpdates.get(Phase.EQUIPMENT).forEach(dispatcher);
            pendingUpdates.get(Phase.ACCESSORY).forEach(dispatcher);
            resolver.resolveModifiers();

            MMOItems.plugin.getLogger().log(Level.FINER,
                    String.format(Locale.ROOT,
                            "调试: 玩家 %s 登录属性刷新批量撤销 %d 个旧 modifier。", playerName, removed));
        });

        for (StatInstance instance : statMap.getInstances()) {
            instance.flushCache();
        }

        final int applied = resolver.countActiveModifiers();
        MMOItems.plugin.getLogger().log(Level.FINER,
                String.format(Locale.ROOT,
                        "调试: 玩家 %s 登录属性刷新批量写入 modifier 数: %d (装备变更=%d, 饰品变更=%d)。", playerName, applied,
                        pendingCounts.get(Phase.EQUIPMENT), pendingCounts.get(Phase.ACCESSORY)));

        SESSIONS.remove(mmoPlayer.getUniqueId());
    }
}
