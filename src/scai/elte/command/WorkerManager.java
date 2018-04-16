package scai.elte.command;

import bwapi.Order;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitCommandType;
import bwta.Region;
import scai.elte.command.Request.RequestType;
import scai.elte.main.Main;
import scai.elte.main.MapUtil;

public class WorkerManager extends UnitManager {
	
	public enum WorkerRole {MINERAL, GAS, BUILD, SCV, MILITIA, SCOUT}
	private WorkerRole role;
	private TilePosition targetTile;
	private Unit targetUnit;
	
	private WorkerRole prevRole;
	//Region def = MapUtil.getRegionOfUnit(r.getRequestingUnit());
	//Set<Unit> unitsInRegion = MapUtil.getUnitsInRegion(def);

	public WorkerManager(Unit unit) {
		super(unit);
	}
	
	public WorkerManager(Unit unit, WorkerRole role) {
		super(unit);
		this.role=role;
	}
	
	@Override
	public void operate() {
		Unit worker = getUnit();
		if (worker.isUnderAttack()) {
			Main.requests.putIfAbsent(worker.getID()+"D", new Request(worker, null, RequestType.DEFEND));	 //When to delete?
		} else {
			Main.requests.remove(worker.getID() + "D");
		}
		
		
		if (role == WorkerRole.BUILD) {
			if (targetUnit != null) {
					// Try to build until it's completed
					if (worker.getOrder() != Order.PlaceBuilding || worker.getOrder() != Order.ConstructingBuilding) {
						worker.build(targetUnit.getType(), targetTile);
					}
					if (targetUnit.isCompleted()) {
						targetUnit = null;
						targetTile = null;
					}
					
			} else {
				if (worker.isIdle()) {
					role = WorkerRole.MINERAL;
				}
			}
		} else if (role == WorkerRole.MINERAL) {
			
    		if (worker.isIdle()) {
    		Unit closestMineral = null;
			// find the closest mineral
			for (Unit neutralUnit : Main.game.neutral().getUnits()) {
				if (neutralUnit.getType().isMineralField()) {
					if (closestMineral == null
							|| worker.getDistance(neutralUnit) < worker.getDistance(closestMineral)) {
						closestMineral = neutralUnit;
					}
				}
			}

			// if a mineral patch was found, send the worker to gather it
			if (closestMineral != null) {
				worker.gather(closestMineral, false);
			}
    		}
			
		} else if (role == WorkerRole.GAS) { //TODO refinery destroyed
			for (Request r : Main.requests.values()) {
				if (r.getType() == RequestType.COMMAND) {
					if (r.getRequestedCommand().getType() == CommandType.GAS_WORKER
							&& r.getRequestStatus() == RequestStatus.BEING_ANSWERED
							&& r.getAnsweringUnit().getID() == worker.getID()) {
						if (worker.isGatheringGas()) {
							r.setRequestStatus(RequestStatus.FULFILLED);
						} else {
							if (!worker.isGatheringGas()) {
								worker.gather(r.getRequestingUnit());
							}

						}
					}
				}
			}
		} else if (role == WorkerRole.MILITIA) {
			if (worker.getLastCommand().getUnitCommandType() != UnitCommandType.Attack_Move) {
				Region def = MapUtil.getRegionOfUnit(worker); 
				Unit enemy = MapUtil.getWeakestUnit(MapUtil.getUnitsInRegion(def));
				worker.attack(enemy);
    		}
    		
		}
		// worker.getTarget().getTilePosition(); //location to put
	}

	

	//builder.build(boi.getUnitType(), boi.getTilePosition()); //work with command
	
	public WorkerRole getRole() {
		return role;
	}

	public void setRole(WorkerRole role) {
		this.role = role;
	}

	public TilePosition getTargetTile() {
		return targetTile;
	}

	public void setTargetTile(TilePosition targetTile) {
		this.targetTile = targetTile;
	}

	public Unit getTargetUnit() {
		return targetUnit;
	}

	public void setTargetUnit(Unit targetUnit) {
		this.targetUnit = targetUnit;
	}

	public WorkerRole getPrevRole() {
		return prevRole;
	}

	public void setPrevRole(WorkerRole prevRole) {
		this.prevRole = prevRole;
	}

}
