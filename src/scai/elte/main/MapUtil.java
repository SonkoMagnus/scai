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
	
	//convenience method: Checki if a unit is inside a BWTA region
	public static Set<Unit> getUnitsInRegion (Region region) {
		Set<Unit> unitsInRegion = new HashSet<Unit>();
		for (UnitManager um : Main.unitManagers.values()) {
			Unit unit = um.getUnit();
			
			Position closestBorder = region.getPolygon().getNearestPoint(unit.getPosition());
			
			if (region.getCenter().getDistance(unit.getPosition()) < region.getCenter().getDistance(closestBorder)) {
				unitsInRegion.add(unit);
			}
			//TOD
			region.getPolygon();
		}
		//Set<Unit> enemies = new HashSet<Unit>();
		/*
		for (Unit u :region.getUnits()) {
			if (u.getPlayer() != Main.self) {
				enemies.add(u);
			}
		}
		*/
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
			if (u.getHitPoints() < minHp) {
				minHp = u.getHitPoints();
				weakest = u;
			}
		}
		return weakest;
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
