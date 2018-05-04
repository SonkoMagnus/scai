package scai.elte.command;

import java.util.HashMap;
import java.util.HashSet;

import bwapi.Color;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import scai.elte.main.Main;

public class Squad {
	
	public enum SquadOrder {
		IDLE, ATTACK_POSITION
	}
	
	private HashMap <UnitType, Integer> targetComposition = new HashMap <UnitType, Integer>();
	private HashMap <UnitType, Integer> actualComposition = new HashMap <UnitType, Integer>();
	private HashSet <Unit> members = new HashSet<Unit>();
	private Integer orderFrame; //Orders expire after a while
	private Integer orderCooldown = 600; //order expire time, in frames, with arbitrary default value
	public SquadOrder currentOrder;
	private Position targetPosition; //Move or attack
	private Position squadCenter;
	
	//For heal/repair
	private Unit lowestHpOrganic = null;
	private Unit lowestHpMechanic = null;
	
	public Squad() {
		currentOrder = SquadOrder.IDLE;
	}

	
//	private HashSet<Unit> injuredOrganic = new HashSet<Unit>();

	
	//Squad loop
	public void executeSquadOrder() {
		//System.out.println("Squad executing order..");
		if (currentOrder != SquadOrder.IDLE && Main.frameCount - orderFrame > orderCooldown ) {
			currentOrder = SquadOrder.IDLE;
		}
		
		int lowOrgHp = 1000000;
		int lowMechHp = 1000000;
		for (UnitType t : actualComposition.keySet()) {
			actualComposition.put(t, 0);
		}
		for (Unit u : members) {
			
			//Not really good - TODO marine center for medix
			if (squadCenter == null) {
				squadCenter = u.getPosition();
			} else {
				int x = ((squadCenter.getX() + u.getPosition().getX())/2);
				int y = ((squadCenter.getY() + u.getPosition().getY())/2);
				squadCenter = new Position(x, y);
			}
			
			
			actualComposition.put(u.getType(), actualComposition.get(u.getType())+1); //Recount the units
			//Regardless of order, we want to heal/repair	
			if (u.getType().isOrganic()) {
				if (u.getType().maxHitPoints() != u.getHitPoints() && !u.isBeingHealed()) { //Lowest health unit who isn't being healed
					if (u.getHitPoints() < lowOrgHp) {
						lowOrgHp = u.getHitPoints();
						lowestHpOrganic = u;
					}
				}
			} else if (u.getType().isMechanical()) {
				if (u.getType().maxHitPoints() != u.getHitPoints()) { //Lowest health mech unit
					if (u.getHitPoints() < lowMechHp) {
						lowMechHp = u.getHitPoints();
						lowestHpMechanic = u;
					}
				}
				
			}
			if (currentOrder == SquadOrder.ATTACK_POSITION) {
				if (u.getType()==UnitType.Terran_Marine) {
					Command c;
					c = new Command(CommandType.ATTACK_MOVE);
					c.setTargetPosition(targetPosition);	
					if (Main.unitManagers.get(u.getID()) != null) {
					Main.unitManagers.get(u.getID()).setActualCommand(c);
					}
				
				} 
				else if (u.getType()==UnitType.Terran_Medic) {
					Command c;
					if (lowestHpOrganic != null) {
						c = new Command(CommandType.HEAL, lowestHpOrganic); 
					} else {
						Position nearestMarinePos = nearestToPosition(UnitType.Terran_Marine, squadCenter).getPosition();
						if (nearestMarinePos != null) {
							c = new Command(CommandType.MOVE);
							c.setTargetPosition(nearestMarinePos);
						} else { 
							c = new Command(CommandType.ATTACK_MOVE);
							c.setTargetPosition(targetPosition);
						}
					}
					if (Main.unitManagers.get(u.getID()) != null) {
					Main.unitManagers.get(u.getID()).setActualCommand(c);
					}
				}
			
			} else if (currentOrder == SquadOrder.IDLE) {
				Main.unitManagers.get(u.getID()).setActualCommand(null);
			}
			
			
		}
		if (lowOrgHp == 1000000) {
			lowestHpOrganic = null;
		}
		if (lowMechHp == 1000000) {
			lowestHpMechanic = null;
		}
		if (squadCenter != null) Main.game.drawCircleMap(squadCenter, 5, Color.Red);
	}
	
	public Unit nearestToPosition(UnitType type, Position pos) {
		Unit nearest = null;
		double minDist = Integer.MAX_VALUE;
		for (Unit m : members) {
			if (type == null || m.getType() == type) {
				double c = Math.sqrt(Math.pow(m.getX() - pos.getX(), 2) + Math.pow(m.getY() - pos.getY(), 2));
				if (c < minDist) {
					minDist = c;
					nearest = m;
				}
			}
		}
		return nearest;
	}
	
	public Squad(HashMap<UnitType, Integer> targetComposition) {
		this.targetComposition = targetComposition;
	}
	
	public void assignMember(Unit unit) {
		actualComposition.putIfAbsent(unit.getType(), 1);
		members.add(unit);
		Main.unitManagers.get(unit.getID()).setSquad(this);
	}
	
	public void removeMember(Unit unit) {
		members.remove(unit);
	}
	
	//Convenience method, check if squad has all the required units
	public boolean fullStrength() {
		boolean full = true;
		
		for (UnitType ut : targetComposition.keySet()) {
			if (actualComposition.getOrDefault(ut,0) < targetComposition.get(ut)) {
				full = false;
				break;
			}
		}
		return full;
	}
	

	public HashSet<Unit> getMembers() {
		return members;
	}

	public void setMembers(HashSet<Unit> members) {
		this.members = members;
	}

	public HashMap<UnitType, Integer> getTargetComposition() {
		return targetComposition;
	}

	public void setTargetComposition(HashMap<UnitType, Integer> targetComposition) {
		this.targetComposition = targetComposition;
	}

	public HashMap<UnitType, Integer> getActualComposition() {
		return actualComposition;
	}

	public void setActualComposition(HashMap<UnitType, Integer> actualComposition) {
		this.actualComposition = actualComposition;
	}

	public Position getTargetPosition() {
		return targetPosition;
	}

	public void setTargetPosition(Position targetPosition) {
		this.targetPosition = targetPosition;
	}

	public Integer getOrderFrame() {
		return orderFrame;
	}

	public void setOrderFrame(Integer orderFrame) {
		this.orderFrame = orderFrame;
	}

	public Integer getOrderCooldown() {
		return orderCooldown;
	}

	public void setOrderCooldown(Integer orderCooldown) {
		this.orderCooldown = orderCooldown;
	}
}
