package net.Indyuce.mmoitems.combat;

import org.jetbrains.annotations.Nullable;

/**
 * 通用战斗修正接口，返回相对于当前伤害的乘数；返回 null 表示不处理。
 */
public interface CombatModifier {

    /**
     * @return 唯一名称/标识
     */
    String getName();

    /**
     * @param ctx 战斗上下文
     * @return 伤害乘数；null 表示跳过
     */
    @Nullable
    Double apply(CombatContext ctx);
}
