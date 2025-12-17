package net.Indyuce.mmoitems.api.integration;

import net.Indyuce.mmoitems.MMOItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * DistanceBonusProvider 注册中心，支持在运行时安全增删，供 Weapon 内联逻辑调用。
 */
public final class DistanceBonusProviderRegistry {

    private static final List<DistanceBonusProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private DistanceBonusProviderRegistry() {
    }

    public static void register(DistanceBonusProvider provider) {
        if (provider == null) {
            return;
        }
        PROVIDERS.add(provider);
        MMOItems.plugin.getLogger().fine("Registered DistanceBonusProvider: " + provider.getClass().getName());
    }

    public static void unregister(DistanceBonusProvider provider) {
        if (provider == null) {
            return;
        }
        PROVIDERS.remove(provider);
    }

    public static List<DistanceBonusProvider> getProviders() {
        return Collections.unmodifiableList(new ArrayList<>(PROVIDERS));
    }

    public static void clear() {
        PROVIDERS.clear();
    }
}
