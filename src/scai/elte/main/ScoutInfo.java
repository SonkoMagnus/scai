package scai.elte.main;

import bwapi.TilePosition;

public class ScoutInfo {

	public enum TileType {NORMAL, BASE_LOC, START_LOC}
	private TilePosition tile;
	private TileType type;
	
	private Integer importance; 
	private boolean walkable;
	//Position threatened by (static) enemy units
	private boolean threatenedByGround = false;
	private boolean threatenedByAir = false;
	
	public ScoutInfo(TilePosition tile, TileType type, Integer importance, boolean walkable) {
		this.tile = tile;
		this.type = type;
		this.importance = importance;
		this.walkable = walkable;
	}
	
	@Override
	public boolean equals(Object obj) {
		System.out.println(tile.equals(((ScoutInfo)obj).getTile()));
		return tile.equals(((ScoutInfo)obj).getTile());
	}
	
	
	public Integer getImportance() {
		return importance;
	}
	public void setImportance(Integer importance) {
		this.importance = importance;
	}
	public boolean isWalkable() {
		return walkable;
	}
	public void setWalkable(boolean walkable) {
		this.walkable = walkable;
	}


	public TileType getType() {
		return type;
	}


	public void setType(TileType type) {
		this.type = type;
	}


	public TilePosition getTile() {
		return tile;
	}


	public void setTile(TilePosition tile) {
		this.tile = tile;
	}

	public boolean isThreatenedByGround() {
		return threatenedByGround;
	}

	public void setThreatenedByGround(boolean threatenedByGround) {
		this.threatenedByGround = threatenedByGround;
	}

	public boolean isThreatenedByAir() {
		return threatenedByAir;
	}

	public void setThreatenedByAir(boolean threatenedByAir) {
		this.threatenedByAir = threatenedByAir;
	}
}

