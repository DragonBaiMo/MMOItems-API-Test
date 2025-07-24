package net.Indyuce.mmoitems.api.edition;

import io.lumine.mythic.lib.gui.PluginInventory;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.edition.input.ChatEdition;
import net.Indyuce.mmoitems.gui.ItemBrowser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class NewItemEdition implements Edition {
    private final ItemBrowser inv;
    private boolean successful;

    public NewItemEdition(ItemBrowser inv) {
        this.inv = inv;
    }

    @Override
    public PluginInventory getInventory() {
        return inv;
    }

    @Override
    public void enable(String... message) {
        inv.getPlayer().closeInventory();

        inv.getPlayer().sendMessage(ChatColor.YELLOW + "" + ChatColor.STRIKETHROUGH + "-----------------------------------------------------");
        inv.getPlayer().sendMessage(MMOItems.plugin.getPrefix() + "Write in the chat the ID of the new item.");
        inv.getPlayer().sendMessage(MMOItems.plugin.getPrefix() + "Type 'cancel' to abort.");

        // Default chat edition feature
        new ChatEdition(this);
        inv.getPlayer().sendTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "Item Creation", "See chat.", 10, 40, 10);
    }

    @Override
    public boolean processInput(String input) {
        return successful = Bukkit.dispatchCommand(inv.getPlayer(),
                "mmoitems create " + inv.getType().getId() + " " + input.toUpperCase().replace(" ", "_").replace("-", "_"));
    }

    @Override
    public boolean shouldGoBack() {
        return !successful;
    }
}
