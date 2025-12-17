package net.Indyuce.mmoitems.combat;

import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.damage.MeleeAttackMetadata;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 统一的战斗上下文，用于 CombatModifier 读取需要的状态与 StatData。
 */
public final class CombatContext {
    private final Player player;
    private final LivingEntity target;
    private final MeleeAttackMetadata attackMeta;
    private final NBTItem weapon;
    private final PlayerData playerData;
    private final double distance;

    public CombatContext(@NotNull Player player,
                         @NotNull LivingEntity target,
                         @NotNull MeleeAttackMetadata attackMeta,
                         @NotNull NBTItem weapon,
                         @NotNull PlayerData playerData) {
        this.player = player;
        this.target = target;
        this.attackMeta = attackMeta;
        this.weapon = weapon;
        this.playerData = playerData;
        this.distance = player.getLocation().distance(target.getLocation());
    }

    public Player getPlayer() {
        return player;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public MeleeAttackMetadata getAttackMeta() {
        return attackMeta;
    }

    public NBTItem getWeapon() {
        return weapon;
    }

    public PlayerData getPlayerData() {
        return playerData;
    }

    public double getDistance() {
        return distance;
    }

    /**
     * 从 StatMap 中读取指定属性的值（数值型）。
     */
    public double getStat(ItemStat<?, ?> stat) {
        if (stat == null) {
            return 0D;
        }
        return playerData.getStat(stat);
    }
}
