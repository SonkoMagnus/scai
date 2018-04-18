package scai.elte.main;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import bwapi.Position;
import bwapi.Unit;
import bwta.Region;
import scai.elte.command.UnitManager;

//Utility class for location items in the map
public class MapUtil {
	

	public static Set<Unit> getUnitsInRegion (Region region) {
		return getUnitsInRegion(region, false);
	}
	
	public static Set<Unit> getUnitsInRegion (Region region, boolean onlyEnemies) {
		Set<Unit> unitsInRegion = new HashSet<Unit>();
		for (UnitManager um : Main.unitManagers.values()) {
			Unit unit = um.getUnit();
			
			Position closestBorder = region.getPolygon().getNearestPoint(unit.getPosition());
			
			if (region.getCenter().getDistance(unit.getPosition()) < region.getCenter().getDistance(closestBorder)) {
				unitsInRegion.add(unit);
			}
		}
		return unitsInRegion;
	}
	
	public static Region getRegionOfUnit(Unit unit){
		Region unitRegion = null;
		for (Region r : Main.baseRegions) {
			Position closestBorder = r.getPolygon().getNearestPoint(unit.getPosition());
			
			if (r.getCenter().getDistance(unit.getPosition()) < r.getCenter().getDistance(closestBorder)) {
				unitRegion = r;
			}
		}
		return unitRegion;
	}
	
	
	public static Unit getNearestUnit(Collection<Unit> units, Unit unit) {
		int dist = 0;
		int minDist = Integer.MAX_VALUE;
		Unit nearestUnit = null;
		for (Unit u : units) {
			dist = unit.getDistance(u);
			if (dist<minDist) {
				minDist = dist;
				nearestUnit = u;
			}
		}
		
		return nearestUnit;
	}
	
	public static Unit getWeakestUnit(Collection<Unit> units) {
		int minHp = Integer.MAX_VALUE;
		Unit weakest = null;
		for (Unit u : units) {
			System.out.println(u.getHitPoints());
			if (u.getHitPoints() < minHp) {
				minHp = u.getHitPoints();
				weakest = u;
			}
		}
		System.out.println("weakest" + weakest);
		return weakest;
	}
	
	
	//Costly method
	public static Set<Position> getPositionsInRadius(Position point, int radius) {
		int x = point.getX();
		int y = point.getY();
		HashSet<Position> positionsInRadius = new HashSet<Position>();
		for  (int i = x-radius; i<=x+radius; i++) {
			for (int j = y-radius; j<=y+radius ; j++) {
				double dist = Math.sqrt((double)((Math.abs(x-i))^2 + Math.abs(y-j)^2));
				if (dist <=radius) {
					Position check = new Position(i,j);
					positionsInRadius.add(check);
				};
			}
			
		}
		return positionsInRadius;
	}
	
	/*
	public static Unit getNearestAttackableUnit(Collection<Unit> units, Unit attacker) {
		int dist = 0;
		int minDist = Integer.MAX_VALUE;
		Unit nearestUnit = null;
		for (Unit u : units) {
			dist = attacker.getDistance(u);
			if (dist<minDist) {
				minDist = dist;

				nearestUnit = u;
			}
		}
		System.out.println(units.size());
		System.out.println("nearest unit:" + nearestUnit);
		return nearestUnit;
	}
	*/
	/*
	if (!Main.enemyUnits.isEmpty()) {
		int dist = 0;
		int minDist = Integer.MAX_VALUE;
		Unit nearestEnemy = null;
		for (Unit enemy : Main.enemyUnits) {
			dist = getUnit().getDistance(enemy);
			if (dist<minDist) {
				minDist = dist;
				if (marine.canAttack(enemy)) {
					nearestEnemy = enemy;
				}
			}
		}
*/
}
