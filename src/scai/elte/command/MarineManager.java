package scai.elte.command;

import bwapi.Position;
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
			if (marine.isAttacking() &&  !marine.isStimmed() && marine.canUseTech(TechType.Stim_Packs) ) {
				marine.useTech(TechType.Stim_Packs);
			}
			
		Command c = getActualCommand();
			if (c!= null) {
				if (c.getType()== CommandType.ATTACK_MOVE && marine.getLastCommand().getUnitCommandType() != UnitCommandType.Attack_Move ) {
					marine.attack(c.getTargetPosition());
				}
			} else {
				//Check defense requests
				for (Request r : Main.requests.values()) {
					if (r.getType() == RequestType.DEFEND && marine.getLastCommand().getUnitCommandType() != UnitCommandType.Attack_Move  ) {
						marine.attack(r.getRequestingUnit().getPosition());
					}
				}
				
			}
		}
		/* - Manning bunker logic, disabled for now
		Unit marine = getUnit();
		if ( getUnit().isCompleted() ) {
		if (getUnit().isIdle() && getUnit().getTransport() == null) {
			this.setGotTask(false);
		}
			
		
		if (!this.isGotTask()) {
			
			for (Request r : Main.requests.values()) {
				if (r.getType() == RequestType.COMMAND) { 
				if (getUnit().getTransport() == null) { //Workaround, as unit.isLoaded doesn't seem to work
					if (r.getRequestedCommand().getType() == CommandType.MAN_BUNKER && r.getRequestStatus() == RequestStatus.NEW
						&& r.getAnsweringUnit() == null) {
						r.setRequestStatus(RequestStatus.BEING_ANSWERED);
						System.out.println("Answering request:" + r.getAnsweringUnit());
						System.out.println("ID:" + getUnit().getID() + " Marine manning bunker...");
						System.out.println("ID:" + getUnit().getID() + " Transport:" + getUnit().getTransport());
						r.setAnsweringUnit(getUnit());
						r.getRequestingUnit().load(getUnit());
						this.setGotTask(true);
						break;
					}
					if (r.getRequestedCommand().getType() == CommandType.MAN_BUNKER && r.getRequestStatus() == RequestStatus.BEING_ANSWERED 
							&& r.getAnsweringUnit().getID() == getUnit().getID()) {
						r.getRequestingUnit().load(getUnit());
					}
				}
				}
			}		
		}
		}
		*/
	}
	public void kite() {
		Unit marine = getUnit();
		if (!Main.enemyUnits.isEmpty()) {
			int dist = 0;
			int minDist = Integer.MAX_VALUE;
			Unit nearestEnemy = null;
			for (Unit enemy : Main.enemyUnits.values()) {
				dist = getUnit().getDistance(enemy);
				if (dist<minDist) {
					minDist = dist;
					if (marine.canAttack(enemy)) {
						nearestEnemy = enemy;
					}
				}
			}
			if (nearestEnemy != null) {
				if ( marine.getGroundWeaponCooldown() > 0) { 
					Position moveTile = null;
					double ed;
					double maxEd = 0;
					for (int i=-1; i<2;i++) {
						for (int j = -1; j<2;j++) {
							int x = marine.getPosition().getX();
							int y = marine.getPosition().getY();
							Position direction = new Position(x+i, y+j);
							ed = direction.getDistance(nearestEnemy.getPosition().getX() ,nearestEnemy.getPosition().getY());
							if (ed > maxEd) {
								maxEd = ed;
								moveTile= direction;	
							}
							//reachable?
						}
					}
					marine.move(moveTile);				
				} else  {
					if (marine.getLastCommand().getUnitCommandType() != UnitCommandType.Attack_Move) {
					getUnit().attack(nearestEnemy.getPosition());
					}
				}						
			}
		}
	}
}
