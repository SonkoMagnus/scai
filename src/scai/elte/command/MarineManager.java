package scai.elte.command;

import bwapi.TechType;
import bwapi.Unit;
import bwapi.UnitCommandType;
import scai.elte.command.Request.RequestType;
import scai.elte.main.Main;

public class MarineManager extends UnitManager {

	public MarineManager(Unit unit) {
		super(unit);
	}

	@Override
	public void operate() {
		Unit marine = getUnit();
		if (marine.isCompleted()) {
			if (marine.isAttacking() && !marine.isStimmed() && marine.canUseTech(TechType.Stim_Packs)) {
				marine.useTech(TechType.Stim_Packs);
			}
			Command c = getActualCommand();
			if (c != null) {
				if (c.getType() == CommandType.ATTACK_MOVE
						&& marine.getLastCommand().getUnitCommandType() != UnitCommandType.Attack_Move) {
					marine.attack(c.getTargetPosition());
				}
			} else {
				// Check defense requests
				for (Request r : Main.requests.values()) {
					if (r.getType() == RequestType.DEFEND
							&& marine.getLastCommand().getUnitCommandType() != UnitCommandType.Attack_Move) {
						marine.attack(r.getRequestingUnit().getPosition());
					}
				}
			}
		}
	}
}
