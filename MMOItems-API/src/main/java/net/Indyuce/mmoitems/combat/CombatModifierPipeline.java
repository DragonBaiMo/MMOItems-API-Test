package net.Indyuce.mmoitems.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CombatModifier 管线，按注册顺序执行。
 */
public final class CombatModifierPipeline {

    private final List<CombatModifier> modifiers = new ArrayList<>();

    public void register(CombatModifier modifier) {
        if (modifier != null) {
            modifiers.add(modifier);
        }
    }

    public List<CombatModifier> getModifiers() {
        return Collections.unmodifiableList(modifiers);
    }

    public double applyAll(CombatContext ctx, double baseDamage) {
        double current = baseDamage;
        for (CombatModifier modifier : modifiers) {
            Double mul = modifier.apply(ctx);
            if (mul != null) {
                current *= mul;
            }
        }
        return current;
    }
}
