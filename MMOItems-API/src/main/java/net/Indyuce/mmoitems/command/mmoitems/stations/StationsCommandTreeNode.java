package net.Indyuce.mmoitems.command.mmoitems.stations;

import io.lumine.mythic.lib.command.CommandTreeNode;

public class StationsCommandTreeNode extends CommandTreeNode {
	public StationsCommandTreeNode(CommandTreeNode parent) {
		super(parent, "stations");

		addChild(new OpenCommandTreeNode(this));
		addChild(new ListCommandTreeNode(this));
	}
}
