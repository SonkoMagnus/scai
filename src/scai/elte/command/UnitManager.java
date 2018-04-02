package scai.elte.command;

import java.util.Set;

import bwapi.Unit;

public class UnitManager {
	
	private Unit unit;
	private boolean gotTask;
	private Command unitCommand;
	
	
	public UnitManager(Unit unit) {
		this.unit=unit;
		this.gotTask=false;
	}
	
	public void executeCommand(Command command) {
		
	}
	
	//Main "deciding" loop
	public void operate () {
		
	}
	
	/*
	public void issueRequest() {
		
	} 
	*/

	public Unit getUnit() {
		return unit;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public boolean isGotTask() {
		return gotTask;
	}

	public void setGotTask(boolean gotTask) {
		this.gotTask = gotTask;
	}

}
