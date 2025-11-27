package net.Indyuce.mmoitems.manager.data;

import io.lumine.mythic.lib.data.DataLoadResult;
import io.lumine.mythic.lib.data.DefaultOfflineDataHolder;
import io.lumine.mythic.lib.data.yaml.YAMLFlatDatabase;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class YAMLDatabaseImpl extends YAMLFlatDatabase<PlayerData, DefaultOfflineDataHolder> {
    public YAMLDatabaseImpl() {
        super(MMOItems.plugin);
    }

    @Override
    public void saveInSection(PlayerData playerData, ConfigurationSection config) {
        //config.set("permissions-from-items", new ArrayList<>(playerData.getInventory().getPermissions()));
        config.createSection("crafting-queue");
        playerData.getCrafting().saveToYaml(config.getConfigurationSection("crafting-queue"));
    }

    @Override
    protected @NotNull DataLoadResult loadFromSection(@NotNull PlayerData playerData, @NotNull ConfigurationSection config, boolean isSaved) {

        if (config.contains("crafting-queue"))
            playerData.getCrafting().loadFromYaml(config.getConfigurationSection("crafting-queue"));

        if (MMOItems.plugin.hasPermissions() && config.contains("permissions-from-items")) {
            final Permission perms = MMOItems.plugin.getVault().getPermissions();
            config.getStringList("permissions-from-items").forEach(perm -> {
                if (perms.has(playerData.getPlayer(), perm)) perms.playerRemove(playerData.getPlayer(), perm);
            });
        }

        return new DataLoadResult(DataLoadResult.Type.SUCCESS, false, isSaved);
    }

    @Override
    public void setup() {
        // Nothing
    }

    @Override
    public DefaultOfflineDataHolder getOffline(@NotNull UUID uuid) {
        return new DefaultOfflineDataHolder(uuid);
    }

    @Override
    public void close() {
        // Nothing
    }
}
