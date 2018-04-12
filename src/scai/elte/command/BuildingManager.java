package scai.elte.command;

import bwapi.Unit;
import scai.elte.command.Request.RequestType;
import scai.elte.main.Main;

public class BuildingManager extends UnitManager {

	public BuildingManager(Unit unit) {
		super(unit);
	}
	
	@Override
	public void operate() {
		//If building is under attack, issue a defend request
		requestDefenseIfNeeded();
	
	}
	
	public void requestDefenseIfNeeded() {
		Unit building = getUnit();
		if (building.isUnderAttack()) {			
			Main.requests.putIfAbsent(building.getID()+"D", new Request(getUnit(), null, RequestType.DEFEND));	
			System.out.println("Building" + building.getType() + " under attack, issuing defense request");
		} else {
			Main.requests.remove(building.getID() + "D");
		}
	} 

}
