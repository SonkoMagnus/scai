package scai.elte.command;

import java.util.HashSet;

import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitCommandType;
import scai.elte.command.Request.RequestType;
import scai.elte.main.Main;

public class MarineManager extends UnitManager {

	public MarineManager(Unit unit) {
		super(unit);
	}

	//TODO bunker destroyed, free marines
	@Override
	public void operate() {
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
						//System.out.println("ID:" + getUnit().getID() + " Answering bunker request...");
						//Spam command until fulfilled
						
						//System.out.println("ID:" + getUnit().getID()+ " IS_LOADED:" + getUnit().isLoaded());
						r.getRequestingUnit().load(getUnit());
					}
				}
				}
			}		
		}
		}
	}
	
	
	//Work in progress
	public void kite() {
		Unit marine = getUnit();
		if (!Main.enemyUnits.isEmpty()) {
			int dist = 0;
			int minDist = Integer.MAX_VALUE;
			Unit nearestEnemy = null;
			for (Unit enemy : Main.enemyUnits) {
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
							TilePosition direction = new TilePosition(x+i, y+j);
							ed = direction.getDistance(nearestEnemy.getPosition().getX() ,nearestEnemy.getPosition().getY());
						
							if (ed > maxEd) {
								maxEd = ed;
								moveTile= direction.toPosition();
					
								
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
