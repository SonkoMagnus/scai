package scai.elte.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import bwapi.Pair;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import scai.elte.main.Util;

public class UnitManager {
	
	private Unit unit;
	private boolean gotTask;
	private Command actualCommand;
	private Squad squad;
	
	public UnitManager(Unit unit) {
		this.unit=unit;
		this.gotTask=false;
	}
	
	
	//From Yegers
	public Position kiteAway(final Unit unit, final Set<Unit> enemies) {
	    if (enemies.isEmpty()) {
	        return null;
	    }
	    final Position ownPosition = unit.getPosition();
	    final List<Pair<Double, Double>> vectors = new ArrayList<>();

	    double minDistance = Double.MAX_VALUE;
	    for (final Unit enemy : enemies) {
	        final Position enemyPosition = enemy.getPosition();
	        final Pair<Double, Double> unitV = new Pair<>((double)(ownPosition.getX() - enemyPosition.getX()),(double) (ownPosition.getY() - enemyPosition.getY()));
	        final double distance = ownPosition.getDistance(enemyPosition);
	        if (distance < minDistance) {
	            minDistance = distance;
	        }
	        unitV.first = (1/distance) * unitV.first;
	        unitV.second = (1/distance) * unitV.second;
	        vectors.add(unitV);
	    }
	    minDistance = 2 * minDistance * minDistance;
	    for (final Pair<Double, Double> vector : vectors){
	        vector.first *= minDistance;
	        vector.second *= minDistance;
	    }
	    Pair<Double,Double> sumAll = Util.sumPosition(vectors);
	    return Util.sumPosition(ownPosition, new Position((int)(sumAll.first / vectors.size()),(int) (sumAll.second / vectors.size())));
	}
	
    public void trainUnit(Unit producer, UnitType unitType) {
    	if (producer.getTrainingQueue().size() < 1) {
    		producer.train(unitType);
    	}
    }
	//Main "deciding" loop
	public void operate () {
		
	}

	public Unit getUnit() {
		return unit;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public boolean isGotTask() {
		return gotTask;
	}

	public void setGotTask(boolean gotTask) {
		this.gotTask = gotTask;
	}


	public Command getActualCommand() {
		return actualCommand;
	}


	public void setActualCommand(Command actualCommand) {
		this.actualCommand = actualCommand;
	}


	public Squad getSquad() {
		return squad;
	}


	public void setSquad(Squad squad) {
		this.squad = squad;
	}

}
