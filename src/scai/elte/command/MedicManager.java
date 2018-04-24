package scai.elte.command;

import bwapi.Unit;

public class MedicManager extends UnitManager {

	public MedicManager(Unit unit) {
		super(unit);
	}
	
	@Override
	public void operate() {
		if (getUnit().isCompleted()) {
		Unit medic = getUnit();
		if (getActualCommand() != null) {
		
		if (getActualCommand().getType() == CommandType.HEAL) {
			
			medic.rightClick(getActualCommand().getTargetUnit()); //Healing..
		} else if (getActualCommand().getType() == CommandType.ATTACK_MOVE) {
			medic.move(getActualCommand().getTargetPosition());
		}
	
		}
		}
	}

}
