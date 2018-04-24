package scai.elte.command;

import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;

//General command class, for (fighting) units. Stateless entity, not tracked.

public class Command {

	private CommandType type;
	private Unit targetUnit;
	private UnitType targetUnitType;  //For build orders, mostly
	private TilePosition targettilePosition;  //For build orders, mostly
	private Position targetPosition; //Attackmove, move

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

	public TilePosition getTargettilePosition() {
		return targettilePosition;
	}

	public void setTargettilePosition(TilePosition targettilePosition) {
		this.targettilePosition = targettilePosition;
	}

	public Position getTargetPosition() {
		return targetPosition;
	}

	public void setTargetPosition(Position targetPosition) {
		this.targetPosition = targetPosition;
	}
}
