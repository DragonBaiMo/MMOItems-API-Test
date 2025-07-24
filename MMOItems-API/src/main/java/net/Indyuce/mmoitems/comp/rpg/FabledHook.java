package net.Indyuce.mmoitems.comp.rpg;

import net.Indyuce.mmoitems.api.player.RPGPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import studio.magemonkey.fabled.Fabled;
import studio.magemonkey.fabled.api.event.PlayerLevelUpEvent;
import studio.magemonkey.fabled.api.player.PlayerData;

import java.util.function.Consumer;

public class FabledHook implements RPGHandler, Listener {

    @Override
    public RPGPlayer getInfo(net.Indyuce.mmoitems.api.player.PlayerData data) {
        return new PlayerWrapper(data);
    }

    @EventHandler
    public void b(PlayerLevelUpEvent event) {
        net.Indyuce.mmoitems.api.player.PlayerData.get(event.getPlayerData().getPlayer()).resolveModifiersLater();
    }

    @Override
    public void refreshStats(net.Indyuce.mmoitems.api.player.PlayerData data) {
    }

    private static class PlayerWrapper extends RPGPlayer {
        public PlayerWrapper(net.Indyuce.mmoitems.api.player.PlayerData playerData) {
            super(playerData);
        }

        @Override
        public int getLevel() {
            var rpgdata = Fabled.getData(getPlayer());
            return rpgdata.hasClass() ? rpgdata.getMainClass().getLevel() : 0;
        }

        @Override
        public String getClassName() {
            var rpgdata = Fabled.getData(getPlayer());
            return rpgdata.hasClass() ? rpgdata.getMainClass().getData().getName() : "";
        }

        @Override
        public double getMana() {
            var rpgdata = Fabled.getData(getPlayer());
            return rpgdata.hasClass() ? rpgdata.getMana() : 0;
        }

        @Override
        public void setMana(double value) {
            consumeIfClass(getPlayer(), playerData -> playerData.setMana(value));
        }

        @Override
        public void consumeMana(double value) {
            consumeIfClass(getPlayer(), playerData -> playerData.useMana(value));
        }

        @Override
        public void giveMana(double value) {
            consumeIfClass(getPlayer(), playerData -> playerData.giveMana(value));
        }

        @Override
        public double getStamina() {
            return getPlayer().getFoodLevel();
        }

        @Override
        public void setStamina(double value) {
            getPlayer().setFoodLevel((int) value);
        }

        private static void consumeIfClass(Player player, Consumer<PlayerData> action) {
            var rpgdata = Fabled.getData(player);
            if (rpgdata.hasClass()) action.accept(rpgdata);
        }
    }
}