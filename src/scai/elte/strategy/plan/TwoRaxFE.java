package scai.elte.strategy.plan;

import bwapi.TechType;
import bwapi.TilePosition;
import bwapi.UnitType;
import bwapi.UpgradeType;
import scai.elte.strategy.BuildOrder;
import scai.elte.strategy.TechItem;
import scai.elte.strategy.UpgradeItem;

public class TwoRaxFE extends BuildOrder {
	
	private TilePosition natural;

	public TilePosition getNatural() {
		return natural;
	}

	public void setNatural(TilePosition natural) {
		this.natural = natural;
	}
	
	public TwoRaxFE(TilePosition natural) {
		super();
		
		addItem(UnitType.Terran_Supply_Depot, 18, 1);	
		addItem(UnitType.Terran_Barracks, 22, 1);	
		addItem(UnitType.Terran_Barracks, 26, 1);
		
		addItem(UnitType.Terran_Supply_Depot, 28, 1);
		addItem(UnitType.Terran_Refinery, 36, 1);
		addItem(UnitType.Terran_Academy, 38, 1);
		addItem(UnitType.Terran_Supply_Depot, 48, 1);
		getImproveOrder().add(new TechItem(TechType.Stim_Packs, 52, 3));
		getImproveOrder().add(new UpgradeItem(UpgradeType.U_238_Shells, 52, 2, 1));
		addItem(UnitType.Terran_Comsat_Station, 40, 1);
		
		this.setSupplyExecuted(56);
	}

}



/*
9/10 - Supply Depot
11/18 - Barracks
13/18 - Barracks
14/18 - Supply Depot
18/26 - Refinery
19/26 - Academy
24/26 - Supply Depot
26/34 - Stim Pack
28/34 - Comsat Station*/