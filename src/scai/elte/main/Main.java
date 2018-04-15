package scai.elte.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.org.apache.xerces.internal.impl.dtd.models.CMStateSet;

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
import bwapi.UnitCommandType;
import bwapi.UnitType;
import bwapi.UpgradeType;
import bwapi.CoordinateType.Enum;
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
import scai.elte.strategy.BasePlanItem;
import scai.elte.strategy.BuildOrder;
import scai.elte.strategy.BuildOrderItem;
import scai.elte.strategy.BuildOrderItemStatus;
import scai.elte.strategy.TechItem;
import scai.elte.strategy.UpgradeItem;
import scai.elte.strategy.plan.TwoRaxFE;

public class Main extends DefaultBWListener {
	
	public boolean DEBUGMODE = true;

    private Mirror mirror = new Mirror();

    private Game game;

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
    public static BuildOrder buildOrder = new BuildOrder();
    
    public static HashMap<Integer, UnitManager> unitManagers = new HashMap<Integer, UnitManager>();
    
    public HashMap<UnitType, HashSet<Integer>> unitIDs = new HashMap<UnitType, HashSet<Integer>>(); //IDs by unit type, for quick access
    Random rand;
    
    //Worker groups
    public ArrayList<Unit> mineralWorkers = new ArrayList<Unit>(); //Default activity
    public ArrayList<Unit> gasWorkers = new ArrayList<Unit>();
    public ArrayList<Unit> builders  = new ArrayList<Unit>();
    public ArrayList<Unit> militia = new ArrayList<Unit>(); //SCVs fighting
    public ArrayList<Unit> scouts = new ArrayList<Unit>();	
    public ArrayList<ArrayList<Unit>> workerGroups = new ArrayList<ArrayList<Unit>>(); 
    
    
    
    public static int supplyUsedActual;
    public static Integer availableMinerals;
    public static Integer availableGas;
    //public int supplyTotal - do i need this?
    
    Set<TilePosition> plannedPositions = new HashSet<TilePosition>();
    public static ConcurrentHashMap<String, Request> requests = new ConcurrentHashMap<String, Request>();
    
    public static ArrayList<Unit> enemyUnits = new ArrayList<Unit>();
    public static Set<BaseLocation> baseLocations = new HashSet<BaseLocation>();
    public static Set<Region> baseRegions = new HashSet<Region>();
    
    BaseLocation home;
    BaseLocation naturalExpansion;
    ArrayList<Chokepoint> chokes; 
    MapUtil mapUtil;
    
    
    @Override
    public void onUnitCreate(Unit unit) {
      //  System.out.println("New unit discovered " + unit.getType());
        if (unit.getType() != UnitType.Resource_Mineral_Field && unit.getType() != UnitType.Resource_Vespene_Geyser && unit.getType() != UnitType.Unknown) {
	        assignUnitManager(unit);
        }
        

        countAllUnits();
        for (BuildOrderItem boi : buildOrder.buildOrderList) {
        	if (boi.status == BuildOrderItemStatus.BUILD_PROCESS_STARTED) {
        		if (unit.getType() == boi.getUnitType()) {
        			boi.status = BuildOrderItemStatus.UNDER_CONSTRUCTION;
//        			if (DEBUGMODE)	System.out.println("Changing "+ boi.getUnitType() + " to UNDER_CONSTRUCTION" );
        			reservedMinerals = reservedMinerals - boi.getUnitType().mineralPrice();
        			reservedGas = reservedGas - boi.getUnitType().gasPrice();
                	break;
        		}
        	}

        }
    }
    
