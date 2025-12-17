package net.Indyuce.mmoitems.combat.modifier;

import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.combat.CombatContext;
import net.Indyuce.mmoitems.combat.CombatModifier;
import org.jetbrains.annotations.Nullable;

/**
 * 百分比减伤，位于固定减伤之后。
 */
public final class PercentReductionModifier implements CombatModifier {
    @Override
    public String getName() {
        return "percent-reduction";
    }

    @Override
    public @Nullable Double apply(CombatContext ctx) {
        double percent = Math.max(0D, ctx.getStat(ItemStats.DECREASE_PERCENTAGE));
        if (percent <= 0D) {
            return null;
        }
        double base = ctx.getAttackMeta().getDamage().getDamage();
        if (base <= 0D) {
            return null;
        }
        double after = Math.max(0D, base - base * (percent / 100D));
        if (after == base) {
            return null;
        }
        return after / base;
    }
}
