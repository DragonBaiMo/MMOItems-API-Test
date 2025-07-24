package net.Indyuce.mmoitems.comp.rpg;

import de.tobiyas.racesandclasses.eventprocessing.events.leveling.LevelDownEvent;
import de.tobiyas.racesandclasses.eventprocessing.events.leveling.LevelUpEvent;
import de.tobiyas.racesandclasses.playermanagement.player.RaCPlayer;
import de.tobiyas.racesandclasses.playermanagement.player.RaCPlayerManager;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.player.RPGPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class RacesAndClassesHook implements RPGHandler, Listener {

    @Override
    public void refreshStats(PlayerData data) {
        RaCPlayer info = RaCPlayerManager.get().getPlayer(data.getPlayer());
        info.getManaManager().removeMaxManaBonus("MMOItems");
        info.getManaManager().addMaxManaBonus("MMOItems", data.getStat(ItemStats.MAX_MANA));
    }

    @Override
    public RPGPlayer getInfo(PlayerData data) {
        return new RacePlayer(data);
    }

    /**
     * Update the player's inventory whenever he levels up
     * since it could change its current stat requirements
     */
    @EventHandler
    public void a(LevelUpEvent event) {
        PlayerData.get(event.getPlayer()).resolveModifiersLater();
    }

    @EventHandler
    public void b(LevelDownEvent event) {
        PlayerData.get(event.getPlayer()).resolveModifiersLater();
    }

    public static class RacePlayer extends RPGPlayer {
        public RacePlayer(PlayerData playerData) {
            super(playerData);
        }

        @Override
        public int getLevel() {
            var info = RaCPlayerManager.get().getPlayer(getPlayer().getUniqueId());
            return info.getCurrentLevel();
        }

        @Override
        public String getClassName() {
            var info = RaCPlayerManager.get().getPlayer(getPlayer().getUniqueId());
            return info.getclass().getDisplayName();
        }

        @Override
        public double getMana() {
            var info = RaCPlayerManager.get().getPlayer(getPlayer().getUniqueId());
            return info.getCurrentMana();
        }

        @Override
        public double getStamina() {
            return getPlayer().getFoodLevel();
        }

        @Override
        public void setMana(double value) {
            // Bad implementation
            var info = RaCPlayerManager.get().getPlayer(getPlayer().getUniqueId()).getManaManager();
            info.fillMana(value - info.getCurrentMana());
        }

        @Override
        public void giveMana(double value) {
            var info = RaCPlayerManager.get().getPlayer(getPlayer().getUniqueId()).getManaManager();
            info.fillMana(value);
        }

        @Override
        public void consumeMana(double value) {
            var info = RaCPlayerManager.get().getPlayer(getPlayer().getUniqueId()).getManaManager();
            info.drownMana(value);
        }

        @Override
        public void setStamina(double value) {
            RaCPlayer info = RaCPlayerManager.get().getPlayer(getPlayer().getUniqueId());
            info.getPlayer().setFoodLevel((int) value);
        }
    }
}
