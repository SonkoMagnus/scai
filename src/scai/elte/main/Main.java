package scai.elte.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.neuroph.core.Connection;
import org.neuroph.core.Neuron;
import org.neuroph.core.input.InputFunction;
import org.neuroph.nnet.comp.neuron.InputNeuron;

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
import bwapi.WeaponType;
import bwta.BWTA;
import bwta.BaseLocation;
import bwta.Region;
import scai.elte.command.BuildingManager;
import scai.elte.command.BunkerManager;
import scai.elte.command.Command;
import scai.elte.command.CommandType;
import scai.elte.command.ComsatManager;
import scai.elte.command.MarineManager;
import scai.elte.command.MedicManager;
import scai.elte.command.Request;
import scai.elte.command.Request.RequestType;
import scai.elte.command.RequestStatus;
import scai.elte.command.Squad;
import scai.elte.command.Squad.SquadOrder;
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
        mirror.startGame(false);
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
    public HashMap<Integer, EnemyPosition> enemyBuildingMemory = new HashMap<Integer, EnemyPosition>();
    
    /**
     * Frames passed
     */
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
    public static ConcurrentHashMap<Position, Integer> scannerPositions = new ConcurrentHashMap<Position, Integer>();
    public static int supplyUsedActual;
    public static Integer availableMinerals;
    public static Integer availableGas;
    
    private static Set<TilePosition> plannedPositions = new HashSet<TilePosition>();
    public static ConcurrentHashMap<String, Request> requests = new ConcurrentHashMap<String, Request>();
    
    public static HashMap<Integer, Unit> enemyUnits = new HashMap<Integer, Unit>();
    public static Set<BaseLocation> baseLocations = new HashSet<BaseLocation>();
    public static Set<Region> baseRegions = new HashSet<Region>();
    
    public HashMap<UnitType, HashSet<Integer>> unitManagerIDs = new HashMap<UnitType, HashSet<Integer>>(); //IDs by unit type, for quick access
    public HashMap<UnitType, Integer> targetUnitNumbers = new HashMap<UnitType, Integer>(); //Unit goals 
    public HashMap<Integer, Unit> scouts = new HashMap<Integer, Unit>(); //Scouting units
    private Random rand;
    private BaseLocation home;
    public static BaseLocation naturalExpansion;
    
	private Set<TilePosition> threatGround = new HashSet<TilePosition>();
	private Set<TilePosition> threatAir = new HashSet<TilePosition>();
	
    public static TreeSet<ScoutInfo> scoutHeatMap;
    
    //The first two squads to be built
    private Squad BioSquad = new Squad();
    private Squad BioSquad2 = new Squad();
    public ArrayList<Squad> army = new ArrayList<Squad>();
    
    //Parts of the neural network
    private InputNeuron incomeNeuron = new InputNeuron();
    private InputNeuron spendingNeuron  = new InputNeuron();
    private Neuron economyNeuron = new Neuron();
    private Neuron armyNeuron = new Neuron();
    
	@Override
	public void onUnitCreate(Unit unit) {
		if (unit.getPlayer() == self) {
			if (unit.getType() != UnitType.Resource_Mineral_Field && unit.getType() != UnitType.Resource_Vespene_Geyser
					&& unit.getType() != UnitType.Unknown) {
				assignUnitManager(unit);
			}
			// Reserve space for addons
			if (unit.getType() == UnitType.Terran_Command_Center) {
				Position addonCorner = new Position(unit.getPosition().getX() + 64, unit.getPosition().getY());
				plannedPositions.addAll(getTilesForBuilding(addonCorner, UnitType.Terran_Comsat_Station));
			}
		}
		countAllUnits();
		for (BuildOrderItem boi : buildOrder.buildOrderList) {
			if (boi.status == BuildOrderItemStatus.BUILD_PROCESS_STARTED) {
				if (unit.getType() == boi.getUnitType()) {
					boi.status = BuildOrderItemStatus.UNDER_CONSTRUCTION;
					reservedMineralsInQueue -= boi.getUnitType().mineralPrice();
					reservedGasInQueue -= boi.getUnitType().gasPrice();
					reservedMinerals = reservedMinerals - boi.getUnitType().mineralPrice();
					reservedGas = reservedGas - boi.getUnitType().gasPrice();
					break;
				}
			}
		}
	}
    
    public void assignUnitManager(Unit unit) {
    	if (unit.getType() == UnitType.Terran_Bunker) {
    		unitManagers.putIfAbsent(unit.getID(), new BunkerManager(unit));
    	} else if (unit.getType() == UnitType.Terran_Marine) {
    		unitManagers.putIfAbsent(unit.getID(), new MarineManager(unit));
    	} else if (unit.getType() == UnitType.Terran_Medic) {
    		unitManagers.putIfAbsent(unit.getID(), new MedicManager(unit));
    	} else if (unit.getType() == UnitType.Terran_Comsat_Station) {
    		unitManagers.putIfAbsent(unit.getID(), new ComsatManager(unit));
    	} else if (unit.getType().isBuilding()) {
    		unitManagers.putIfAbsent(unit.getID(), new BuildingManager(unit));
    	} else if (unit.getType() == UnitType.Terran_SCV) {
    		unitManagers.putIfAbsent(unit.getID(), new WorkerManager(unit, WorkerRole.MINERAL));
    	}  else {
    		unitManagers.putIfAbsent(unit.getID(), new UnitManager(unit));
    	}
    	unitManagerIDs.putIfAbsent(unit.getType(), new HashSet<Integer>());
    	unitManagerIDs.get(unit.getType()).add(unit.getID());
    }

    public void countAllUnits() {
    	unitCounts = new HashMap<UnitType, Integer>();
        unitsInProduction = new HashMap<UnitType, Integer>();
        supplyUsedActual = 0;
            for (Unit myUnit : self.getUnits()) 
	        {
            	supplyUsedActual += myUnit.getType().supplyRequired();
	        	if(!unitCounts.containsKey(myUnit.getType())) 
	        		unitCounts.put(myUnit.getType(), 1);
	        	else
	        		unitCounts.put(myUnit.getType(), unitCounts.get(myUnit.getType())+1);
	        	
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
    	if (unit.getPlayer() == self) {
    	assignUnitManager(unit);
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
    	} else if (!unit.getType().isNeutral() && ! unit.getType().isSpecialBuilding()) {
    		if (unit.getType().isBuilding()) {
    			enemyBuildingMemory.put((Integer)unit.getID(), new EnemyPosition(unit.getPosition(), unit.getType()));
    			updateThreatMap();
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
     	countAllUnits();
        //Use BWTA to analyze map
        //This may take a few minutes if the map is processed first time!
        System.out.println("Analyzing map...");
        BWTA.analyze();  
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
        	}
        }  
        System.out.println(baseLocations);
        
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

        targetUnitNumbers.putIfAbsent(UnitType.Terran_Marine, 26);
        targetUnitNumbers.putIfAbsent(UnitType.Terran_SCV, 24);
        targetUnitNumbers.putIfAbsent(UnitType.Terran_Medic, 6);       
        BioSquad2.getTargetComposition().put(UnitType.Terran_Marine, 20);
        BioSquad.getTargetComposition().put(UnitType.Terran_Marine, 16);
        BioSquad.getTargetComposition().put(UnitType.Terran_Medic, 6);
        army.add(BioSquad);
        army.add(BioSquad2);
        setupNeuralNetwork();
    	}
    	catch (Exception ex) {
    		ex.printStackTrace();
    	}
    }
    @Override
    public void onEnd(boolean b) {
    	//super.onEnd(b);
    	System.out.println("That's all folks, frames: " + frameCount);
    }

	@Override
	public void onUnitDestroy(Unit unit) {
		if (unit.getPlayer() != self) {
			enemyBuildingMemory.remove(unit.getID());
			updateThreatMap();
		}
		if (unit.getPlayer() == self && !unit.getType().isNeutral()) {
			unitManagerIDs.get(unit.getType()).remove(Integer.valueOf(unit.getID()));
			unitManagers.remove(unit.getID());
			if (scouts.containsKey(unit.getID())) {
				scouts.remove(unit.getID());
			}
			for (Squad s : army) {
				if (s.getMembers().contains(unit)) {
					s.removeMember(unit);
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
        if (unit.getPlayer() == self && !unit.getType().isNeutral()) {
        for (Squad s : army) {
        	if (!s.fullStrength()) {
        		if (s.getActualComposition().getOrDefault(unit.getType(),0) < s.getTargetComposition().getOrDefault(unit.getType(), 0)) {
        			s.assignMember(unit);
        			break;
        		}
        	}
        }
        }
    }
    
    public void assignWorkerRole(Unit worker, WorkerRole role) {
    	if ( ((WorkerManager)unitManagers.get(worker.getID())).getRole() != role) {
    	((WorkerManager)unitManagers.get(worker.getID())).setPrevRole(((WorkerManager)unitManagers.get(worker.getID())).getRole());
    	((WorkerManager)unitManagers.get(worker.getID())).setRole(role);
    	}
    }
    
    //Convenience method 
    public void assignWorkerRoles(Collection<Unit> workers, WorkerRole role) {
    	for (Unit worker : workers) {
    		assignWorkerRole(worker, role);
    	}
    }
    
    public Unit getWorkerFromRole(WorkerRole prevRole, WorkerRole role) {
    	//assign random
        HashSet<Integer> randomIds = new HashSet<Integer>(unitManagerIDs.get(UnitType.Terran_SCV));
    	while (randomIds.size() > 0) {
    	Integer d = rand.nextInt(randomIds.size()-1);
    	Integer wmid = null;
    	for (int i=0; i<=d; i++ ) {
    		wmid = randomIds.iterator().next();
    	}
    	randomIds.remove(wmid);
   		WorkerManager wm = (WorkerManager)unitManagers.get(wmid);

   		if (wm.getUnit().exists() && wm.getUnit().isCompleted() && wm.getRole() == prevRole) {
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
    		enemyUnits.putIfAbsent(unit.getID(), unit);
    		if (enemyBuildingMemory.containsKey(unit.getID())) {
    			EnemyPosition enemyBuildingPos = enemyBuildingMemory.get(unit.getID());
    			if (enemyBuildingPos.getType() != unit.getType()) {
    				enemyBuildingPos.setType(unit.getType()); //Morphing changes unit type
    				updateThreatMap();
    			} 
    			if (!enemyBuildingPos.getPosition().equals(unit.getPosition()) && !enemyUnits.get(unit.getID()).isLifted()) { //Terran building floated away - only update position, if landed
    				enemyBuildingPos.setPosition(unit.getPosition());
    				updateThreatMap();
    			}	
    		}	
    	}
    }
    
    @Override
    public void onUnitHide(Unit unit) {
    	if (unit.getPlayer() != self && !unit.getType().isNeutral()) {
        	enemyUnits.remove(unit.getID());
    	}
    }
        
    @Override
    public void onUnitDiscover(Unit unit) {
    	if (unit.getType() == UnitType.Spell_Scanner_Sweep && unit.getPlayer() == self) {
    		scannerPositions.put(unit.getPosition(), 262);
    	}
    	if (unit.getPlayer() != self && !unit.getType().isNeutral() && unit.getType().isBuilding()) {
    		enemyBuildingMemory.put(unit.getID(), new EnemyPosition(unit.getPosition(), unit.getType()));
    		updateThreatMap();
    	}
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
        statusMessages.append("Frame count:" + frameCount+ "\n");
        statusMessages.append("APM:" + game.getAPM() + "\n");
        statusMessages.append("Income:" + (double)((double) self.gatheredMinerals() / (double) frameCount) + "\n");
        
        double income = ((double) self.gatheredMinerals() / (double) frameCount);
        double spending =-((double) self.spentMinerals() / (double) frameCount);
        //statusMessages.append("Spending:" + spending + "\n");
        incomeNeuron.setInput(income);
        spendingNeuron.setInput(spending);;
        
        incomeNeuron.calculate();
        economyNeuron.calculate();
        spendingNeuron.calculate();
        armyNeuron.calculate();
        
        
        //The set build order has executed, now improve the build according to the neural network - to avoid spamming, only check every 2000 frames
        if (buildOrder.getSupplyExecuted() < supplyUsedActual && frameCount % 2000 == 0) {
        	if (economyNeuron.getOutput()==1) {
        		targetUnitNumbers.get(UnitType.Terran_SCV);
        		targetUnitNumbers.put(UnitType.Terran_SCV, targetUnitNumbers.get(UnitType.Terran_SCV)+1);
        	}
        	
        	if (armyNeuron.getOutput() == 1) {
        		boolean build = true;
        		for (Squad s : army) {
        			if (!s.fullStrength()) {
        				build = false;
        				break;
        			}
        		}

        		targetUnitNumbers.put(UnitType.Terran_Marine, targetUnitNumbers.get(UnitType.Terran_Marine)+8);
        		Squad marineSquad = new Squad();
        		marineSquad.getTargetComposition().put(UnitType.Terran_Marine, 8);
        		if (build) {
        		buildOrder.addItem(UnitType.Terran_Barracks, supplyUsedActual, 1);
        		if (self.supplyTotal() < 400) { 
        			buildOrder.addItem(UnitType.Terran_Supply_Depot, supplyUsedActual, 1);
        		}
        		army.add(marineSquad);
        		}
        	}
        }
        
        updateScannedPositions();        
        ageHeatMap();
        assignScouting();
        checkUpgrades();	  
        checkSupplyExtension();
    	manageRequests();
    	manageBuildQueue();
			
    	for (Integer i : unitManagers.keySet()) {
    		unitManagers.get(i).operate();
    	}
    	//For every unit target, get a producer building, and train unit TODO maybe importance of different units?
    	trainRequiredUnits();
    	requestScanIfNeeded();
        //Army management - all full squads must attack the nearest enemy building
        manageArmy();	
    	fillSquadsWithUnassigned();
        game.drawTextScreen(10, 25, statusMessages.toString());        
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    
    }

    public static void main(String[] args) {
        new Main().run();
    }
    
    public void setupNeuralNetwork() {
        economyNeuron.addInputConnection(incomeNeuron,1);
        economyNeuron.addInputConnection(spendingNeuron,1);
        economyNeuron.setInputFunction(new InputFunction() {	
			private static final long serialVersionUID = 6739309684907310952L;

			@Override
			public double getOutput(Connection[] cns) {
				double sum = 0;
				for (Connection c : cns) {
					sum += c.getWeightedInput();
				}
				if (sum<=0) {
					return 1; //Balance negative, improve economy
				} else {
					return 0; //Balance positive, nothing to do
				}
			}
		});    
        armyNeuron.addInputConnection(incomeNeuron,1);
        armyNeuron.addInputConnection(spendingNeuron,1.2); 
        armyNeuron.setInputFunction(new InputFunction() {
			private static final long serialVersionUID = 2936270777689108667L;

			@Override
			public double getOutput(Connection[] cns) {
			
				double sum = 0;
				for (Connection c : cns) {
					sum += c.getWeightedInput();
				}
				if (sum<=0) {
					return 0; //Balance negative, do nothing
				} else {
					return 1; //Balance positive, improve the army
				}
			}
		});
    }
    
    public void manageRequests() {
		for (String rk : requests.keySet()) {
			Request r = requests.get(rk);
			if (r.getRequestStatus() == RequestStatus.FULFILLED) {
				requests.remove(rk);
			}
			if (r.getType() == RequestType.COMMAND) {
				if (r.getRequestedCommand().getType() == CommandType.GAS_WORKER
						&& r.getRequestStatus() == RequestStatus.NEW) {
					Unit gasWorker = getWorkerFromRole(WorkerRole.MINERAL, WorkerRole.GAS);
					r.setAnsweringUnit(gasWorker);
					r.setRequestStatus(RequestStatus.BEING_ANSWERED);
				}
			} else if (r.getType() == RequestType.DEFEND && r.getRequestStatus() == RequestStatus.NEW) {
				// Check if there is an army present in the region
				boolean armyAvailable = false;
				ArrayList<Unit> workersInRegion = new ArrayList<Unit>();
				Region def = MapUtil.getRegionOfUnit(r.getRequestingUnit());
				if (def != null) {
					Set<Unit> unitsInRegion = MapUtil.getUnitsInRegion(def);
					for (Unit u : unitsInRegion) {
						if (!u.getType().isBuilding()) {
							if (u.getPlayer() == self) {
								if (!u.getType().isWorker()) {
									armyAvailable = true;
								} else {
									workersInRegion.add(u);
								}
							}
						}
						if (armyAvailable) {				
						} else {
							assignWorkerRoles(workersInRegion, WorkerRole.MILITIA);
							r.setRequestStatus(RequestStatus.BEING_ANSWERED);
						}
					}

				}
			}
		}
    }
    
    public void manageBuildQueue() {
    	if (unitCounts.getOrDefault(UnitType.Terran_SCV, 0) > 1) {
			for (BuildOrderItem boi : buildOrder.buildOrderList) {
				UnitType buildingType = boi.getUnitType();
				if (boi.getSupplyThreshold() <= supplyUsedActual && boi.status == BuildOrderItemStatus.PLANNED) {
					// Reserve minerals
					reservedMinerals = reservedMinerals + buildingType.mineralPrice();
					reservedGas = reservedGas + buildingType.gasPrice();
					boi.status = BuildOrderItemStatus.IN_QUEUE;
				}
				if (boi.status == BuildOrderItemStatus.IN_QUEUE) {
				}
			if (boi.status == BuildOrderItemStatus.IN_QUEUE
						&& (self.minerals() - reservedMineralsInQueue) >= buildingType.mineralPrice()
						&& self.gas() - reservedGasInQueue >= buildingType.gasPrice()
						&& game.canMake(buildingType)) {
					if (buildingType.isAddon()) {
						if (unitManagerIDs.get(buildingType.whatBuilds().first) != null) {
							BuildingManager addonBuilder = ((BuildingManager) getAddonBuilder(buildingType));
							if (addonBuilder != null) {
								addonBuilder.setAddon(new Pair<>(buildingType, false)); 
								boi.status = BuildOrderItemStatus.BUILD_PROCESS_STARTED;
							}
						}
					} else {
						Unit mb = getWorkerFromRole(WorkerRole.MINERAL, WorkerRole.BUILD); 
						TilePosition aroundTile;
						TilePosition buildTile = null;
						if (boi.getTilePosition() == null) {
								aroundTile = self.getStartLocation();
								buildTile = getBuildTile(mb, buildingType, aroundTile);			
							boi.setTilePosition(buildTile);
						} else {
							if (!game.canBuildHere(boi.getTilePosition(), boi.getUnitType())) {
								aroundTile = boi.getTilePosition();
								buildTile = getBuildTile(mb, buildingType, aroundTile);
								boi.setTilePosition(buildTile);
							} 
						}	
						mb.build(boi.getUnitType(), boi.getTilePosition()); 							
						((WorkerManager)unitManagers.get(mb.getID())).setTargetTile(boi.getTilePosition());
						reservedMineralsInQueue = reservedMineralsInQueue + boi.getUnitType().mineralPrice();	
						reservedGasInQueue += boi.getUnitType().gasPrice();
						boi.status = BuildOrderItemStatus.BUILD_PROCESS_STARTED;
						break;
					}
				}
			}
		}
    }
    
    public void manageArmy() {
		Position nearestEnemyBuildingPos = null;
		double minDist = Double.MAX_VALUE;
		for (EnemyPosition ep : enemyBuildingMemory.values()) {
			if (minDist > ep.getPosition().getDistance(home.getPoint())) {
				minDist = ep.getPosition().getDistance(home.getPoint());
				nearestEnemyBuildingPos = ep.getPosition();
			}
		}
		if (nearestEnemyBuildingPos != null) {
			game.drawBoxMap(nearestEnemyBuildingPos, new Position(nearestEnemyBuildingPos.getX()+20, nearestEnemyBuildingPos.getY()+20), Color.Orange);
			for (Squad s : army) {
				if (s.fullStrength()) {
					s.currentOrder = SquadOrder.ATTACK_POSITION;
					s.setTargetPosition(nearestEnemyBuildingPos);
					s.setOrderFrame(frameCount);
				}
			}
		}
		//Squad orders must be executed in any case
		for (Squad s : army) {
			s.executeSquadOrder();
		}
    }
    
    public void checkUpgrades() {
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
    }
    
    public void assignScouting() {
        if (scouts.size() < 1 && unitCounts.getOrDefault(UnitType.Terran_SCV, 0) >= 10 && enemyBuildingMemory.size() < 1) {
        	Unit worker = getWorkerFromRole(WorkerRole.MINERAL, WorkerRole.SCOUT);
        	assignWorkerRole(worker, WorkerRole.SCOUT);
        	scouts.put(worker.getID(), worker);
        }
    }
    
    
    public void fillSquadsWithUnassigned() {
    	for (Squad s : army) {
    		if (!s.fullStrength()) {
    			for (UnitType ut : s.getActualComposition().keySet()) {
    				if (s.getActualComposition().get(ut) < s.getTargetComposition().get(ut)) {
    					for (Integer i : unitManagerIDs.get(ut)) {
    						if (unitManagers.get(i).getSquad() == null) {
    							s.assignMember(unitManagers.get(i).getUnit());
    							break;
    						};
    					}
    				}
    			}
    		}
    	}
    }
    
    public void checkSupplyExtension() {
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
    }
    
    public void updateThreatMap() {
    	for (EnemyPosition b : enemyBuildingMemory.values()) {
    		//Get tiles in range, update threat map
    		if (b.getType().airWeapon() != WeaponType.None) {
    			threatAir.addAll(MapUtil.getTilePositionsInRadius(b.getPosition(), b.getType().airWeapon().maxRange()));
    		}
    		if (b.getType().groundWeapon() != WeaponType.None) {
    			threatGround.addAll(MapUtil.getTilePositionsInRadius(b.getPosition(), b.getType().groundWeapon().maxRange()));
    		}
    	}
    }
    
    
    public void ageHeatMap() {
    	if (frameCount % 20 == 0) { //Speed up, for debug purposes
    	for (ScoutInfo sc : scoutHeatMap) {
    		if (threatGround.contains(sc.getTile())) {
    			sc.setThreatenedByGround(true);
    		} else {
    			sc.setThreatenedByGround(false);
    		}
    		if (threatAir.contains(sc.getTile())) {
    			sc.setThreatenedByAir(true);
    		} else {
    			sc.setThreatenedByAir(false);
    		}
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
    	for (Unit enemy : enemyUnits.values()) {
    		if (enemy.getType() == UnitType.Zerg_Lurker) {
    			if (enemy.isAttacking() && enemy.isBurrowed()) {
    				TilePosition tp = enemy.getTilePosition();
    				String key = tp.getX() + "sc" + tp.getY();
    				if (!requests.containsKey(key)) { 
    				Command scan = new Command(CommandType.SCAN);
        			
        			scan.setTargettilePosition(tp);
        			Request r = new Request(null, scan);
        			requests.putIfAbsent(tp.getX() + "sc" + tp.getY(), r); 
    				}
    			}
	
    		}
    		if (enemy.isAttacking() && (!enemy.isTargetable() || enemy.getType().hasPermanentCloak())) {
    			
    			Command scan = new Command(CommandType.SCAN);
    			TilePosition tp = enemy.getTilePosition();
    			scan.setTargettilePosition(tp);
    			Request r = new Request(null, scan);
    			requests.put(tp.getX() + "scan" + tp.getY(), r);				
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
		if (unitManagerIDs.get(addonType.whatBuilds().first) != null) {
			for (Integer buildingID : unitManagerIDs.get(addonType.whatBuilds().first)) {
				if (unitManagers.get(buildingID).getUnit().canBuild(addonType)) {
					return unitManagers.get(buildingID);
				}
			}
		}
		return null;
	} 
    
    public static TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile) {
    	TilePosition ret = null;
    	int maxDist = 3;
    	int stopDist = 40;
    	
    	if (aroundTile == null) {
    		aroundTile = self.getStartLocation();
    	}
    	
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
    				boolean canBuild = true;
    				for (TilePosition tp : getTilesForBuilding(targetTile.toPosition(), buildingType)){
    					if (plannedPositions.contains(tp)) {
    						canBuild = false;
    					}
    				}

    				if (game.canBuildHere(new TilePosition(i,j), buildingType, builder, false) && canBuild) {
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
    
    //Starting from the given position, returns which building tiles will the building occupy.
    public static HashSet<TilePosition> getTilesForBuilding(Position pos, UnitType type) {
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
    
}	