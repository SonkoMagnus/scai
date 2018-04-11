package scai.elte.command;

import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;

//General command class, for (fighting) units

public class Command {

	private CommandType type;
	private Unit targetUnit;
	private UnitType targetUnitType; //For build orders, mostly
	private TilePosition targetPosition;

	
	public TilePosition getTargetPosition() {
		return targetPosition;
	}

	public void setTargetPosition(TilePosition targetPosition) {
		this.targetPosition = targetPosition;
	}

	public Command(CommandType type) {
		this.type = type;
	}
	
	public Command(CommandType type, Unit targetUnit) {
		this.type = type;
		this.targetUnit = targetUnit;
	}
	
	public Command(CommandType type, Unit targetUnit, UnitType targetUnitType) {
		this.type = type;
		this.targetUnit = targetUnit;
		this.targetUnitType = targetUnitType;
	}

	public Unit getTargetUnit() {
		return targetUnit;
	}

	public void setTargetUnit(Unit targetUnit) {
		this.targetUnit = targetUnit;
	}

	public UnitType getTargetUnitType() {
		return targetUnitType;
	}

	public void setTargetUnitType(UnitType targetUnitType) {
		this.targetUnitType = targetUnitType;
	}

	public CommandType getType() {
		return type;
	}

	public void setType(CommandType type) {
		this.type = type;
	}
}
