package scai.elte.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitCommandType;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;
import scai.elte.command.BunkerManager;
import scai.elte.command.Command;
import scai.elte.command.CommandType;
import scai.elte.command.MarineManager;
import scai.elte.command.Request;
import scai.elte.command.RequestStatus;
import scai.elte.command.UnitManager;
import scai.elte.strategy.BioSquads;
import scai.elte.strategy.BuildOrder;
import scai.elte.strategy.BuildOrderItem;
import scai.elte.strategy.BuildOrderItemStatus;

public class Main extends DefaultBWListener {
	
	public boolean DEBUGMODE = true;

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
    public int reservedMineralsInQueue;
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
    
    public HashMap<Integer, UnitManager> unitManagers = new HashMap<Integer, UnitManager>();
    
    Random rand;
    
    public ArrayList<Unit> mineralWorkers = new ArrayList<Unit>(); //Default activity
    public ArrayList<Unit> gasWorkers = new ArrayList<Unit>(); //Default activity
    public ArrayList<Unit> builders  = new ArrayList<Unit>();
    
    public int supplyUsedActual;
    //public int supplyTotal - do i need this?
    
    Set<TilePosition> plannedPositions = new HashSet<TilePosition>();
    public static ConcurrentHashMap<String, Request> requests = new ConcurrentHashMap<String, Request>();
    
