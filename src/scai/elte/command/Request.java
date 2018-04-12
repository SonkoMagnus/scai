package scai.elte.command;

import bwapi.Unit;

public class Request {
	
	private Unit requestingUnit;
	private Command requestedCommand;
	private RequestStatus requestStatus;
	private Unit answeringUnit;
	private RequestType type;

	//COMMAND: Specific command, DEFEND: "defend me"
	public enum RequestType {
		COMMAND, DEFEND
	}
	
	public Request(Unit unit, Command command) {
		this.requestingUnit=unit;
		this.requestedCommand=command;
		this.setRequestStatus(RequestStatus.NEW);
		this.setType(RequestType.COMMAND);
	}
	
	public Request(Unit unit, Command command, RequestType type) {
		this.requestingUnit=unit;
		this.requestedCommand=command;
		this.setRequestStatus(RequestStatus.NEW);
		this.setType(type);
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

	public RequestType getType() {
		return type;
	}

	public void setType(RequestType type) {
		this.type = type;
	}	
	
}
