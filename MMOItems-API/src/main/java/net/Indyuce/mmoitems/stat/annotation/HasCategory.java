package net.Indyuce.mmoitems.stat.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Can be used to give categories to stats
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface HasCategory {

    public String cat();
}
