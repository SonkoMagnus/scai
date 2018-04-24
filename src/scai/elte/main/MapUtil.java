package scai.elte.main;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwta.Region;
import scai.elte.command.UnitManager;

//Utility class for location items in the map
public class MapUtil {
	

	
	public static Set<Unit> getUnitsInRegion (Region region) {
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
	
	public static Region getRegionOfTile(TilePosition tile){
		Region tileRegion = null;
		for (Region r : Main.baseRegions) {
			Position closestBorder = r.getPolygon().getNearestPoint(tile.toPosition());
			
			if (r.getCenter().getDistance(tile.toPosition()) < r.getCenter().getDistance(closestBorder)) {
				tileRegion = r;
			}
		}
		return tileRegion;
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
	
	//"Cautious" method
	public static Set<TilePosition> getTilePositionsInRadius (Position point, double radius) {
		Set<TilePosition> tiles = new HashSet<TilePosition>();
		
		int x = point.getX();
		int y = point.getY();
		
		int rt = (int) Math.ceil(radius /32);
		int xt = x/32;
		int yt = y/32;
		
		for  (int i = xt-rt; i<=xt+rt; i++) {
			for (int j = yt-rt; j<=yt+rt ; j++) {
				TilePosition tp = new TilePosition(i, j);
				tiles.add(tp);
			}
		}
		
		return tiles;
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