    public void assignUnitManager(Unit unit) {
    	if (unit.getType() == UnitType.Terran_Bunker) {
    		unitManagers.put(unit.getID(), new BunkerManager(unit));
    	} else if (unit.getType() == UnitType.Terran_Marine) {
    		unitManagers.put(unit.getID(), new MarineManager(unit));
    	} else if (unit.getType() == UnitType.Terran_Comsat_Station) {
    		unitManagers.put(unit.getID(), new ComsatManager(unit));
    	} else if (unit.getType().isBuilding()) {
    		unitManagers.put(unit.getID(), new BuildingManager(unit));
    	} else {
    		unitManagers.put(unit.getID(), new UnitManager(unit));
    	}
    	
    	unitIDs.putIfAbsent(unit.getType(), new HashSet<Integer>());
    	unitIDs.get(unit.getType()).add(unit.getID());
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
        			reservedGasInQueue = reservedGasInQueue - boi.getUnitType().gasPrice();
        			reservedGas = reservedGas - - boi.getUnitType().gasPrice();	
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
        game = mirror.getGame();
        self = game.self();
     	game.setLocalSpeed(5);
     	game.enableFlag(1); //single cheats seldom work
     	mapUtil = new MapUtil();   	
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
        chokes = (ArrayList<Chokepoint>) BWTA.getChokepoints();
        workerGroups.add(mineralWorkers);
        workerGroups.add(gasWorkers);
        workerGroups.add(builders);
        workerGroups.add(militia);
        workerGroups.add(scouts);
        for(BaseLocation baseLocation : BWTA.getBaseLocations()){
        	baseLocations.add(baseLocation);
        	baseRegions.add(baseLocation.getRegion());
        
        	System.out.println(baseLocation.getTilePosition());
        	if (baseLocation.getTilePosition().equals(self.getStartLocation())) {
        		home = baseLocation;

        	}
        }
        

        double minDist = Double.MAX_VALUE;
        for (BaseLocation bl : baseLocations) {
        	if (!bl.equals(home) && bl.getGroundDistance(home) < minDist) {
        		minDist = home.getGroundDistance(bl);
        		naturalExpansion = bl;
        	}
        }  
        buildOrder = new TwoRaxFE(naturalExpansion.getTilePosition());
    	}
    	catch (Exception ex) {
    		ex.printStackTrace();
    	}
    }

	@Override
	public void onUnitDestroy(Unit unit) {
		unitManagers.remove(unit.getID());
    	//unitIDs.get(unit.getType()).remove(unit.getID());
		//if unit.getPlayer() == self TODO
		if (unit.getType().isWorker()) {
			for (List<Unit> gr : workerGroups) {
				if (gr.contains(unit)) {
					gr.remove(unit);
				}
			}
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
        			System.out.println("Setting " + boi.getUnitType() + " to DONE");
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
    
    
    public void assignWorker(Unit unit, ArrayList<Unit> group) {
    	for (List<Unit> gr : workerGroups) {
    		if (gr == group) {
    			if (!gr.contains(unit)) {
    				gr.add(unit);
    			}
    		} else {
    			if (gr.contains(unit)) {
    				gr.remove(unit);
    			}
    		}
    		
    	}
    }
    
    public void assignWorker(Collection<Unit> units, ArrayList<Unit> group) {
    	for (Unit unit : units) {
    	
    	for (List<Unit> gr : workerGroups) {
    		if (gr == group) {
    			if (!gr.contains(unit)) {
    				gr.add(unit);
    			}
    		} else {
    			if (gr.contains(unit)) {
    				gr.remove(unit);
    			}
    		}
    		
    	}
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
    		game.setLocalSpeed(40);
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
    	
    	//if unit.getPlayer() //ENemy unit management
    }
    
    
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
        statusMessages.append("Frame count:" + frameCount + "\n");
        statusMessages.append("Units:" + self.getUnits().size() + " Managers:" + unitManagers.size() + "\n");
        statusMessages.append("Requests:" + requests.size()+ "\n");
        statusMessages.append("APM:" + game.getAPM());
        
        
        for (BasePlanItem bpi : buildOrder.getImproveOrder()) {
        	if (bpi.getExecutorId() == null || !unitManagers.containsKey(bpi.getExecutorId())) {
        	//	System.out.println("Upgradeloop" + bpi.getExecutorId());
        	if (bpi instanceof TechItem) {
        		TechType tech = ((TechItem) bpi).getTechType();
        		
            	if (game.canResearch(tech) && tech.gasPrice() < availableGas && tech.mineralPrice() < availableMinerals) {
            		if (unitIDs.get(tech.whatResearches()) != null) {
            			BuildingManager bm = (BuildingManager) getResearcher(tech);
            			if (bm!= null) {
            				System.out.println("WUT");
            				bm.getImproveList().add(bpi);
            				bpi.setExecutorId(bm.getUnit().getID());
            				break;
            			};
            			/*
            		Integer buildingID = unitIDs.get(tech.whatResearches()).iterator().next();
            		if (unitManagers.get(buildingID).getUnit().canResearch(tech)) {
            			((BuildingManager)unitManagers
            			break;
            		}
            		*/
            		}
            	} else if (self.hasResearched(tech)) {
            		buildOrder.getImproveOrder().remove(bpi);   
            		}
        		
        	} else if (bpi instanceof UpgradeItem) {
        		UpgradeType upg = ((UpgradeItem) bpi).getUpgradeType();
        		if (game.canUpgrade(upg) && upg.gasPrice() < availableGas && upg.mineralPrice() < availableMinerals) {
            		if (unitIDs.get(upg.whatUpgrades()) != null) {
            		//Integer buildingID = unitIDs.get(upg.whatUpgrades()).iterator().next();
            			BuildingManager bm = (BuildingManager) getUpgrader(upg);
            		if (bm != null) {
            			bm.getImproveList().add(bpi);
            			bpi.setExecutorId(bm.getUnit().getID());
            			break;
            		}
            		/*
            		if (unitManagers.get(buildingID).getUnit().canUpgrade(upg)) {
            			((BuildingManager)unitManagers.get(buildingID)) .getImproveList().add(bpi);
            			break;
            		}
            		*/
            		}
            	} else if (self.getUpgradeLevel(upg) >= ((UpgradeItem)bpi).getLevel()) { //already upgraded
            		buildOrder.getImproveOrder().remove(bpi);   
            	}
        		
        	}
        	
        }
        }
    	
        //Verify/debug
        for (BaseLocation bl : baseLocations) {
    		
    		game.drawBox(Enum.Map, bl.getRegion().getCenter().getX(), bl.getRegion().getCenter().getY(), bl.getRegion().getCenter().getX()+10, 
    				bl.getRegion().getCenter().getY()+10, Color.Red, true);
    		for (Position pos : bl.getRegion().getPolygon().getPoints()) {
    			game.drawDot(Enum.Map, pos.getX(), pos.getY(), Color.White);
    			
    		}
    		
    	}
        for (Chokepoint c : chokes) {
        	//c.getRegions()
        	//c.getSides() left top right bottom
        	game.drawBox(Enum.Map, c.getX(), c.getY(), c.getX()+10, c.getY()+10, Color.Yellow, true);
        	game.drawBox(Enum.Map, c.getSides().first.getX(), c.getSides().first.getY(), c.getSides().first.getX()+10, c.getSides().first.getY()+10, Color.Cyan, true);
        	game.drawBox(Enum.Map, c.getSides().second.getX(), c.getSides().second.getY(), c.getSides().second.getX()+10, c.getSides().second.getY()+10, Color.Cyan, true);
            
        	//game.drawBox(Enum.Screen, c.getX(), c.getY(), c.getX()+100, c.getY()+100, Color.Green, true);
        	//game.drawBox(Enum.Mouse, c.getX(), c.getY(), c.getX()+100, c.getY()+100, Color.Cyan, true);
        	c.getCenter();
        	
        //	game.drawTextMap(c.getCenter().getX(), c.getCenter().getY(), "choke center:" + c.getCenter());
        }
        //end debug
        
	   	int workersInProduction = 0;
    	if (unitsInProduction.get(UnitType.Terran_SCV) != null) {
    		workersInProduction = unitsInProduction.get(UnitType.Terran_SCV);
    	}
    	
    	

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
    	/*
    	 *         for (Request r : requests.values()) {

        	if (r.getRequestedCommand().getType() == CommandType.GAS_WORKER && r.getRequestStatus() == RequestStatus.NEW) {
        		//Random gatherer, closest one would be best //TODO closest unit of type
        		Unit gasWorker = mineralWorkers.get(rand.nextInt(mineralWorkers.size()));
        		assignWorker(gasWorker, gasWorkers);
        		r.setAnsweringUnit(gasWorker);
        		r.setRequestStatus(RequestStatus.BEING_ANSWERED);
        	}
        }
        
    	 */
			for (String rk : requests.keySet()) {
				Request r = requests.get(rk);
				if (r.getRequestStatus() == RequestStatus.FULFILLED) {
					requests.remove(rk);
				}

				if (r.getType() == RequestType.COMMAND) {
					if (r.getRequestedCommand().getType() == CommandType.GAS_WORKER
							&& r.getRequestStatus() == RequestStatus.NEW) {
						// Random gatherer, closest one would be best //TODO closest unit of type
						Unit gasWorker = mineralWorkers.get(rand.nextInt(mineralWorkers.size()));
						assignWorker(gasWorker, gasWorkers);
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
							// System.out.println("No army available, assigning all workers in the region to
							// militia");
							assignWorker(workersInRegion, militia);

						}
					}
				}
			}
    	
    	
    	for (Unit mil : militia) {
    		Unit enemy = MapUtil.getNearestUnit(enemyUnits, mil); //TODO only in region
    		if (mil.getLastCommand().getUnitCommandType() != UnitCommandType.Attack_Move) {
    			mil.attack(enemy);
    		}
    	}
    	
    	
			if (workerCount > 1) {
				for (BuildOrderItem boi : buildOrder.buildOrderList) {
					UnitType buildingType = boi.getUnitType();
					if (boi.getSupplyThreshold() <= supplyUsedActual && boi.status == BuildOrderItemStatus.PLANNED) {
						// Reserve minerals
						reservedMinerals = reservedMinerals + buildingType.mineralPrice();
						reservedGas = reservedGas + buildingType.gasPrice();
						boi.status = BuildOrderItemStatus.IN_QUEUE;
					}
					// Periodically check for false positives - this can occur after failed builds
					// (occupied place, etc)

					if (frameCount % 250 == 0 && boi.status == BuildOrderItemStatus.IN_QUEUE && boi.gotBuilder) {
						boi.gotBuilder = false;
					}

					if (boi.status == BuildOrderItemStatus.IN_QUEUE
							&& (self.minerals() - reservedMineralsInQueue) >= buildingType.mineralPrice()
							&& self.gas() - reservedGasInQueue >= buildingType.gasPrice()
							&& game.canMake(buildingType)) {
						if (buildingType.isAddon()) {
							if (unitIDs.get(buildingType.whatBuilds().first) != null) {
								System.out.println("ADDON requested");
								((BuildingManager) getAddonBuilder(buildingType)).setAddon(new Pair<>(buildingType, false));
								boi.status = BuildOrderItemStatus.UNDER_CONSTRUCTION; //TODO unit manager to do this?
							}
						} else {

							if (!boi.gotBuilder) {
								Unit builder = null;
								for (Unit b : builders) {
									if (!b.isConstructing()
											&& !b.getLastCommand().getUnitCommandType().equals(UnitCommandType.Build)) {
										builder = b;
										boi.gotBuilder = true;
										break;
									}
								}

								if (!boi.gotBuilder) {
									Unit mb = mineralWorkers.get(rand.nextInt(mineralWorkers.size()));
									assignWorker(mb, builders);
								}

								if (boi.getTilePosition() == null) {
									TilePosition buildTile = null;
									if (builder != null) {
										// get a nice place to build
										TilePosition aroundTile;
										if (boi.getTilePosition() == null) {
											aroundTile = self.getStartLocation();
											buildTile = getBuildTile(builder, buildingType, aroundTile);
										} else {
											buildTile = boi.getTilePosition();
										}
									}
									boi.setTilePosition(buildTile);
								}

								if (boi.getTilePosition() != null && boi.gotBuilder) {
									if (builder.canBuild(buildingType, boi.getTilePosition())) {
										builder.build(boi.getUnitType(), boi.getTilePosition());

										System.out.println("--------------------------------------------Builder id:"
												+ builder.getID() + " command is to build " + boi.getUnitType() + "in "
												+ boi.getTilePosition().getX() + " " + boi.getTilePosition().getY());

										boi.gotBuilder = true;
										reservedMineralsInQueue += boi.getUnitType().mineralPrice();
										reservedGasInQueue += boi.getUnitType().gasPrice();
										boi.status = BuildOrderItemStatus.BUILD_PROCESS_STARTED;
										break;
									}
								}

							}
						}

					}
				}
			}

    	//The UnitManager loop will eventually replace the myUnit loop
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
					if (r.getType() == RequestType.COMMAND) {
						if (r.getRequestedCommand().getType() == CommandType.GAS_WORKER
								&& r.getRequestStatus() == RequestStatus.BEING_ANSWERED
								&& r.getAnsweringUnit().getID() == worker.getID()) {
							if (worker.isGatheringGas()) {
								r.setRequestStatus(RequestStatus.FULFILLED);
							} else {
								if (!worker.isGatheringGas()) {
									worker.gather(r.getRequestingUnit());
								}

							}
						}
					}
				}
			}
			
			
			
		       if (workerCount == 10 && scouts.size() ==0) {
		        	assignWorker(mineralWorkers.get(rand.nextInt(mineralWorkers.size())), scouts);
		        }
		        
		        /*
		         * game.drawBox(Enum.Map, bl.getRegion().getCenter().getX(), bl.getRegion().getCenter().getY(), bl.getRegion().getCenter().getX()+10, 
		    				bl.getRegion().getCenter().getY()+10, Color.Red, true);
		         */
		        
		        //Basic scouting behavior WIP
		       /*
		        for (Unit sc : scouts) {
		        	for (BaseLocation loc : baseLocations) {
		        		
		        		if (!sc.isMoving()) {
		        		if (!game.isExplored(loc.getRegion().getCenter().toTilePosition())) {
		        			sc.move(loc.getRegion().getCenter());
		        			game.drawTextMap(sc.getPosition().getX(), sc.getPosition().getY(), "SCOUT");
		        			break;
		        		}
		        		}
		        	}
		        	
		        	
		        }

*/

        for (Unit myUnit : self.getUnits()) {
        	//game.drawTextMap(myUnit.getPosition().getX(), myUnit.getPosition().getY(), "ID:" + myUnit.getID());
        	//game.drawTextMap(myUnit.getPosition().getX(), myUnit.getPosition().getY(), myUnit.getOrder().toString());
        	//game.drawLineMap(myUnit.getPosition().getX(), myUnit.getPosition().getY(), myUnit.getOrderTargetPosition().getX(), 
        		//	 myUnit.getOrderTargetPosition().getY(), bwapi.Color.Black);
        	int b = workerCount + workersInProduction;
        
        	//TODO experimental, test
        	if (myUnit.getType().isWorker()) {
        		if (myUnit.isUnderAttack()) {
        			Main.requests.putIfAbsent(myUnit.getID()+"D", new Request(myUnit, null, RequestType.DEFEND));	 //When to delete?
        		} else {
        			Main.requests.remove(myUnit.getID() + "D");
        		}
        	}
        	
        	
        	//if there's enough minerals, train an SCV        	
            if (myUnit.getType() == UnitType.Terran_Command_Center && availableMinerals >= 50 && b < targetWorkers+scouts.size()) { //TODO CC manager
            	trainUnit(myUnit, UnitType.Terran_SCV); 
            }
            
            if (myUnit.getType() == UnitType.Terran_Barracks && availableMinerals >=50) {
            	trainUnit(myUnit, UnitType.Terran_Marine);
            }
			if (myUnit.getType().isWorker() && myUnit.isCompleted()) {
				if (myUnit.isIdle() && !scouts.contains(myUnit)) { //TODO revise this
					assignWorker(myUnit, mineralWorkers);						
				} 
			}

        }
        
        
        requestScanIfNeeded();


        
        game.drawTextScreen(10, 25, statusMessages.toString());
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    }

    public static void main(String[] args) {
        new Main().run();
    }
    
    
    public void requestScanIfNeeded() {
    	
    	for (Unit enemy : enemyUnits) {
    		if (enemy.getType() == UnitType.Zerg_Lurker) {
    			if (enemy.isAttacking() && enemy.isBurrowed()) {
    				Command scan = new Command(CommandType.SCAN);
        			TilePosition tp = enemy.getTilePosition();
        			scan.setTargettilePosition(tp);
        			Request r = new Request(null, scan);
        			requests.put(tp.getX() + "scan" + tp.getY(), r);
        			//System.out.println("SCAN REQUESTED ON" + tp.getX() + tp.getY());
    			}
    				//duration:10, i have no idea what that means
    			//System.out.println("LURKER att:" + enemy.isAttacking() + " VISIBLE: " + enemy.isVisible() + " TaRGETable:" + enemy.isTargetable());
    			
    		}
    		//DUNNO LOL
    		if (enemy.isAttacking() && !enemy.isTargetable()) {
    			Command scan = new Command(CommandType.SCAN);
    			TilePosition tp = enemy.getTilePosition();
    			scan.setTargettilePosition(tp);
    			Request r = new Request(null, scan);
    			requests.put(tp.getX() + "scan" + tp.getY(), r);
    			System.out.println("SCAN REQUESTED ON" + tp.getX() + tp.getY());
    			
    		}
    	}
    }
    
    //Convenience method for getting a "random" available upgrader
    public UnitManager getUpgrader(UpgradeType upg) {
    	if (unitIDs.get(upg.whatUpgrades()) != null) {
    		for (Integer buildingID : unitIDs.get(upg.whatUpgrades())) {
    			if (unitManagers.get(buildingID).getUnit().canUpgrade(upg)) {
					return unitManagers.get(buildingID);
				}    			
    		}

		}
		return null;
    }
    
    //Convenience method for getting a "random" available upgrader
    public UnitManager getResearcher(TechType tech) {
    	if (unitIDs.get(tech.whatResearches()) != null) {
    		for (Integer buildingID : unitIDs.get(tech.whatResearches())) {
    			if (unitManagers.get(buildingID).getUnit().canResearch(tech)) {
					return unitManagers.get(buildingID);
				}
    			
    		}
		}
		return null;
    }
    
  //Convenience method for getting a "random" available addon builder
	public UnitManager getAddonBuilder(UnitType addonType) {
		System.out.println("ADDON");
		if (unitIDs.get(addonType.whatBuilds().first) != null) {
			for (Integer buildingID : unitIDs.get(addonType.whatBuilds().first)) {
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