package scai.elte.main;

import bwapi.Position;
import bwapi.UnitType;

public class EnemyPosition {
	
	private Position position;
	private UnitType type;
	
	public EnemyPosition(Position position, UnitType type) {
		this.position = position;
		this.type = type;		
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public UnitType getType() {
		return type;
	}

	public void setType(UnitType type) {
		this.type = type;
	}

}
