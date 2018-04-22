package scai.elte.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import bwapi.Color;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Pair;
import bwapi.Player;
import bwapi.Position;
import bwapi.TechType;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.UpgradeType;
import bwapi.WalkPosition;
import bwta.BWTA;
import bwta.BaseLocation;
import bwta.Chokepoint;
import bwta.Region;
import scai.elte.command.BuildingManager;
import scai.elte.command.BunkerManager;
import scai.elte.command.Command;
import scai.elte.command.CommandType;
import scai.elte.command.ComsatManager;
import scai.elte.command.MarineManager;
import scai.elte.command.Request;
import scai.elte.command.Request.RequestType;
import scai.elte.command.RequestStatus;
import scai.elte.command.UnitManager;
import scai.elte.command.WorkerManager;
import scai.elte.command.WorkerManager.WorkerRole;
import scai.elte.main.ScoutInfo.TileType;
import scai.elte.strategy.BasePlanItem;
import scai.elte.strategy.BuildOrder;
import scai.elte.strategy.BuildOrderItem;
import scai.elte.strategy.BuildOrderItemStatus;
import scai.elte.strategy.TechItem;
import scai.elte.strategy.UpgradeItem;
import scai.elte.strategy.plan.TwoRaxFE;

public class Main extends DefaultBWListener {
	
    private Mirror mirror = new Mirror();

    public static Game game;

    public static Player self;

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
    public int reservedGasInQueue;
    
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
	//public int maximumWorkers = 45;
	public static int frameCount = 0;
    /**
     * Keeps track of how many units of each unit type the player has.
     */
    public HashMap<UnitType, Integer> unitCounts = new HashMap<UnitType, Integer>();
    /**
     * Keeps track of how many units are in production
     */
    public HashMap<UnitType, Integer> unitsInProduction = new HashMap<UnitType, Integer>();
    /**
     * Build order, buildings, addons, tech, upgrade
     */
    public static BuildOrder buildOrder = new BuildOrder();
    public static HashMap<Integer, UnitManager> unitManagers = new HashMap<Integer, UnitManager>();
    public static HashMap<Position, Integer> scannerPositions = new HashMap<Position, Integer>();
    public static int supplyUsedActual;
    public static Integer availableMinerals;
    public static Integer availableGas;
    
    Set<TilePosition> plannedPositions = new HashSet<TilePosition>();
    public static ConcurrentHashMap<String, Request> requests = new ConcurrentHashMap<String, Request>();
    
    public static ArrayList<Unit> enemyUnits = new ArrayList<Unit>();
    public static Set<BaseLocation> baseLocations = new HashSet<BaseLocation>();
    public static Set<Region> baseRegions = new HashSet<Region>();
    
