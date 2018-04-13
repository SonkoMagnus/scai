package scai.elte.command;

import java.util.PriorityQueue;

import bwapi.Unit;
import scai.elte.command.Request.RequestType;
import scai.elte.main.Main;
import scai.elte.strategy.BasePlanItem;
import scai.elte.strategy.TechItem;
import scai.elte.strategy.UpgradeItem;

public class BuildingManager extends UnitManager {

	private PriorityQueue<BasePlanItem> improveList = new PriorityQueue<BasePlanItem>(); //TODO comparator with importance value 
	
	public BuildingManager(Unit unit) {
		super(unit);
	}

	@Override
	public void operate() {
		// If building is under attack, issue a defend request
		
		//Unit building = getUnit();
		requestDefenseIfNeeded();
		/*
		TechItem techToResearch = null;
		UpgradeItem upgradeToResearch = null;
		for (UpgradeItem ui : Main.buildOrder.getUpgradeOrder()) {
			int highPrio = Integer.MIN_VALUE;
			if (building.canUpgrade(ui.getUpgradeType()) && Main.supplyUsedActual > ui.getSupplyThreshold()) { // &&supply
				if (highPrio < ui.getPriority()) {
					highPrio = ui.getPriority();
					upgradeToResearch = ui;
				}
			}
		}

		for (TechItem ti : Main.buildOrder.getTechOrder()) {
			int highPrio = Integer.MIN_VALUE;

			if (building.canResearch(ti.getTechType()) && Main.supplyUsedActual > ti.getSupplyThreshold()) {
				if (highPrio < ti.getPriority()) {
					highPrio = ti.getPriority();
					techToResearch = ti;
				}

			}

		}
		//System.out.println("UPG:" + upgradeToResearch.getUpgradeType() + " TECH:" + techToResearch.getTechType());

		if (techToResearch != null && upgradeToResearch != null) {
			if (techToResearch.getPriority() >= upgradeToResearch.getPriority()) {
				if (Main.availableMinerals > techToResearch.getTechType().mineralPrice()) {// TODO available gas!
					building.research(techToResearch.getTechType());
				}
			} else {
				if (Main.availableMinerals > techToResearch.getTechType().mineralPrice()) {// TODO available gas!
					building.upgrade(upgradeToResearch.getUpgradeType());
				}
			}
		} 

		if (upgradeToResearch != null) {
			if (Main.availableMinerals > upgradeToResearch.getUpgradeType().mineralPrice()) {// TODO available gas!
				building.upgrade(upgradeToResearch.getUpgradeType());
			}
		} else if (techToResearch != null) {
			if (Main.availableMinerals > techToResearch.getTechType().mineralPrice()) {// TODO available gas!
				building.research(techToResearch.getTechType());
			}
		}
	*/
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

	public PriorityQueue<BasePlanItem> getImproveList() {
		return improveList;
	}

	public void setImproveList(PriorityQueue<BasePlanItem> improveList) {
		this.improveList = improveList;
	} 

}
