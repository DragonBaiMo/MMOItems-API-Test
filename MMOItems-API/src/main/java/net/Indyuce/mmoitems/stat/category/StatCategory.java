package net.Indyuce.mmoitems.stat.category;

import org.jetbrains.annotations.NotNull;

public class StatCategory {
    private final String id, name, loreTag;

    /**
     * @param id      Internal identifier of stat category. Must be unique
     * @param name    Name used to identify the category in the item browser
     * @param loreTag Lore tag added to stats with the given category
     */
    public StatCategory(String id, String name, String loreTag) {
        this.id = id;
        this.name = name;
        this.loreTag = loreTag;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getLoreTag() {
        return loreTag;
    }

    public static final StatCategory
            TEMPLATE_OPTION = new StatCategory("TEMPLATE_OPTION", "Template Option, Misc", "Template Option"),
            SOULBOUND = new StatCategory("SOULBOUND", "Soulbound", "Soulbound"),
            ELEMENTAL = new StatCategory("ELEMENTAL", "Elements", "Elements"),
            VANILLA_ATTRIBUTE = new StatCategory("VANILLA_ATTRIBUTE", "Vanilla Attributes", "Vanilla Attribute"),
            REQUIREMENT = new StatCategory("REQUIREMENT", "Item Requirements", "Item Requirement"),
            USE_COST = new StatCategory("USE_COST", "Item Costs", "Use Cost");
}
