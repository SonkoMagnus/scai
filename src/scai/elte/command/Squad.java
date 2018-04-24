package scai.elte.command;

import java.util.HashMap;
import java.util.HashSet;

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
	
	public Squad() {
		currentOrder = SquadOrder.IDLE;
	}
	//For heal/repair
	private Unit lowestHpOrganic = null;
	private Unit lowestHpMechanic = null;

	
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
					Command c = new Command(CommandType.ATTACK_MOVE);
					c.setTargetPosition(targetPosition);
					if (Main.unitManagers.get(u.getID()) != null) {
					Main.unitManagers.get(u.getID()).setActualCommand(c);
					}
				
				} 
				else if (u.getType()==UnitType.Terran_Medic) {
				//	System.out.println("commanding medic");
					Command c;
					
					if (lowestHpOrganic != null) {
						c = new Command(CommandType.HEAL, lowestHpOrganic);
					} else {
						c = new Command(CommandType.ATTACK_MOVE);
						c.setTargetPosition(targetPosition);
						
					}
					if (Main.unitManagers.get(u.getID()) != null) {
					//	System.out.println("commanding medic to " + c.getType());
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
		
		
		
		
	}
	
	public Squad(HashMap<UnitType, Integer> targetComposition) {
		this.targetComposition = targetComposition;
	}
	
	public void assignMember(Unit unit) {
		actualComposition.putIfAbsent(unit.getType(), 1);
		/*
		if (actualComposition.get(unit.getType()) == null) {
			actualComposition.put(unit.getType(), 1);
		} else {
			actualComposition.put(unit.getType(), actualComposition.get(unit.getType()) +1);
		}
		*/
		members.add(unit);
	}
	
	public void removeMember(Unit unit) {
		//actualComposition.put(unit.getType(), actualComposition.get(unit.getType())-1);
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
