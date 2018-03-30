package scai.elte.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitCommand;
import bwapi.UnitCommandType;
import bwapi.UnitType;
import bwapi.UpgradeType;
import bwta.BWTA;
import bwta.BaseLocation;
import scai.elte.strategy.BioSquads;
import scai.elte.strategy.BuildOrder;
import scai.elte.strategy.BuildOrderItem;
import scai.elte.strategy.BuildOrderItemStatus;
import sun.misc.UCDecoder;

public class Main extends DefaultBWListener {
	

    private Mirror mirror = new Mirror();

    private Game game;

    private Player self;

    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame(true);
    }


    /**
     * The amount of minerals currently reserved for buildings.
     */
    public int reservedMinerals;

    /**
     * The amount of gas currently reserved for buildings.
     */
    public int reservedGas;
    
    /**
     * Set of all known enemy buildings.
     */
    public HashSet<EnemyPosition> enemyBuildingMemory = new HashSet<EnemyPosition>();

    /**
     * Current number of workers, starting with 4
     */
    public Integer workerCount = 0;
    
    /**
     * Current targeted worker count
     */
    
    public Integer targetWorkers = 12;
    
    /**
     * The maximum number of workers that is to be constructed.
     */
	public int maximumWorkers = 45;
	
	public int frameCount = 0;
	
    /**
     * Keeps track of how many units of each unit type the player has.
     */
    public HashMap<UnitType, Integer> unitCounts = new HashMap<UnitType, Integer>();
    
    /**
     * Keeps track of how many units are in production
     */
    public HashMap<UnitType, Integer> unitsInProduction = new HashMap<UnitType, Integer>();
    
    
    /**
     * Build order, only buildings
     */
    public BuildOrder buildOrder = new BuildOrder();
    
    
    public ArrayList<Unit> gatherers = new ArrayList<Unit>(); //Default activity
    public ArrayList<Unit> builders  = new ArrayList<Unit>();
    
    public int supplyUsedActual;
    //public int supplyTotal - do i need this?
    
    @Override
    public void onUnitCreate(Unit unit) {
        System.out.println("New unit discovered " + unit.getType());
        countAllUnits();
        //Update queue when buildings
    	if (frameCount > 10) {
        for (BuildOrderItem boi : buildOrder.buildOrderList) {
        	if (boi.status == BuildOrderItemStatus.IN_QUEUE) {
        		if (unit.getType() == boi.getUnitType()) {
        			boi.status = BuildOrderItemStatus.UNDER_CONSTRUCTION;
        			System.out.println("Changing "+ boi + " to UNDER_CONSTRUCTION" );
        			reservedMinerals = reservedMinerals - boi.getUnitType().mineralPrice();
        		}
        	}
        }
    	}
    }
    
    

    public void countAllUnits() {
    	unitCounts = new HashMap<UnitType, Integer>();
        unitsInProduction = new HashMap<UnitType, Integer>();
        supplyUsedActual = 0;
    	workerCount =0;
            for (Unit myUnit : self.getUnits()) 
	        {
            	supplyUsedActual += myUnit.getType().supplyRequired();
	        	if(!unitCounts.containsKey(myUnit.getType())) 
	        		unitCounts.put(myUnit.getType(), 1);
	        	else
	        		unitCounts.put(myUnit.getType(), unitCounts.get(myUnit.getType())+1);
	        	
	        	if(myUnit.getType() == UnitType.Terran_SCV) {
	        		workerCount++;
	        	}
	        	//
	        	
	        	if (myUnit.getType().isBuilding()) {
	        		List<UnitType> trimList = myUnit.getTrainingQueue();
	        		
	        		if (trimList.size() > 0 ) {
	        			trimList.remove(0);
	        		}
	        		
	        		for (UnitType ut : trimList) {		
	        			
	        			if(!unitsInProduction.containsKey(ut)) 
	        				unitsInProduction.put(ut, 1);
	        			
	    	        	else
	    	        		unitsInProduction.put(ut, unitsInProduction.get(ut)+1);
	    	        			
	        		}
	        		
	        		
	        		
	        	}
	        	
	        }
     }
    
    
    public int count(UnitType type)
    {
    	Integer result = unitCounts.get(type);
    	if(result == null)
    		return 0;
    	else
    		return result;
    }

    @Override
    public void onStart() {
    	try {
    	System.out.println("START");
        game = mirror.getGame();
        self = game.self();
        buildOrder = new BioSquads();
        //Use BWTA to analyze map
        //This may take a few minutes if the map is processed first time!
        System.out.println("Analyzing map...");

        BWTA.analyze();
        System.out.println("Map data ready");
        
        int i = 0;
        for(BaseLocation baseLocation : BWTA.getBaseLocations()){
        	System.out.println("Base location #" + (++i) + ". Printing location's region polygon:");
        	for(Position position : baseLocation.getRegion().getPolygon().getPoints()){
        		System.out.print(position + ", ");
        	}
        	System.out.println();
        }

    	}
    	catch (Exception ex) {
    		ex.printStackTrace();
    	}
    }

    @Override
    public void onUnitDestroy(Unit unit) {

    //	super.onUnitDestroy(arg0);
    }
    
    @Override
    public void onUnitComplete(Unit unit) {
    	if (frameCount > 10) {
    		countAllUnits();
    		
    	}
    }
    
    public void assignWorker(Unit unit, ArrayList<Unit> group) {
    	boolean contains;
    	if (group == gatherers) {
    		contains = builders.contains(unit);
    		if (contains) builders.remove(unit);
    		contains = gatherers.contains(unit);
    		if (!contains) gatherers.add(unit);
    	}
    	else if (group == builders) {
    		contains = gatherers.contains(unit);
    		if (contains) gatherers.remove(unit);
    		contains = builders.contains(unit);
    		if (!contains) builders.add(unit);
    	}
    	else {
    		//Derp the fuck out, i don't know
    	}
    }
    

    
    @Override
    public void onFrame() {
    	try {
    	frameCount++;
    	countAllUnits();
        //game.setTextSize(10);
        game.drawTextScreen(10, 10, "Playing as " + self.getName() + " - " + self.getRace());

        StringBuilder statusMessages = new StringBuilder("My units:\n");
        Integer availableMinerals = self.minerals() - reservedMinerals;
        statusMessages.append("Available minerals:" + availableMinerals.toString() + "\n");
        statusMessages.append("Reserved minerals:" + reservedMinerals+ "\n");
    //    statusMessages.append("Current worker count:" + workerCount.toString() + "\n");
    //    statusMessages.append("Target worker count:" + targetWorkers.toString() + "\n");
        statusMessages.append("Frame count:" + frameCount + "\n");



	   	int workersInProduction = 0;
    	if (unitsInProduction.get(UnitType.Terran_SCV) != null) {
    		workersInProduction = unitsInProduction.get(UnitType.Terran_SCV);
    	}
    	
    	if (frameCount > 10) {
    		
    		
    		for (Unit b : builders) {	
    			if (b.isIdle() &&b.getLastCommand().getUnitCommandType() != UnitCommandType.Build)
    			{
    				System.out.println("DEBUG_lc:" + b.getLastCommand().getUnitCommandType());
    				//if (b.getLastCommand().)
    				//b.build(b.getLastCommand().getTarget().getType(), b.getLastCommand().getTargetTilePosition());
    			}
    		}
    		
				for (BuildOrderItem boi : buildOrder.buildOrderList) {
					if (boi.getSupplyThreshold() <= supplyUsedActual && boi.status == BuildOrderItemStatus.PLANNED) {
						// System.out.println("DEBUGsupp:" + self.supplyUsed());
						// System.out.println("DEBUGsupp2:" + self.supplyTotal());
						// Reserve minerals
						reservedMinerals = reservedMinerals + boi.getUnitType().mineralPrice();
						boi.status = BuildOrderItemStatus.IN_QUEUE;
						System.out.println(
								"Reserving" + boi.getUnitType().mineralPrice() + " minerals for:" + boi.getUnitType());
					}
					//Periodically check for false positives - this can occur after false positives
					if (frameCount % 250 == 0 && boi.status == BuildOrderItemStatus.IN_QUEUE && boi.gotBuilder) {
						boi.gotBuilder = false;
					}

					if (boi.status == BuildOrderItemStatus.IN_QUEUE	&& self.minerals() >= boi.getUnitType().mineralPrice()) {
						// boolean gotBuilder = false;
						if (!boi.gotBuilder) {
							Unit builder = null;
							for (Unit b : builders) {
								if (!b.isConstructing()	|| b.getLastCommand().getUnitCommandType() != UnitCommandType.Build) {
									builder = b;
									boi.gotBuilder = true;
									break;
								}
							}

							if (!boi.gotBuilder) {
								Unit mb = gatherers.get(0);
								assignWorker(mb, builders);
							}
							
							if (builder !=null ) {
							System.out.println("Builder id:" + builder.getID());
							// get a nice place to build
							TilePosition buildTile = getBuildTile(builder, boi.getUnitType(),
									self.getStartLocation());

							if (buildTile != null) {
								builder.build(boi.getUnitType(), buildTile);
								System.out.println("CHanging " + boi.getUnitType() + " builder status to true");
								boi.gotBuilder = true;
							}
							}

						}
					}
				}
			}
    	
    	
    	
      	
      	
      	
      	

        for (Unit myUnit : self.getUnits()) {
        	int b = workerCount + workersInProduction;
            
        	
            //if there's enough minerals, train an SCV
            if (myUnit.getType() == UnitType.Terran_Command_Center && availableMinerals >= 50 && b < targetWorkers) {
            	/*
            	 System.out.println("DEBUG_w:" + workers);
            	 System.out.println("DEBUG_wp:" + workersInProduction);
            	 System.out.println("DEBUG_f:" + (workers+workersInProduction));
                */
                myUnit.train(UnitType.Terran_SCV);
                //System.out.println(myUnit.getTrainingQueue());
                
            }
            
            if (myUnit.getType() == UnitType.Terran_Barracks && availableMinerals >=50) {
            	myUnit.train(UnitType.Terran_Marine);
            }
      //      myUnit.getTrainingQueue()
            
            
            //if it's a worker and it's idle, send it to the closest mineral patch
            if (myUnit.getType().isWorker() && myUnit.isIdle()) {
                Unit closestMineral = null;

                //find the closest mineral
                for (Unit neutralUnit : game.neutral().getUnits()) {
                    if (neutralUnit.getType().isMineralField()) {
                        if (closestMineral == null || myUnit.getDistance(neutralUnit) < myUnit.getDistance(closestMineral)) {
                            closestMineral = neutralUnit;
                        }
                    }
                }

                //if a mineral patch was found, send the worker to gather it
                if (closestMineral != null) {
                    myUnit.gather(closestMineral, false);
                }
                assignWorker(myUnit, gatherers);
               
                
            }
            
            
            /*
            if (workers >= targetWorkers && (self.minerals() >= 100)) {
            	//iterate over units to find a worker
            	for (Unit unit : self.getUnits()) {
            		if (unit.getType() == UnitType.Terran_SCV) {
            			//get a nice place to build a supply depot
            			TilePosition buildTile =
            				getBuildTile(unit, UnitType.Terran_Supply_Depot, self.getStartLocation());
            			//and, if found, send the worker to build it (and leave others alone - break;)
            			if (buildTile != null) {
            				unit.build(UnitType.Terran_Supply_Depot, buildTile);
            				break;
            			}
            		}
            	}
            }
                        */
            /*
            if (myUnit.getType().isWorker()) {
            	myUnit.build(arg0, arg1)
            }
            */
        }
        /*
        for (UnitType m : unitsInProduction.keySet()) {
        	//System.out.println("Prod:");
        	//System.out.print(m + " ");
        }
        */
      //  System.out.println(availableMinerals);
        //draw my units on screen
       // System.out.println(unitsInProduction);
        game.drawTextScreen(10, 25, statusMessages.toString());
        //game.drawTextScreen(10, 25, availableMinerals.toString());
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    }

    public static void main(String[] args) {
        new Main().run();
    }
    
    
    public TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile) {
    	TilePosition ret = null;
    	int maxDist = 3;
    	int stopDist = 40;

    	// Refinery, Assimilator, Extractor
    	if (buildingType.isRefinery()) {
    		for (Unit n : game.neutral().getUnits()) {
    			if ((n.getType() == UnitType.Resource_Vespene_Geyser) &&
    					( Math.abs(n.getTilePosition().getX() - aroundTile.getX()) < stopDist ) &&
    					( Math.abs(n.getTilePosition().getY() - aroundTile.getY()) < stopDist )
    					) return n.getTilePosition();
    		}
    	}

    	while ((maxDist < stopDist) && (ret == null)) {
    		for (int i=aroundTile.getX()-maxDist; i<=aroundTile.getX()+maxDist; i++) {
    			for (int j=aroundTile.getY()-maxDist; j<=aroundTile.getY()+maxDist; j++) {
    				if (game.canBuildHere(new TilePosition(i,j), buildingType, builder, false)) {
    					// units that are blocking the tile
    					boolean unitsInWay = false;
    					for (Unit u : game.getAllUnits()) {
    						if (u.getID() == builder.getID()) continue;
    						if ((Math.abs(u.getTilePosition().getX()-i) < 4) && (Math.abs(u.getTilePosition().getY()-j) < 4)) unitsInWay = true;
    					}
    					if (!unitsInWay) {
    						return new TilePosition(i, j);
    					}
    					// creep for Zerg
    					if (buildingType.requiresCreep()) {
    						boolean creepMissing = false;
    						for (int k=i; k<=i+buildingType.tileWidth(); k++) {
    							for (int l=j; l<=j+buildingType.tileHeight(); l++) {
    								if (!game.hasCreep(k, l)) creepMissing = true;
    								break;
    							}
    						}
    						if (creepMissing) continue;
    					}
    				}
    			}
    		}
    		maxDist += 2;
    	}

    	if (ret == null) game.printf("Unable to find suitable build position for "+buildingType.toString());
    	return ret;
    }
    
}	