package scai.elte.command;

import java.util.PriorityQueue;

import bwapi.Unit;
import scai.elte.command.Request.RequestType;
import scai.elte.main.Main;
import scai.elte.strategy.BasePlanItem;
import scai.elte.strategy.BasePlanItemComparator;
import scai.elte.strategy.TechItem;
import scai.elte.strategy.UpgradeItem;

public class BuildingManager extends UnitManager {

	private PriorityQueue<BasePlanItem> improveList = new PriorityQueue<BasePlanItem>(1,new BasePlanItemComparator());
	
	public BuildingManager(Unit unit) {
		super(unit);
	}

	@Override
	public void operate() {
		// If building is under attack, issue a defend request
		requestDefenseIfNeeded();
		researchFromQueue();
	}
	
	public void researchFromQueue() {
		BasePlanItem bpi = improveList.poll();
		if (bpi != null) {
			if (bpi instanceof TechItem) {
				 getUnit().research(((TechItem) bpi).getTechType());
				 System.out.println("Researching:" + ((TechItem) bpi).getTechType());
			} 
			if (bpi instanceof UpgradeItem) {
				getUnit().upgrade(((UpgradeItem) bpi).getUpgradeType());
				System.out.println("Upgrading:" +((UpgradeItem) bpi).getUpgradeType());
			} 
		}
	}
	
	public void requestDefenseIfNeeded() {
		Unit building = getUnit();
		if (building.isUnderAttack()) {			
			Main.requests.putIfAbsent(building.getID()+"D", new Request(getUnit(), null, RequestType.DEFEND));	
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
