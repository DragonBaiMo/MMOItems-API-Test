package net.Indyuce.mmoitems.command.mmoitems.update;

import io.lumine.mythic.lib.command.CommandTreeNode;
import io.lumine.mythic.lib.command.argument.Argument;
import net.Indyuce.mmoitems.MMOItems;

public class UpdateCommandTreeNode extends CommandTreeNode {
    public static final Argument<Integer> UPDATE_ID = Argument.AMOUNT_INT
            .withKey("update_id")
            .withAutoComplete((explorer, list) -> MMOItems.plugin.getUpdates().getAll().forEach(update -> list.add(String.valueOf(update.getId()))));

    public UpdateCommandTreeNode(CommandTreeNode parent) {
		super(parent, "update");

		addChild(new ListCommandTreeNode(this));
		addChild(new ApplyCommandTreeNode(this));
		addChild(new InfoCommandTreeNode(this));
	}
}
