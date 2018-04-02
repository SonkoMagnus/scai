package scai.elte.command;

import bwapi.Unit;
import scai.elte.main.Main;

public class BunkerManager extends UnitManager {

	public BunkerManager(Unit unit) {
		super(unit);
	}
	
	@Override
	public void operate () {
	if(!getUnit().isBeingConstructed()) {
		if (getUnit().getLoadedUnits().size() <4) {
			//System.out.println("OPERATIN BUNKER...");
			//Issue a bunker manning request
		
			for (int i=getUnit().getLoadedUnits().size();i<4;i++) {
				Command c = new Command(CommandType.MAN_BUNKER);
				String id = getUnit().getID() + "_" + i;
				Request req = new Request(getUnit(), c);
				if (!Main.requests.containsKey(id)) {
					System.out.println("Bunkermanager issuing man bunker request with id:" + id);
					Main.requests.put(id, req);
				};
			}
		}
	}
	}

}
