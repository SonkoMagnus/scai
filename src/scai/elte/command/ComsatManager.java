package scai.elte.command;

import bwapi.Position;
import bwapi.TechType;
import bwapi.Unit;
import scai.elte.command.Request.RequestType;
import scai.elte.main.Main;

public class ComsatManager extends BuildingManager{
	
	private int lastScanFrame;

	public ComsatManager(Unit unit) {
		super(unit);
	}
	 
	@Override
	public void operate() {
		super.operate();

		for (String rk : Main.requests.keySet()) {
			Request r = Main.requests.get(rk);
			if (r.getRequestStatus() == RequestStatus.NEW && r.getType() == RequestType.COMMAND
					&& r.getRequestedCommand().getType() == CommandType.SCAN) {

				Position scanTarget = r.getRequestedCommand().getTargettilePosition().toPosition();
				for (Position p : Main.scannerPositions.keySet()) {
					if (p.getDistance(scanTarget) <= 360) {
						Main.requests.remove(rk);
					}
				}
				if (Main.requests.containsKey(rk)) {
					getUnit().useTech(TechType.Scanner_Sweep, scanTarget);
					r.setAnsweringUnit(getUnit());
					r.setRequestStatus(RequestStatus.BEING_ANSWERED);
					lastScanFrame = Main.frameCount;
					break;
				} else if (r.getRequestStatus() == RequestStatus.BEING_ANSWERED && r.getType() == RequestType.COMMAND
						&& r.getRequestedCommand().getType() == CommandType.SCAN && r.getAnsweringUnit() == getUnit()) {
					if (Main.frameCount - lastScanFrame > 10) {
						r.setRequestStatus(RequestStatus.FULFILLED);
					}
				}
			}
		}
	}
}