    public static ArrayList<Unit> enemyUnits = new ArrayList<Unit>();
    Set<BaseLocation> baseLocations = new HashSet<BaseLocation>();
    BaseLocation home;
    BaseLocation naturalExpansion;
    
    
    
    
    @Override
    public void onUnitCreate(Unit unit) {
      //  System.out.println("New unit discovered " + unit.getType());
        if (unit.getType() != UnitType.Resource_Mineral_Field && unit.getType() != UnitType.Resource_Vespene_Geyser && unit.getType() != UnitType.Unknown) {
        	if (unit.getType() == UnitType.Terran_Bunker) {
        		unitManagers.put(unit.getID(), new BunkerManager(unit));
        	} else if (unit.getType() == UnitType.Terran_Marine) {
        		unitManagers.put(unit.getID(), new MarineManager(unit));
        	} else {
        		unitManagers.put(unit.getID(), new UnitManager(unit));
        	}
        }
        

        countAllUnits();
        for (BuildOrderItem boi : buildOrder.buildOrderList) {
        	if (boi.status == BuildOrderItemStatus.BUILD_PROCESS_STARTED) {
        		if (unit.getType() == boi.getUnitType()) {
        			boi.status = BuildOrderItemStatus.UNDER_CONSTRUCTION;
//        			if (DEBUGMODE)	System.out.println("Changing "+ boi.getUnitType() + " to UNDER_CONSTRUCTION" );
        			reservedMinerals = reservedMinerals - boi.getUnitType().mineralPrice();
                	break;
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
    public void onUnitMorph(Unit unit) {
    	super.onUnitMorph(unit);
        for (BuildOrderItem boi : buildOrder.buildOrderList) {
        	if (boi.status == BuildOrderItemStatus.BUILD_PROCESS_STARTED) {
        		if (unit.getType() == boi.getUnitType()) {
        			boi.status = BuildOrderItemStatus.UNDER_CONSTRUCTION;
//        if (DEBUGMODE)	System.out.println("Changing "+ boi.getUnitType() + " to UNDER_CONSTRUCTION" );
        			reservedMineralsInQueue = reservedMineralsInQueue - boi.getUnitType().mineralPrice();
        			reservedMinerals = reservedMinerals - boi.getUnitType().mineralPrice();
        		}
        	}
        }
    	
    }
    


    @Override
    public void onStart() {
    	try {
   
    	rand = new Random();
//    	System.out.println("START");
        game = mirror.getGame();
        self = game.self();
        buildOrder = new BioSquads();
     	game.setLocalSpeed(5);
     	game.enableFlag(1); //does not seem to work
     	
     	
     	countAllUnits();
     	for (Unit unit : self.getUnits()) {
     		if (unit.getType() == UnitType.Terran_SCV) {
     			assignWorker(unit, mineralWorkers);
     		}
     	}
        //Use BWTA to analyze map
        //This may take a few minutes if the map is processed first time!
        //System.out.println("Analyzing map...");

        BWTA.analyze();
        //System.out.println("Map data ready");
        /*
        for (Unit u : self.getUnits()) {
        	unitManagers.put(u.getID(), new UnitManager(u)); 
        }
        */
        int i = 0;
        for(BaseLocation baseLocation : BWTA.getBaseLocations()){
        	System.out.println("Base location #" + (++i) + ". Printing location's region polygon:");
        	baseLocations.add(baseLocation);
        	if (baseLocation.isStartLocation()) {
        		home = baseLocation;
        	}
        	for(Position position : baseLocation.getRegion().getPolygon().getPoints()){
        		System.out.print(position + ", ");
        	}
        	System.out.println();
        }
        
     
        double maxDist = 0;
        for (BaseLocation bl : baseLocations) {
        	if (home.getGroundDistance(bl) > maxDist) {
        		maxDist = home.getGroundDistance(bl);
        		naturalExpansion = bl;
        	}
        }
        

    	}
    	catch (Exception ex) {
    		ex.printStackTrace();
    	}
    }

    @Override
    public void onUnitDestroy(Unit unit) {
    	unitManagers.remove(unit.getID(), new UnitManager(unit));
    }
    
    @Override
    public void onUnitComplete(Unit unit) {
    	if (frameCount > 10) {
    		countAllUnits();
    	}
    	
        for (BuildOrderItem boi : buildOrder.buildOrderList) {
        	if (boi.status == BuildOrderItemStatus.UNDER_CONSTRUCTION) {
        		if (unit.getType() == boi.getUnitType()) {
        			boi.status = BuildOrderItemStatus.DONE;
				    int i = unit.getTilePosition().getX();
				    int j = unit.getTilePosition().getY();
        			for (int th=0; th<unit.getType() .tileHeight();th++) {
					    for (int tw=0; tw<unit.getType() .tileWidth();tw++) {    	
					    	TilePosition d = new TilePosition(i+tw, j+th);
					    	plannedPositions.remove(d);
					    }
				    }
        		}
        	}
        }
        
        if (unit.getType() == UnitType.Terran_Refinery) {
        	requests.put(unit.getID() + "_1", new Request(unit, new Command(CommandType.GAS_WORKER)));
        	requests.put(unit.getID() + "_2", new Request(unit, new Command(CommandType.GAS_WORKER)));
        }
    	
    

   
    }
    //TODO rework generic
    public void assignWorker(Unit unit, ArrayList<Unit> group) {
    	System.out.print("Assigning worker " +unit.getID() + " to unit " );
    	boolean contains;
    	
    	if (group == mineralWorkers) {
    		System.out.println(" mineralworkers");
    		contains = builders.contains(unit);
    		if (contains) builders.remove(unit);
    		contains = gasWorkers.contains(unit);
    		if (contains) gasWorkers.remove(unit);
    		contains = mineralWorkers.contains(unit);
    		if (!contains) mineralWorkers.add(unit);
    	}
    	else if (group == builders) {
    		System.out.println(" builders");
    		contains = mineralWorkers.contains(unit);
    		if (contains) mineralWorkers.remove(unit);
    		contains = gasWorkers.contains(unit);
    		if (contains) gasWorkers.remove(unit);    		
    		contains = builders.contains(unit);
    		if (!contains) builders.add(unit);
    	}
    	else if (group == gasWorkers){
    		System.out.println(" gasworkers");
    		contains = mineralWorkers.contains(unit);
    		if (contains) mineralWorkers.remove(unit);
    		contains = builders.contains(unit);
    		if (contains) builders.remove(unit);
    		contains = gasWorkers.contains(unit);
    		if (!contains) gasWorkers.add(unit);
    	}
    }

    public void trainUnit(Unit producer, UnitType unitType) {
    	if (producer.getTrainingQueue().size() < 1) {
    		producer.train(unitType);
    	}
    	
    }
    
    @Override
    public void onUnitShow(Unit unit) {

    	if (unit.getPlayer() != self && !unit.getType().isNeutral()) {
    	System.out.println("OMG IT'S A ENEMY"+ unit.getType()  + " IT HAZ A ID:" + unit.getID());
    	game.setLocalSpeed(50);
    	enemyUnits.add(unit);
    	System.out.println("Frame:" + frameCount);
    	}
    	
    	
    	
    }
    
    @Override
    public void onUnitHide(Unit unit) {
    	if (unit.getPlayer() != self && !unit.getType().isNeutral()) {
        	System.out.println("ENEMY "+ unit.getType() + " DIEDED"   + " IT HAZ A ID:" + unit.getID());
        	enemyUnits.remove(unit);
    	}
    }
    
    @Override
    public void onUnitDiscover(Unit unit) {
    	
    	//if unit.getPlayer() //ENemy unit management
    	
    }
    
    //Evaluates if a supply depot is needed to be constructed RIGHT NOW, based on production times - not sure if good approach
/*
    public boolean supplyEnoughForProd() {
    	UnitType.Terran_Supply_Depot.buildTime();
    	for (Integer k : unitsInProduction.values()) {
    		
    	}
    	return false;
    }
    */
    
    @Override
    public void onFrame() {
    	try {
    	frameCount++;
    	countAllUnits();
        //game.setTextSize(10);
        //game.drawTextScreen(10, 10, "Playing as " + self.getName() + " - " + self.getRace());
        
        StringBuilder statusMessages = new StringBuilder();
        Integer availableMinerals = self.minerals() - reservedMinerals;
        statusMessages.append("Available minerals:" + availableMinerals.toString() + "\n");
        statusMessages.append("Frame count:" + frameCount + "\n");
        statusMessages.append("Units:" + self.getUnits().size() + " Managers:" + unitManagers.size() + "\n");
        statusMessages.append("Requests:" + requests.size());
        
        

	   	int workersInProduction = 0;
    	if (unitsInProduction.get(UnitType.Terran_SCV) != null) {
    		workersInProduction = unitsInProduction.get(UnitType.Terran_SCV);
    	}
    	
    	//requests.putIfAbsent(key, value)
    	if (buildOrder.getSupplyExecuted() <= supplyUsedActual && supplyUsedActual >= self.supplyTotal()-2) {
    		Command bc = new Command(CommandType.BUILD, null, UnitType.Terran_Supply_Depot);
    		requests.putIfAbsent("supplyExtend", new Request(null, bc));
    		//System.out.println("EXTEND REQUESTED");
    		//System.out.println ("ACTUAL:" + supplyUsedActual + " buildorder:" + buildOrder.getSupplyExecuted());
    	} else {
    		requests.remove("supplyExtend");
    	}
    	
    	if (requests.get("supplyExtend") != null && requests.get("supplyExtend").getRequestStatus() == RequestStatus.NEW) {
    		buildOrder.addItem(UnitType.Terran_Supply_Depot, supplyUsedActual, 1);
    		requests.get("supplyExtend").setRequestStatus(RequestStatus.BEING_ANSWERED);

    	}
    	
    	if (workerCount > 1) {	
    			for (BuildOrderItem boi : buildOrder.buildOrderList) {
					if (boi.getSupplyThreshold() <= supplyUsedActual && boi.status == BuildOrderItemStatus.PLANNED) {
						// Reserve minerals
						reservedMinerals = reservedMinerals + boi.getUnitType().mineralPrice();
						boi.status = BuildOrderItemStatus.IN_QUEUE;
			//			System.out.println(	"Reserving" + boi.getUnitType().mineralPrice() + " minerals for:" + boi.getUnitType());
					}
					//Periodically check for false positives - this can occur after failed builds
					
					if (frameCount % 250 == 0 && boi.status == BuildOrderItemStatus.IN_QUEUE && boi.gotBuilder) {
						boi.gotBuilder = false;
					}
					

					if (boi.status == BuildOrderItemStatus.IN_QUEUE	&& (self.minerals()-reservedMineralsInQueue) >= boi.getUnitType().mineralPrice() &&	game.canMake(boi.getUnitType())) {
						

						if (!boi.gotBuilder) {
							Unit builder = null;
							for (Unit b : builders) {								
								if (!b.isConstructing() && !b.getLastCommand().getUnitCommandType().equals(UnitCommandType.Build) ) {	
									builder = b;
									boi.gotBuilder = true;
									break;
								}
							}
							
							if (!boi.gotBuilder) {
								Unit mb = mineralWorkers.get(rand.nextInt(mineralWorkers.size()));
								assignWorker(mb, builders);
							} 							
							
							if (boi.getTilePosition() == null)
							{
								TilePosition buildTile = null;
								if (builder !=null ) {
								// get a nice place to build
								buildTile = getBuildTile(builder, boi.getUnitType(),
										self.getStartLocation());
								}
								boi.setTilePosition(buildTile);
							}
							
							if (boi.getTilePosition() != null && boi.gotBuilder) {
								builder.build(boi.getUnitType(), boi.getTilePosition());
		/*
								System.out.println("--------------------------------------------Builder id:" + builder.getID()
								+  " command is to build " + boi.getUnitType()
								+ "in "+ boi.getTilePosition().getX() + " " + boi.getTilePosition().getY()
								);
			*/
								boi.gotBuilder = true;
								//System.out.print("DEBUG:(build loop) changing reserved minerals from" + reservedMineralsInQueue + " to ");
								reservedMineralsInQueue += boi.getUnitType().mineralPrice();
			        			//System.out.println(reservedMineralsInQueue);
			        			boi.status=BuildOrderItemStatus.BUILD_PROCESS_STARTED;
			        			break;					
							}
							
					}
					}
				}
			}

    	
    	//The UnitManager loop will eventually replace the myUnit loopx
    	for (Integer i : unitManagers.keySet()) {
    		unitManagers.get(i).operate();
    	}
      	
      	
    	for (Unit worker : mineralWorkers) {
    		if (worker.isIdle()) {
    		Unit closestMineral = null;
			// find the closest mineral
			for (Unit neutralUnit : game.neutral().getUnits()) {
				if (neutralUnit.getType().isMineralField()) {
					if (closestMineral == null
							|| worker.getDistance(neutralUnit) < worker.getDistance(closestMineral)) {
						closestMineral = neutralUnit;
					}
				}
			}

			// if a mineral patch was found, send the worker to gather it
			if (closestMineral != null) {
				worker.gather(closestMineral, false);
			}
    		}
    	}
    	
    	
    	for (Unit worker : gasWorkers) {
		    for (Request r : requests.values()) {
		    	if (r.getRequestedCommand().getType() == CommandType.GAS_WORKER 
		    			&& r.getRequestStatus() == RequestStatus.BEING_ANSWERED
		    			&& r.getAnsweringUnit().getID() == worker.getID()
		    			) {
		    		if (worker.isCarryingGas()) {
		    			//Fulfilled
		    			r.setRequestStatus(RequestStatus.FULFILLED);
		    		} else {
		    			if (!worker.isGatheringGas()) {
		    			worker.gather(r.getRequestingUnit());
		    			}
		    		
		    		}
		    	}
		    }
    		
    	}
      	

        for (Unit myUnit : self.getUnits()) {
        	//game.drawTextMap(myUnit.getPosition().getX(), myUnit.getPosition().getY(), "ID:" + myUnit.getID());
        	//game.drawTextMap(myUnit.getPosition().getX(), myUnit.getPosition().getY(), myUnit.getOrder().toString());
        	//game.drawLineMap(myUnit.getPosition().getX(), myUnit.getPosition().getY(), myUnit.getOrderTargetPosition().getX(), 
        		//	 myUnit.getOrderTargetPosition().getY(), bwapi.Color.Black);
        	int b = workerCount + workersInProduction;
            //if there's enough minerals, train an SCV
            if (myUnit.getType() == UnitType.Terran_Command_Center && availableMinerals >= 50 && b < targetWorkers) {
            	trainUnit(myUnit, UnitType.Terran_SCV); 
            }
            
            if (myUnit.getType() == UnitType.Terran_Barracks && availableMinerals >=50) {
            	trainUnit(myUnit, UnitType.Terran_Marine);
            }
			if (myUnit.getType().isWorker() && myUnit.isCompleted()) {
				if (myUnit.isIdle()) {
					assignWorker(myUnit, mineralWorkers);						
				} 
			}

        }
        for (Request r : requests.values()) {
        	if (r.getRequestStatus() == RequestStatus.FULFILLED) {
        		requests.remove(r);
        	}
        	if (r.getRequestedCommand().getType() == CommandType.GAS_WORKER && r.getRequestStatus() == RequestStatus.NEW) {
        		//Random gatherer, closest one would be best //TODO closest unit of type
        		Unit gasWorker = mineralWorkers.get(rand.nextInt(mineralWorkers.size()));
        		assignWorker(gasWorker, gasWorkers);
        		r.setAnsweringUnit(gasWorker);
        		r.setRequestStatus(RequestStatus.BEING_ANSWERED);
        	}
        }
        

        
        game.drawTextScreen(10, 25, statusMessages.toString());
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
    				TilePosition targetTile = new TilePosition(i,j);
    		
    			//	System.out.println("DEBUG containstile_outer: +" + plannedPositions.contains(targetTile));
    				
    				if (game.canBuildHere(new TilePosition(i,j), buildingType, builder, false) && !plannedPositions.contains(targetTile)) {
        		//		System.out.println("DEBUG containstile_inner: +" + plannedPositions.contains(targetTile));
    					// units that are blocking the tile
    					boolean unitsInWay = false;
    					for (Unit u : game.getAllUnits()) {
    						if (u.getID() == builder.getID()) continue;
    						if ((Math.abs(u.getTilePosition().getX()-i) < 4) && (Math.abs(u.getTilePosition().getY()-j) < 4)) unitsInWay = true;
    					}
    					if (!unitsInWay) {
    						ret = new TilePosition(i, j);
    						
  //  						System.out.println("Adding planned position:" + ret); 
    						//System.out.println("DEBUG typedim:" + buildingType +  " DOWN:" + buildingType.dimensionDown() + " LEFT:" + buildingType.dimensionLeft() + " RIght" + buildingType.dimensionRight() + " UP"  + buildingType.dimensionUp());
//    						System.out.println("DEBUG tilesize:" + buildingType.tileSize() + " H:" + buildingType.tileHeight() + " W:" + buildingType.tileWidth());
    						
    					    for (int th=0; th<buildingType.tileHeight();th++) {
        					    for (int tw=0; tw<buildingType.tileWidth();tw++) {    	
        					    	TilePosition occupied = new TilePosition(i+tw, j+th);
        					    	if (!plannedPositions.contains(occupied)) plannedPositions.add(occupied);
        					    }
    					    }
    						
    						return ret;
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