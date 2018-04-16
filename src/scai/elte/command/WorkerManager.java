package scai.elte.command;

import java.util.HashSet;

import bwapi.Order;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitCommandType;
import bwapi.UnitType;
import bwta.Region;
import scai.elte.command.Request.RequestType;
import scai.elte.main.Main;
import scai.elte.main.MapUtil;

public class WorkerManager extends UnitManager {
	
	public enum WorkerRole {MINERAL, GAS, BUILD, SCV, MILITIA, SCOUT}
	private WorkerRole role;
	private TilePosition targetTile;
	private Unit targetUnit;
	private UnitType buildType;
	
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
		boolean changeRole = false;
		Unit worker = getUnit();
		if (worker.isCompleted()) {
		if (worker.isUnderAttack()) {
			Main.requests.putIfAbsent(worker.getID()+"D", new Request(worker, null, RequestType.DEFEND));	 //When to delete?
		} else {
			Main.requests.remove(worker.getID() + "D");
		}
		
		
		if (role == WorkerRole.BUILD) {

					if ((worker.getOrder() == Order.PlaceBuilding)) {
						buildType = worker.getBuildType();
						
					}
					if ((worker.getOrder() == Order.ConstructingBuilding)) {
						targetUnit = worker.getOrderTarget();
					}
					
					if (targetUnit != null && targetUnit.isCompleted()) { //not gud
						System.out.println(targetUnit.getType() + " completed");
						targetUnit = null;
						targetTile = null;
						changeRole = true;
					}
					
					if (!worker.isConstructing() && targetUnit != null) {
						worker.build(targetUnit.getType(), targetTile);
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
				/*
				Region def = MapUtil.getRegionOfUnit(worker);
				HashSet<Unit> enemies = (HashSet<Unit>) MapUtil.getUnxitsInRegion(def, true);
				System.out.println("esize:" + enemies.size());
				for (Unit e : enemies) {
					if (e.getPlayer() != 
				}
				Unit enemy = MapUtil.getWeakestUnit(MapUtil.getUnitsInRegion(def, true));
				worker.attack(enemy);
				*/
    		}
    		
		}
		}
		if (changeRole) {
			System.out.println("Current:" + role + " prev:" + prevRole);
			role = prevRole;
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

	public UnitType getBuildType() {
		return buildType;
	}

	public void setBuildType(UnitType buildType) {
		this.buildType = buildType;
	}

}
