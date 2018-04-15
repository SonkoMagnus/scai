package scai.elte.command;

import bwapi.TechType;
import bwapi.Unit;
import scai.elte.command.Request.RequestType;
import scai.elte.main.Main;

public class ComsatManager extends BuildingManager{

	public ComsatManager(Unit unit) {
		super(unit);
	}
	 
	@Override
	public void operate() {
		super.operate();
		
		for (String rk : Main.requests.keySet()) {
			Request r = Main.requests.get(rk);
			
			if (r.getRequestStatus() == RequestStatus.NEW && r.getType() == RequestType.COMMAND && r.getRequestedCommand().getType() == CommandType.SCAN) {
				//System.out.println("COMSAT..." + getUnit().canBuild(UnitType.Spell_Scanner_Sweep,  r.getRequestedCommand().getTargettilePosition()));
				//getUnit().build(UnitType.Spell_Scanner_Sweep,  r.getRequestedCommand().getTargettilePosition());
				getUnit().useTech(TechType.Scanner_Sweep, r.getRequestedCommand().getTargettilePosition().toPosition());
				
				/*
				if (getUnit().canBuild(UnitType.Spell_Scanner_Sweep,  r.getRequestedCommand().getTargettilePosition())) {
					
					System.out.println(getUnit().isConstructing());
					
				} 
				*/
			} 
		}
		//getUnit().build(UnitType.Spell_Scanner_Sweep, target)
		
		//UnitType.Spell_Scanner_Sweep;
	}
	//Scan behaviour, TODO

}
