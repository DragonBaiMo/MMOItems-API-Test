package net.Indyuce.mmoitems.command.mmoitems.item;

import io.lumine.mythic.lib.command.CommandTreeNode;

public class ItemCommandTreeNode extends CommandTreeNode {
	public ItemCommandTreeNode(CommandTreeNode parent) {
		super(parent, "item");

		addChild(new IdentifyCommandTreeNode(this));
		addChild(new UnidentifyCommandTreeNode(this));
		addChild(new RepairCommandTreeNode(this));
		addChild(new DeconstructCommandTreeNode(this));
		addChild(new UpgradeCommandTreeNode(this));
	}
}
