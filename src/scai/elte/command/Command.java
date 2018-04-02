package scai.elte.command;

import bwapi.Unit;

//General command class, for (fighting) units

public class Command {

	private CommandType type;
	
	public Command(CommandType type) {
		this.type=type;
	}

	public CommandType getType() {
		return type;
	}

	public void setType(CommandType type) {
		this.type = type;
	}
}
