package scai.elte.command;

import bwapi.Unit;

public class Request {
	
	private Unit requestingUnit;
	private Command requestedCommand;
	private RequestStatus requestStatus;
	private Unit answeringUnit;


	public Request(Unit unit, Command command) {
		this.requestingUnit=unit;
		this.requestedCommand=command;
		this.setRequestStatus(RequestStatus.NEW);
	}
	
	public Unit getRequestingUnit() {
		return requestingUnit;
	}
	public void setRequestingUnit(Unit requestingUnit) {
		this.requestingUnit = requestingUnit;
	}
	public Command getRequestedCommand() {
		return requestedCommand;
	}
	public void setRequestedCommand(Command requestedCommand) {
		this.requestedCommand = requestedCommand;
	}


	public RequestStatus getRequestStatus() {
		return requestStatus;
	}


	public void setRequestStatus(RequestStatus requestStatus) {
		this.requestStatus = requestStatus;
	}


	public Unit getAnsweringUnit() {
		return answeringUnit;
	}


	public void setAnsweringUnit(Unit answeringUnit) {
		this.answeringUnit = answeringUnit;
	}	
	
}
