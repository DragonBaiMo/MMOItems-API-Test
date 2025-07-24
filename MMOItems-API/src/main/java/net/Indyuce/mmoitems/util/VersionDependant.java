package net.Indyuce.mmoitems.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @see net.Indyuce.mmoitems.stat.annotation.VersionDependant
 * @deprecated Moved to another class
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
public @interface VersionDependant {

    public int[] version();
}
