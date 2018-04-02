package scai.elte.command;

import bwapi.Unit;
import scai.elte.main.Main;

public class MarineManager extends UnitManager {

	public MarineManager(Unit unit) {
		super(unit);
	}

	@Override
	public void operate() {

		
		if ( getUnit().isCompleted() && !this.isGotTask()) {
			System.out.println("mmmmmmmmm");
			for (Request r : Main.requests.values()) {
				System.out.println("BLEP");
				if (getUnit().getTransport() == null) { //Workaround, as unit.isLoaded doesn't seem to work
					if (r.getRequestedCommand().getType() == CommandType.MAN_BUNKER && r.getRequestStatus() == RequestStatus.NEW
						&& r.getAnsweringUnit() == null) {
						r.setRequestStatus(RequestStatus.BEING_ANSWERED);
						System.out.println("Answering request:" + r.getAnsweringUnit());
						System.out.println("ID:" + getUnit().getID() + " Marine manning bunker...");
						System.out.println("ID:" + getUnit().getID() + " Transport:" + getUnit().getTransport());
						r.setAnsweringUnit(getUnit());
						r.getRequestingUnit().load(getUnit());
						this.setGotTask(true);
						break;
					}
					if (r.getRequestedCommand().getType() == CommandType.MAN_BUNKER && r.getRequestStatus() == RequestStatus.BEING_ANSWERED 
							&& r.getAnsweringUnit().getID() == getUnit().getID()) {
						//System.out.println("ID:" + getUnit().getID() + " Answering bunker request...");
						//Spam command until fulfilled
						
						//System.out.println("ID:" + getUnit().getID()+ " IS_LOADED:" + getUnit().isLoaded());
						r.getRequestingUnit().load(getUnit());
					}
				}
			}
		}
	}
		

}