    public HashMap<UnitType, ArrayList<Integer>> unitManagerIDs = new HashMap<UnitType, ArrayList<Integer>>(); //IDs by unit type, for quick access
    public HashMap<UnitType, Integer> targetUnitNumbers = new HashMap<UnitType, Integer>(); //Unit goals 
    Random rand;
    BaseLocation home;
    public static BaseLocation naturalExpansion;
    ArrayList<Chokepoint> chokes; 
    MapUtil mapUtil;
    
    
    @Override
    public void onUnitCreate(Unit unit) {
    	//System.out.println("Created:" + unit.getType());
    	if (unit.getPlayer() == self) {
        if (unit.getType() != UnitType.Resource_Mineral_Field && unit.getType() != UnitType.Resource_Vespene_Geyser && unit.getType() != UnitType.Unknown) {
	        assignUnitManager(unit);
        }
        //Reserve space for addons
        if (unit.getType() == UnitType.Terran_Command_Center) {
        	Position addonCorner=new Position (unit.getPosition().getX()+64, unit.getPosition().getY());
        	plannedPositions.addAll(getTilesForBuilding(addonCorner, UnitType.Terran_Comsat_Station));
        }
        	
        }
        countAllUnits();
        for (BuildOrderItem boi : buildOrder.buildOrderList) {
        	if (boi.status == BuildOrderItemStatus.BUILD_PROCESS_STARTED) {
        		if (unit.getType() == boi.getUnitType()) {
        			boi.status = BuildOrderItemStatus.UNDER_CONSTRUCTION;
        			reservedMinerals = reservedMinerals - boi.getUnitType().mineralPrice();
        			reservedGas = reservedGas - boi.getUnitType().gasPrice();
                	break;
        		}
        	}

        }
    }
    //Starting from the given position, returns which building tiles will the building occupy.
    public HashSet<TilePosition> getTilesForBuilding(Position pos, UnitType type) {
    	int x = (pos.getX() / 32)*32 ;
    	int y = (pos.getY() / 32)*32;
    	int h = type.tileHeight();
    	int w = type.tileWidth();
    	HashSet<TilePosition> buildingTiles = new HashSet<TilePosition>();
    	
    	for (int i=0; i<h;i++) {
    		for (int j=0;j<w;j++) {
    			TilePosition tp = new TilePosition((x+i*32)/32, (y+j*32)/32);
    			buildingTiles.add(tp);
    		}
    	}
    	return buildingTiles;
    }
    
    
    public void assignUnitManager(Unit unit) {
    	if (unit.getType() == UnitType.Terran_Bunker) {
    		unitManagers.putIfAbsent(unit.getID(), new BunkerManager(unit));
    	} else if (unit.getType() == UnitType.Terran_Marine) {
    		unitManagers.putIfAbsent(unit.getID(), new MarineManager(unit));
    	} else if (unit.getType() == UnitType.Terran_Comsat_Station) {
    		unitManagers.putIfAbsent(unit.getID(), new ComsatManager(unit));
    	} else if (unit.getType().isBuilding()) {
    		unitManagers.putIfAbsent(unit.getID(), new BuildingManager(unit));
    	} else if (unit.getType() == UnitType.Terran_SCV) {
    		unitManagers.putIfAbsent(unit.getID(), new WorkerManager(unit, WorkerRole.MINERAL));
    	}  else {
    		unitManagers.putIfAbsent(unit.getID(), new UnitManager(unit));
    	}
    	unitManagerIDs.putIfAbsent(unit.getType(), new ArrayList<Integer>());
    	unitManagerIDs.get(unit.getType()).add(unit.getID());
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
    
    @Override
    public void onUnitMorph(Unit unit) {
    	super.onUnitMorph(unit);
        for (BuildOrderItem boi : buildOrder.buildOrderList) {
        	if (boi.status == BuildOrderItemStatus.BUILD_PROCESS_STARTED) {
        		if (unit.getType() == boi.getUnitType()) {
        			boi.status = BuildOrderItemStatus.UNDER_CONSTRUCTION;
        			reservedGasInQueue = reservedGasInQueue - boi.getUnitType().gasPrice();
        			reservedGas = reservedGas - - boi.getUnitType().gasPrice();	
        			reservedMineralsInQueue = reservedMineralsInQueue - boi.getUnitType().mineralPrice();
        			reservedMinerals = reservedMinerals - boi.getUnitType().mineralPrice();
        		}
        	}
        }
    	
    }
    

    

    public static TreeSet<ScoutInfo> scoutHeatMap;

    @Override
    public void onStart() {
    	try {
    	rand = new Random();
        game = mirror.getGame();
        self = game.self();
     	game.setLocalSpeed(5);
     	game.enableFlag(1); //single cheats seldom work
     	mapUtil = new MapUtil();   	
     	countAllUnits();
     	
     
     	//TilePosition bt;
     	//game.isVisible(position)
     	
     	
        //Use BWTA to analyze map
        //This may take a few minutes if the map is processed first time!
        System.out.println("Analyzing map...");
        BWTA.analyze();
        
        chokes = (ArrayList<Chokepoint>) BWTA.getChokepoints();
        for(BaseLocation baseLocation : BWTA.getBaseLocations()){
        	baseLocations.add(baseLocation);
        	
        	
        	baseRegions.add(baseLocation.getRegion());
        	if (baseLocation.getTilePosition().equals(self.getStartLocation())) {
        		home = baseLocation;
        	}
        }

        double minDist = Double.MAX_VALUE;
        for (BaseLocation bl : baseLocations) {
        	if (!bl.equals(home) && bl.getGroundDistance(home) < minDist) {
        		minDist = home.getGroundDistance(bl);
        		naturalExpansion = bl;
        		
        	//	bl.getRegion().
        		
        	}
        }  
        

     	scoutHeatMap = new  TreeSet<ScoutInfo>(new ScoutInfoComparator());
     	//Build heatmap for exploration
     	for (int i=0; i<game.mapWidth(); i++) {
     		for (int j=0; j<game.mapHeight();j++) {

     			TilePosition tp = new TilePosition(i,j);
     			Region r = MapUtil.getRegionOfTile(tp);
     			ScoutInfo si;
     			boolean start = false;
     			if (baseRegions.contains(r)) {
     				for (BaseLocation bl : r.getBaseLocations()) {
     					if (bl.isStartLocation()) {
     						start = true;
     						break;
     					}
     				}
     				if (start) {
     					si = new ScoutInfo(tp, TileType.START_LOC, 3, game.isWalkable(tp.toWalkPosition()));
     				} else {
     					si = new ScoutInfo(tp, TileType.BASE_LOC, 2, game.isWalkable(tp.toWalkPosition()));
     				}
     			} else {
     				si = new ScoutInfo(tp, TileType.NORMAL, 1, game.isWalkable(tp.toWalkPosition()));
     				
     			}
     			scoutHeatMap.add(si);
     		}
     	}
     	

        
        
        
     	for (Unit unit : self.getUnits()) {
     		assignUnitManager(unit);
     		if (unit.getType() == UnitType.Terran_SCV) {
     			assignWorkerRole(unit, WorkerRole.MINERAL);
     		}
     	}
        buildOrder = new TwoRaxFE(naturalExpansion.getTilePosition());
        //Could be part of the build order?
        targetUnitNumbers.putIfAbsent(UnitType.Terran_Marine, 30);
        targetUnitNumbers.putIfAbsent(UnitType.Terran_SCV, 12);
    	}
    	catch (Exception ex) {
    		ex.printStackTrace();
    	}
    }

	@Override
	public void onUnitDestroy(Unit unit) {
		if (unit.getPlayer() == self && !unit.getType().isNeutral()) {
		unitManagerIDs.get(unit.getType()).remove(Integer.valueOf(unit.getID()));
		unitManagers.remove(unit.getID());
		}
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
    
    
    public void assignWorkerRole(Unit worker, WorkerRole role) {
    	((WorkerManager)unitManagers.get(worker.getID())).setPrevRole(((WorkerManager)unitManagers.get(worker.getID())).getRole());
    	((WorkerManager)unitManagers.get(worker.getID())).setRole(role);
    }
    
    //Convenience method 
    public void assignWorkerRoles(Collection<Unit> workers, WorkerRole role) {
    	for (Unit worker : workers) {
    		assignWorkerRole(worker, role);
    	}
    }
    
    public Unit getWorkerFromRole(WorkerRole prevRole, WorkerRole role) {
    	//assign random
    	ArrayList<Integer> randomIds = unitManagerIDs.get(UnitType.Terran_SCV);
    	while (randomIds.size() > 0) {
    	Integer d = rand.nextInt(randomIds.size());
    	Integer wmid = randomIds.get(d);
    	randomIds.remove(wmid);
   		WorkerManager wm = (WorkerManager)unitManagers.get(wmid);
   		if (wm.getUnit().isCompleted() && wm.getRole() == prevRole) {
   			wm.setPrevRole(prevRole);
   			wm.setRole(role);
   			return wm.getUnit();
   		}
    	}
    	return null;
    }
   
    @Override
    public void onUnitShow(Unit unit) {
    	if (unit.getPlayer() != self && !unit.getType().isNeutral()) {
    		//System.out.println("OMG IT'S A ENEMY"+ unit.getType()  + " IT HAZ A ID:" + unit.getID());
    		//game.setLocalSpeed(40);
    		enemyUnits.add(unit);
    	}
    	
    }
    
    @Override
    public void onUnitHide(Unit unit) {
    	if (unit.getPlayer() != self && !unit.getType().isNeutral()) {
        	enemyUnits.remove(unit);
    	}
    }
        
    @Override
    public void onUnitDiscover(Unit unit) {
    	if (unit.getType() == UnitType.Spell_Scanner_Sweep && unit.getPlayer() == self) {
    		scannerPositions.put(unit.getPosition(), 262);
    	}
    }
    
    boolean scout = false;
    @Override
    public void onFrame() {

    	try {
    	//Accounting
    	frameCount++;
    	countAllUnits();

        //Debug / Info
        StringBuilder statusMessages = new StringBuilder();
        availableMinerals = self.minerals() - reservedMinerals;
        availableGas = self.gas() - reservedGas;
        statusMessages.append("Available minerals:" + availableMinerals.toString() + "\n");
        statusMessages.append("Frame count:" + frameCount+ "\n");
        statusMessages.append("Units:" + self.getUnits().size() + " Managers:" + unitManagers.size() + "\n");
        statusMessages.append("Requests:" + requests.size()+ "\n");
        statusMessages.append("APM:" + game.getAPM());
        
        updateScannedPositions();
        
        ageHeatMap();
        
        if (!scout && unitCounts.get(UnitType.Terran_SCV) >= 10) {
        	Integer rid = unitManagerIDs.get(UnitType.Terran_SCV).get(rand.nextInt(unitManagerIDs.get(UnitType.Terran_SCV).size())); //TODO "get random worker logic rework"
        	Unit worker = unitManagers.get(rid).getUnit();
        	assignWorkerRole(worker, WorkerRole.SCOUT);
        	scout = true;
        }
        
        for (BasePlanItem bpi : buildOrder.getImproveOrder()) {
        	if (bpi.getExecutorId() == null || !unitManagers.containsKey(bpi.getExecutorId())) {
        	if (bpi instanceof TechItem) {
        		TechType tech = ((TechItem) bpi).getTechType();
        		
            	if (game.canResearch(tech) && tech.gasPrice() < availableGas && tech.mineralPrice() < availableMinerals) {
            		if (unitManagerIDs.get(tech.whatResearches()) != null) {
            			BuildingManager bm = (BuildingManager) getResearcher(tech);
            			if (bm!= null) {
            				bm.getImproveList().add(bpi);
            				bpi.setExecutorId(bm.getUnit().getID());
            				break;
            			};
            		}
            	} else if (self.hasResearched(tech)) {
            		buildOrder.getImproveOrder().remove(bpi);   
            		}
        		
        	} else if (bpi instanceof UpgradeItem) {
        		UpgradeType upg = ((UpgradeItem) bpi).getUpgradeType();
        		if (game.canUpgrade(upg) && upg.gasPrice() < availableGas && upg.mineralPrice() < availableMinerals) {
            		if (unitManagerIDs.get(upg.whatUpgrades()) != null) {
            			BuildingManager bm = (BuildingManager) getUpgrader(upg);
            		if (bm != null) {
            			bm.getImproveList().add(bpi);
            			bpi.setExecutorId(bm.getUnit().getID());
            			break;
            		}
            		}
            	} else if (self.getUpgradeLevel(upg) >= ((UpgradeItem)bpi).getLevel()) { //already upgraded
            		buildOrder.getImproveOrder().remove(bpi);   
            	}	
        	}
        }
        }
        //Verify/debug
        /*
        for (ScoutInfo sc : scoutHeatMap) {
        	if (sc.isWalkable())   {  		
        		game.drawBoxMap(sc.getTile().toPosition().getX(), sc.getTile().toPosition().getY(), 
        			sc.getTile().toPosition().getX()+10, 
        			sc.getTile().toPosition().getY()+10, Color.Red, true);
        	} else {
        		game.drawBoxMap(sc.getTile().toPosition().getX(), sc.getTile().toPosition().getY(), 
            			sc.getTile().toPosition().getX()+10, 
            			sc.getTile().toPosition().getY()+10, Color.Green, true);
        	}
        }
        */
        
        /*
        
        for (TilePosition c : plannedPositions) {
        	game.drawBox(Enum.Map, c.toPosition().getX(), c.toPosition().getY(), c.toPosition().getX()+16, c.toPosition().getY()+16, Color.Yellow, true);
        }
        
        
        for (BaseLocation bl : baseLocations) {
    		
    		game.drawBox(Enum.Map, bl.getRegion().getCenter().getX(), bl.getRegion().getCenter().getY(), bl.getRegion().getCenter().getX()+10, 
    				bl.getRegion().getCenter().getY()+10, Color.Red, true);
    		for (Position pos : bl.getRegion().getPolygon().getPoints()) {
    			game.drawDot(Enum.Map, pos.getX(), pos.getY(), Color.White);
    			
    		}
    		
    	}
        
        for (Chokepoint c : chokes) {
        	
        	game.drawBox(Enum.Map, c.getX(), c.getY(), c.getX()+10, c.getY()+10, Color.Yellow, true);
        	game.drawBox(Enum.Map, c.getSides().first.getX(), c.getSides().first.getY(), c.getSides().first.getX()+10, c.getSides().first.getY()+10, Color.Cyan, true);
        	game.drawBox(Enum.Map, c.getSides().second.getX(), c.getSides().second.getY(), c.getSides().second.getX()+10, c.getSides().second.getY()+10, Color.Cyan, true);
        }
        
        */

    	
    	

    	if (buildOrder.getSupplyExecuted() <= supplyUsedActual && supplyUsedActual >= self.supplyTotal()-2) {
    		Command bc = new Command(CommandType.BUILD, null, UnitType.Terran_Supply_Depot);
    		requests.putIfAbsent("supplyExtend", new Request(null, bc));
    	} else {
    		requests.remove("supplyExtend");
    	}
    	
    	//Check if we need another supply depot, after the initial build order is fulfilled
    	if (requests.get("supplyExtend") != null && requests.get("supplyExtend").getRequestStatus() == RequestStatus.NEW) {
    		buildOrder.addItem(UnitType.Terran_Supply_Depot, 0, 1);
    		requests.get("supplyExtend").setRequestStatus(RequestStatus.BEING_ANSWERED);
    	}
    	
    	//Main request loop
			for (String rk : requests.keySet()) {
				Request r = requests.get(rk);
				if (r.getRequestStatus() == RequestStatus.FULFILLED) {
					requests.remove(rk);
				}

				if (r.getType() == RequestType.COMMAND) {
					if (r.getRequestedCommand().getType() == CommandType.GAS_WORKER
							&& r.getRequestStatus() == RequestStatus.NEW) {
						// Random gatherer, closest one would be best //TODO closest unit of type
						Unit gasWorker = getWorkerFromRole(WorkerRole.MINERAL, WorkerRole.GAS);
						System.out.println("ROLEGAS:" + ((WorkerManager)unitManagers.get(gasWorker.getID())).getRole());
						//assignWorker(gasWorker, gasWorkers);
						r.setAnsweringUnit(gasWorker);
						r.setRequestStatus(RequestStatus.BEING_ANSWERED);
					}
				} else if (r.getType() == RequestType.DEFEND) {
					// Check if there is an army present in the region
					boolean armyAvailable = false;
					ArrayList<Unit> workersInRegion = new ArrayList<Unit>();
					Region def = MapUtil.getRegionOfUnit(r.getRequestingUnit());
					Set<Unit> unitsInRegion = MapUtil.getUnitsInRegion(def);
					for (Unit u : unitsInRegion) {

						if (!u.getType().isBuilding()) {
							// TODO maybe check closest regions / standing army?
							if (u.getPlayer() == self) {
								if (!u.getType().isWorker()) {

									armyAvailable = true;
								} else {
									workersInRegion.add(u);
								}

							}
						}

						if (armyAvailable) {
							// TODO
						} else {
							assignWorkerRoles(workersInRegion, WorkerRole.MILITIA);
						}
					}
				}
			}
			if (workerCount > 1) {
				for (BuildOrderItem boi : buildOrder.buildOrderList) {
					UnitType buildingType = boi.getUnitType();
					if (boi.getSupplyThreshold() <= supplyUsedActual && boi.status == BuildOrderItemStatus.PLANNED) {
						// Reserve minerals
						reservedMinerals = reservedMinerals + buildingType.mineralPrice();
						reservedGas = reservedGas + buildingType.gasPrice();
						System.out.println("supply:" + supplyUsedActual + " , type:" + boi.getUnitType() + " to queue");
						boi.status = BuildOrderItemStatus.IN_QUEUE;
					}
				if (boi.status == BuildOrderItemStatus.IN_QUEUE
							&& (self.minerals() - reservedMineralsInQueue) >= buildingType.mineralPrice()
							&& self.gas() - reservedGasInQueue >= buildingType.gasPrice()
							&& game.canMake(buildingType)) {
						if (buildingType.isAddon()) {
							//System.out.println("Addon...");
							if (unitManagerIDs.get(buildingType.whatBuilds().first) != null) {
								BuildingManager addonBuilder = ((BuildingManager) getAddonBuilder(buildingType));
								if (addonBuilder != null) {
									addonBuilder.setAddon(new Pair<>(buildingType, false)); 
									boi.status = BuildOrderItemStatus.UNDER_CONSTRUCTION;
								}
							}
						} else {
							
							Unit mb = getWorkerFromRole(WorkerRole.MINERAL, WorkerRole.BUILD); 
							if (boi.getTilePosition() == null) {
								TilePosition buildTile = null;
									// get a nice place to build
									TilePosition aroundTile;
									if (boi.getTilePosition() == null) {
										aroundTile = self.getStartLocation();
										buildTile = getBuildTile(mb, buildingType, aroundTile);
									} else {
										buildTile = boi.getTilePosition();
									}								
								boi.setTilePosition(buildTile);
							} 
							
							mb.build(boi.getUnitType(), boi.getTilePosition()); 							
							((WorkerManager)unitManagers.get(mb.getID())).setTargetTile(boi.getTilePosition());

							
							System.out.println("--------------------------------------------Builder id:"
									+ mb.getID() + " command is to build " + boi.getUnitType() + "in "
									+ boi.getTilePosition().getX() + " " + boi.getTilePosition().getY());
									
							reservedMineralsInQueue += boi.getUnitType().mineralPrice();
							reservedGasInQueue += boi.getUnitType().gasPrice();
							boi.status = BuildOrderItemStatus.BUILD_PROCESS_STARTED;
							break;
						}

					}
				}
			}

    	for (Integer i : unitManagers.keySet()) {
    		unitManagers.get(i).operate();
    	}

    	
    	//For every unit target, get a producer building, and train unit TODO maybe importance of different units?
    	trainRequiredUnits();
    	requestScanIfNeeded();
  /*      
    	for (Unit myUnit : self.getUnits()) {
        	game.drawTextMap(myUnit.getPosition().getX(), myUnit.getPosition().getY(), "ID:" + myUnit.getID());
        }
*/

        game.drawTextScreen(10, 25, statusMessages.toString());        
        

    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }

    public static void main(String[] args) {
        new Main().run();
    }
    
    public void ageHeatMap() {
    	if (frameCount % 20 == 0) { //Speed up, for debug purposes
    	for (ScoutInfo sc : scoutHeatMap) {
    		//scoutHeatMap.remove(sc);
			int weight = 0;
			if (game.isVisible(sc.getTile())) {
		
				sc.setImportance(0);
			} else {

				if (sc.getType() == TileType.BASE_LOC) {
					weight = 2;
				} else if (sc.getType() == TileType.START_LOC) {
					weight = 3;
				} else if (sc.getType() == TileType.NORMAL) {
					weight = 1;
				}
				if (!game.isExplored(sc.getTile())) {
					weight = weight * 2;
				}
				sc.setImportance(sc.getImportance() + weight);
			}	
    	}
    	}
    }
    
    public void updateScannedPositions () { 
    	for (Position p : scannerPositions.keySet()) {
    		int age = scannerPositions.get(p);
    		if (age>0) {
    			age--;
    			scannerPositions.put(p, age);
    		} else {
    			scannerPositions.remove(p);
    		}
    	}
    }
    
    public void trainRequiredUnits() {
    	for (UnitType ut : targetUnitNumbers.keySet()) {
    		UnitType producer = ut.whatBuilds().first;
    		int numProducers = unitCounts.getOrDefault(producer,0);
    		int targetNumber = targetUnitNumbers.get(ut);
    		int inProduction = unitsInProduction.getOrDefault(ut, 0);
    		if (numProducers != 0 && availableMinerals >= ut.mineralPrice() && availableGas >= ut.gasPrice() && unitCounts.getOrDefault(ut,0) < targetNumber + inProduction) {
    			for (Integer i = inProduction; i < targetNumber + inProduction ; i++) {
    				for (Integer pId : unitManagerIDs.get(producer)) {
        				UnitManager prodManager = unitManagers.get(pId);
        				if ((unitManagers.get(pId).getUnit().getTrainingQueue().size() < 1)) {
        					prodManager.trainUnit(prodManager.getUnit(), ut);
        					break;
        				}
        			}
    				
    			}
    		}
    	}
    }
    
    public void requestScanIfNeeded() {
    	
    	for (Unit enemy : enemyUnits) {
    		if (enemy.getType() == UnitType.Zerg_Lurker) {
    			if (enemy.isAttacking() && enemy.isBurrowed()) {
    				TilePosition tp = enemy.getTilePosition();
    				String key = tp.getX() + "sc" + tp.getY();
    				if (!requests.containsKey(key)) { 
    				Command scan = new Command(CommandType.SCAN);
        			
        			scan.setTargettilePosition(tp);
        			Request r = new Request(null, scan);
        			requests.putIfAbsent(tp.getX() + "sc" + tp.getY(), r); //TODO better id 
//        			System.out.println("SCAN REQUESTED ON" + tp.getX() + tp.getY() + " Frame: "+ frameCount);
    				}
    			}
	
    		}
    		if (enemy.isAttacking() && !enemy.isTargetable()) {
    			Command scan = new Command(CommandType.SCAN);
    			TilePosition tp = enemy.getTilePosition();
    			scan.setTargettilePosition(tp);
    			Request r = new Request(null, scan);
    			requests.put(tp.getX() + "scan" + tp.getY(), r);
    			System.out.println("SCAN REQUESTED ON (invisible)" + tp.getX() + tp.getY());
    			
    		}
    		
    	}
    }
    
    //Convenience method for getting a "random" available upgrader
    public UnitManager getUpgrader(UpgradeType upg) {
    	if (unitManagerIDs.get(upg.whatUpgrades()) != null) {
    		for (Integer buildingID : unitManagerIDs.get(upg.whatUpgrades())) {
    			if (unitManagers.get(buildingID).getUnit().canUpgrade(upg)) {
					return unitManagers.get(buildingID);
				}    			
    		}

		}
		return null;
    }
    
    //Convenience method for getting a "random" available researcher
    public UnitManager getResearcher(TechType tech) {
    	if (unitManagerIDs.get(tech.whatResearches()) != null) {
    		for (Integer buildingID : unitManagerIDs.get(tech.whatResearches())) {
    			if (unitManagers.get(buildingID).getUnit().canResearch(tech)) {
					return unitManagers.get(buildingID);
				}
    			
    		}
		}
		return null;
    }
    
  //Convenience method for getting a "random" available addon builder
	public UnitManager getAddonBuilder(UnitType addonType) {
		//System.out.println("ADDON");
		if (unitManagerIDs.get(addonType.whatBuilds().first) != null) {
			for (Integer buildingID : unitManagerIDs.get(addonType.whatBuilds().first)) {
				if (unitManagers.get(buildingID).getUnit().canBuild(addonType)) {
					return unitManagers.get(buildingID);
				}
			}
		}
		return null;
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
    				boolean canBuild = true;
    				for (TilePosition tp : getTilesForBuilding(targetTile.toPosition(), buildingType)){
    					if (plannedPositions.contains(tp)) {
    						canBuild = false;
    					}
    					
    				}
    				
    				if (game.canBuildHere(new TilePosition(i,j), buildingType, builder, false) && canBuild) {
        		//		System.out.println("DEBUG containstile_inner: +" + plannedPositions.contains(targetTile));
    					// units that are blocking the tile
    					boolean unitsInWay = false;
    					for (Unit u : game.getAllUnits()) {
    						if (u.getID() == builder.getID()) continue;
    						if ((Math.abs(u.getTilePosition().getX()-i) < 4) && (Math.abs(u.getTilePosition().getY()-j) < 4)) unitsInWay = true;
    					}
    					if (!unitsInWay) {
    						ret = new TilePosition(i, j);
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
    		maxDist += 1;
    	}
    	if (ret == null) game.printf("Unable to find suitable build position for "+buildingType.toString());
    	return ret;
    }
}	