package scai.elte.command;

import bwapi.Unit;
import scai.elte.main.Main;

public class BunkerManager extends BuildingManager {

	public BunkerManager(Unit unit) {
		super(unit);
	}
	
	@Override
	public void operate () {
	if(getUnit().isCompleted()) {
		if (getUnit().getLoadedUnits().size() <4) {
			if (getUnit().getLoadedUnits().size() == 0) {
				requestDefenseIfNeeded();
			}
			for (int i=getUnit().getLoadedUnits().size();i<4;i++) {
				Command c = new Command(CommandType.MAN_BUNKER);
				String id = getUnit().getID() + "_" + i;
				Request req = new Request(getUnit(), c);
				if (!Main.requests.containsKey(id)) {
					Main.requests.putIfAbsent(id, req);
				};
		
		} 
	}
	} else {
	    String myId = Integer.toString(getUnit().getID());
		for (String n : Main.requests.keySet()) {
			if (n.substring(0,myId.length()-1).equals(myId) ) {
				Main.requests.get(n).setRequestStatus(RequestStatus.FULFILLED);
			}
			
		}
	}
	
	}

}
