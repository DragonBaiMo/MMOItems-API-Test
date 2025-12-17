package net.Indyuce.mmoitems.combat.modifier;

import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.combat.CombatContext;
import net.Indyuce.mmoitems.combat.CombatModifier;
import org.jetbrains.annotations.Nullable;

/**
 * 固定减伤，先于百分比减伤执行。
 */
public final class FlatReductionModifier implements CombatModifier {
    @Override
    public String getName() {
        return "flat-reduction";
    }

    @Override
    public @Nullable Double apply(CombatContext ctx) {
        double direct = Math.max(0D, ctx.getStat(ItemStats.DECREASE_DIRECT));
        if (direct <= 0D) {
            return null;
        }
        double base = ctx.getAttackMeta().getDamage().getDamage();
        if (base <= 0D) {
            return null;
        }
        double after = Math.max(0D, base - direct);
        if (after == base) {
            return null;
        }
        return after / base;
    }
}
