package net.Indyuce.mmoitems.gui;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.util.AltChar;
import io.lumine.mythic.lib.api.util.ui.SilentNumbers;
import io.lumine.mythic.lib.gui.Navigator;
import io.lumine.mythic.lib.version.Sounds;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.edition.NewItemEdition;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.gui.edition.ItemEdition;
import net.Indyuce.mmoitems.stat.BrowserDisplayIDX;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ItemBrowser extends MMOItemsInventory {
    private final Map<String, ItemStack> cached = new LinkedHashMap<>();

    @NotNull
    private final Type type;
    private boolean deleteMode;

    // Slots used to display items based on the item type explored
    private static final int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int[] slotsAlt = {1, 2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

    public ItemBrowser(Navigator navigator, Type type) {
        super(navigator);

        this.type = type;
    }

    public static ItemBrowser of(Player player, Type type) {
        TypeBrowser prev = TypeBrowser.of(player);
        return new ItemBrowser(prev.getNavigator(), type);
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        Inventory inv = Bukkit.createInventory(this, 54, (deleteMode ? "Delete Mode: " : "Item Explorer: ") + MythicLib.plugin.getAdventureParser().stripColors(type.getName()));

        /*
         * Build cool Item Stacks for buttons and sh
         */
        ItemStack error = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta errorMeta = error.getItemMeta();
        errorMeta.setDisplayName(ChatColor.RED + "- Error -");
        List<String> errorLore = new ArrayList<>();
        errorLore.add("\u00a7\u00a7oAn error occurred while");
        errorLore.add("\u00a7\u00a7otrying to generate that item.");
        errorMeta.setLore(errorLore);
        error.setItemMeta(errorMeta);

        ItemStack noItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta noItemMeta = noItem.getItemMeta();
        noItemMeta.setDisplayName(ChatColor.RED + "- No Item -");
        noItem.setItemMeta(noItemMeta);

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = next.getItemMeta();
        nextMeta.setDisplayName(ChatColor.GREEN + "Next Page");
        next.setItemMeta(nextMeta);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatColor.GREEN + AltChar.rightArrow + " Back");
        back.setItemMeta(backMeta);

        ItemStack create = new ItemStack(new ItemStack(Material.WRITABLE_BOOK));
        ItemMeta createMeta = create.getItemMeta();
        createMeta.setDisplayName(ChatColor.GREEN + "Create New");
        create.setItemMeta(createMeta);

        ItemStack delete = new ItemStack(new ItemStack(Material.CAULDRON));
        ItemMeta deleteMeta = delete.getItemMeta();
        deleteMeta.setDisplayName(ChatColor.RED + (deleteMode ? "Cancel Deletion" : "Delete Item"));
        delete.setItemMeta(deleteMeta);

        ItemStack previous = new ItemStack(Material.ARROW);
        ItemMeta previousMeta = previous.getItemMeta();
        previousMeta.setDisplayName(ChatColor.GREEN + "Previous Page");
        previous.setItemMeta(previousMeta);

        if (type == Type.BLOCK) {
            ItemStack downloadPack = new ItemStack(Material.HOPPER);
            ItemMeta downloadMeta = downloadPack.getItemMeta();
            downloadMeta.setDisplayName(ChatColor.GREEN + "Download Default Resourcepack");
            downloadMeta.setLore(Arrays.asList(ChatColor.LIGHT_PURPLE + "Only seeing stone blocks?", "",
                    ChatColor.RED + "By downloading the default resourcepack you can", ChatColor.RED + "edit the blocks however you want.",
                    ChatColor.RED + "You will still have to add it to your server!"));
            downloadPack.setItemMeta(downloadMeta);
            inv.setItem(45, downloadPack);
        }

        // Get templates of this type
        HashMap<Double, ArrayList<MMOItemTemplate>> templates = BrowserDisplayIDX.select(MMOItems.plugin.getTemplates().getTemplates(type));

        /*
         *  -----------
         *    CALCULATE GUI BOUNDS AND PAGE SIZES
         *
         *      Each display index claims the entire column of items, such that there will be
         *      empty spaces added to fill the inventories.
         *
         *      In Four GUI mode, columns are four slots tall, else they are three slots tall.
         *  -----------
         */
        int[] usedSlots = type.isFourGUIMode() ? slotsAlt : slots;
        int min = (page - 1) * usedSlots.length;
        int max = page * usedSlots.length;
        int n = 0;

        int sc = type.isFourGUIMode() ? 4 : 3;
        int totalSpaceCount = 0;

        for (Map.Entry<Double, ArrayList<MMOItemTemplate>> indexTemplates : templates.entrySet()) {

            // Claim columns
            int totalSpaceAdd = indexTemplates.getValue().size();
            while (totalSpaceAdd > 0) {
                totalSpaceCount += sc;
                totalSpaceAdd -= sc;
            }
        }

        /*
         * Over the page-range currently in use...
         */
        for (int j = min; j < Math.min(max, totalSpaceCount); j++) {
            MMOItemTemplate template = BrowserDisplayIDX.getAt(j, templates);

            // No template here?
            if (template == null) {

                // Set Item
                inv.setItem(usedSlots[n], noItem);

                /*
                 *      Calculate next n from the slots.
                 *
                 *      #1 Adding 7 will give you the slot immediately under
                 *
                 *      #2 If it overflows, subtract 7sc (column space * 7)
                 *         and add one
                 */
                n += 7;
                if (n >= usedSlots.length) {
                    n -= 7 * sc;
                    n++;
                }
                continue;
            }

            // Build item -> any errors?
            final ItemStack item = template.newBuilder(PlayerData.get(playerData).getRPG(), true).build().newBuilder().build();
            if (item == null || item.getType().isAir() || !item.getType().isItem() || item.getItemMeta() == null) {

                // Set Item
                cached.put(template.getId(), error);
                inv.setItem(usedSlots[n], error);

                /*
                 *      Calculate next n from the slots.
                 *
                 *      #1 Adding 7 will give you the slot immediately under
                 *
                 *      #2 If it overflows, subtract 7sc (column space * 7)
                 *         and add one
                 */
                n += 7;
                if (n >= usedSlots.length) {
                    n -= 7 * sc;
                    n++;
                }
                continue;
            }

            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.add("");

            // Deleting lore?
            if (deleteMode) {
                lore.add(ChatColor.RED + AltChar.cross + " CLICK TO DELETE " + AltChar.cross);
                meta.setDisplayName(ChatColor.RED + "DELETE: " + (meta.hasDisplayName() ? meta.getDisplayName() : MMOUtils.getDisplayName(item)));

                // Editing lore?
            } else {
                lore.add(ChatColor.YELLOW + AltChar.smallListDash + " Left click to obtain this item.");
                lore.add(ChatColor.YELLOW + AltChar.smallListDash + " Right click to edit this item.");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);

            // Set item
            cached.put(template.getId(), item);
            inv.setItem(usedSlots[n], cached.get(template.getId()));

            /*
             *      Calculate next n from the slots.
             *
             *      #1 Adding 7 will give you the slot immediately under
             *
             *      #2 If it overflows, subtract 7sc (column space * 7)
             *         and add one
             */
            n += 7;
            if (n >= usedSlots.length) {
                n -= 7 * sc;
                n++;
            }
        }

        // Put the buttons
        if (!deleteMode) {
            inv.setItem(51, create);
        }
        inv.setItem(47, delete);
        inv.setItem(49, back);
        inv.setItem(18, page > 1 ? previous : null);
        inv.setItem(26, max >= totalSpaceCount ? null : next);
        for (int i : usedSlots) {
            if (SilentNumbers.isAir(inv.getItem(i))) {
                inv.setItem(i, noItem);
            }
        }
        return inv;
    }

    public Type getType() {
        return type;
    }

    @Override
    public void whenClicked(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getInventory() != event.getClickedInventory())
            return;

        ItemStack item = event.getCurrentItem();
        if (MMOUtils.isMetaItem(item, false)) {
            if (item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Next Page")) {
                page++;
                open();
            } else if (item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Previous Page")) {
                page--;
                open();
            } else if (item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + AltChar.rightArrow + " Back"))
                getNavigator().popOpen();

            else if (item.getItemMeta().getDisplayName().equals(ChatColor.RED + "Cancel Deletion")) {
                deleteMode = false;
                open();
            } else if (item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Create New"))
                new NewItemEdition(this).enable("Write in the chat the text you want.");

            else if ( item.getItemMeta().getDisplayName().equals(ChatColor.RED + "Delete Item")) {
                deleteMode = true;
                open();
            }
        }

        String id = NBTItem.get(item).getString("MMOITEMS_ITEM_ID");
        if (id.equals(""))
            return;

        if (deleteMode) {
            MMOItems.plugin.getTemplates().deleteTemplate(type, id);
            deleteMode = false;
            open();

        } else {
            if (event.getAction() == InventoryAction.PICKUP_ALL) {
                getPlayer().getInventory().addItem(MMOItems.plugin.getItem(type, id, PlayerData.get(player)));
                getPlayer().playSound(getPlayer().getLocation(), Sounds.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 2);
            }

            if (event.getAction() == InventoryAction.PICKUP_HALF)
                new ItemEdition(getNavigator(), MMOItems.plugin.getTemplates().getTemplate(type, id)).open();
        }
    }
}
