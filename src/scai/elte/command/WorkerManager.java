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
import scai.elte.main.ScoutInfo;

public class WorkerManager extends UnitManager {

	public enum WorkerRole {
		MINERAL, GAS, BUILD, SCV, MILITIA, SCOUT
	}

	private WorkerRole role;
	private TilePosition targetTile;
	private Unit targetUnit;
	private UnitType buildType;
	private WorkerRole prevRole;

	public WorkerManager(Unit unit) {
		super(unit);
	}

	public WorkerManager(Unit unit, WorkerRole role) {
		super(unit);
		this.role = role;
	}

	@Override
	public void operate() {
		boolean changeRole = false;
		Unit worker = getUnit();
		if (worker.isCompleted()) {
			if (worker.isUnderAttack() && role != WorkerRole.SCOUT) {
				Main.requests.putIfAbsent(worker.getID() + "D", new Request(worker, null, RequestType.DEFEND)); 
			} else {
				Main.requests.remove(worker.getID() + "D");
			}

			if (role == WorkerRole.BUILD) {
				if (worker.isIdle()) {
				}
				if ((worker.getOrder() == Order.PlaceBuilding)) {
					buildType = worker.getBuildType();
				}
				if ((worker.getOrder() == Order.ConstructingBuilding)) {
					targetUnit = worker.getOrderTarget();
				}
				if (targetUnit != null && targetUnit.isCompleted()) {
					targetUnit = null;
					targetTile = null;
					changeRole = true;
				} else if (!worker.isConstructing()) {
					if (worker.canBuild(buildType, targetTile)) {
						worker.build(buildType, targetTile);
					} else {
						if (Main.availableMinerals >= buildType.mineralPrice()
								&& Main.availableGas >= buildType.gasPrice()) {
							targetTile = Main.getBuildTile(worker, buildType, targetTile);
						}
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
			} else if (role == WorkerRole.GAS) { // TODO refinery destroyed
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
					HashSet<Unit> enemies = new HashSet<Unit>();
					for (Unit e : Main.enemyUnits.values()) {
						if (MapUtil.getRegionOfUnit(e) == def) {
							enemies.add(e);
						}
					}
					Unit enemy = MapUtil.getWeakestUnit(enemies);
					if (enemy == null) {
						System.out.println("prevr:" + prevRole);
						changeRole = true;
						// setRole(prevRole);
					} else {
						worker.attack(enemy);
					}
				}
			} else if (role == WorkerRole.SCOUT) {
				if (!Main.game.isExplored(Main.naturalExpansion.getTilePosition())) {
					worker.move(Main.naturalExpansion.getPosition()); // Scout the natural expansion first - hardcoding
																		// this might not be the best
				} else {
					TilePosition moveTile = null;
					// Main.game.drawBoxMap(worker.getOrderTargetPosition().getX(),
					// worker.getOrderTargetPosition().getY(),
					// worker.getOrderTargetPosition().getX()+10,
					// worker.getOrderTargetPosition().getY()+10, Color.Yellow, true);
					if (worker.isBraking() || !worker.isMoving()) {
						int maxImp = Integer.MIN_VALUE;
						for (ScoutInfo sc : Main.scoutHeatMap) {
							if (sc.getImportance() > maxImp) {
								maxImp = sc.getImportance();
								moveTile = sc.getTile();
							}
						}
						double minDist = Integer.MAX_VALUE;
						for (ScoutInfo sc : Main.scoutHeatMap) {
							if (!sc.isThreatenedByGround() && sc.isWalkable() && sc.getImportance() == maxImp) {
								double dist = worker.getDistance(sc.getTile().toPosition());
								if (dist < minDist) {
									minDist = dist;
									moveTile = sc.getTile();
								}
							}
						}
						worker.move(moveTile.toPosition());
					}
				}
			}
		}
		if (changeRole) {
			if (role != prevRole) {
				role = prevRole;
			}
		}

	}

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
