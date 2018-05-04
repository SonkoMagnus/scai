package scai.elte.command;

import bwapi.Color;
import bwapi.Unit;
import scai.elte.main.Main;

public class MedicManager extends UnitManager {

	public MedicManager(Unit unit) {
		super(unit);
	}

	@Override
	public void operate() {
		if (getUnit().isCompleted()) {
			Unit medic = getUnit();
			if (getActualCommand() != null) {
				if (getActualCommand().getType() == CommandType.HEAL && Main.frameCount % 10 == 0) {
					medic.attack(getActualCommand().getTargetUnit()); // Healing..
				} else if (getActualCommand().getType() == CommandType.ATTACK_MOVE) {
					medic.move(getActualCommand().getTargetPosition());
				}
			}
		}
	}

}
