package scai.elte.main;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;

public class Main extends DefaultBWListener {
	

    private Mirror mirror = new Mirror();

    private Game game;

    private Player self;

    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame(true);
    }

    @Override
    public void onUnitCreate(Unit unit) {
        System.out.println("New unit discovered " + unit.getType());
        countAllUnits();
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
    public Integer workers = 0;
    
    /**
     * Current targeted worker count
     */
    
    public Integer targetWorkers = 9;
    
    /**
     * The maximum number of workers that is to be constructed.
     */
	public int maximumWorkers = 45;
	
    /**
     * Keeps track of how many units of each unit type the player has.
     */
    public HashMap<UnitType, Integer> unitCounts = new HashMap<UnitType, Integer>();
    
    /**
     * Keeps track of how many units are in production
     */
    public HashMap<UnitType, Integer> unitsInProduction = new HashMap<UnitType, Integer>();

    public void countAllUnits() {
    	unitCounts = new HashMap<UnitType, Integer>();
        unitsInProduction = new HashMap<UnitType, Integer>();

    		workers =0;
            for (Unit myUnit : self.getUnits()) 
	        {
	        	if(!unitCounts.containsKey(myUnit.getType())) 
	        		unitCounts.put(myUnit.getType(), 1);
	        	else
	        		unitCounts.put(myUnit.getType(), unitCounts.get(myUnit.getType())+1);
	        	
	        	if(myUnit.getType() == UnitType.Terran_SCV) {
	        		workers++;
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
    /*
    public int countProd(UnitType type)
    {
    	Integer result = unitsInProduction.get(type);
    	if(result == null)
    		return 0;
    	else
    		return result;
    }
    
    
    public void countUnitsInProductin(UnitType type) {
    	  for (Unit myUnit : self.getUnits()) {
	        	if (myUnit.getType().isBuilding()) {
	        		List<UnitType> trimList = myUnit.getTrainingQueue();
	        		/*
	        		if (trimList.size() > 0 ) {
	        			trimList.remove(0);
	        		}
	     		
	        		if (trimList.contains(UnitType.Terran_SCV)) {
	        			System.out.println("blep");
	        		}
	        		
	        		if (trimList.contains(type)) {
	        			System.out.println("blep2");
	        		}
	        	}
    	  }
    }
*/
    @Override
    public void onStart() {
    	try {
    	System.out.println("START");
        game = mirror.getGame();
        self = game.self();
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
    	
    }

    
    @Override
    public void onFrame() {
    	try {
    	
    	countAllUnits();
        //game.setTextSize(10);
        game.drawTextScreen(10, 10, "Playing as " + self.getName() + " - " + self.getRace());

        StringBuilder statusMessages = new StringBuilder("My units:\n");
        Integer availableMinerals = self.minerals() - reservedMinerals;
        statusMessages.append("Available minerals:" + availableMinerals.toString() + "\n");
        statusMessages.append("Current worker count:" + workers.toString() + "\n");
        statusMessages.append("Target worker count:" + targetWorkers.toString() + "\n");



	   	int workersInProduction = 0;
    	if (unitsInProduction.get(UnitType.Terran_SCV) != null) {
    		workersInProduction = unitsInProduction.get(UnitType.Terran_SCV);
    	}
	//	System.out.println("DEBÜG3_würkürs_in_pröd:" + workersInProduction); 
       
        for (Unit myUnit : self.getUnits()) {
            //units.append(myUnit.getType()).append(" ").append(myUnit.getTilePosition()).append("\n");
        	
        	
        	System.out.println("DDD:" + workersInProduction);
        	int b = workers + workersInProduction;
            
        	
            //if there's enough minerals, train an SCV
            if (myUnit.getType() == UnitType.Terran_Command_Center && availableMinerals >= 50 
            		&& b < targetWorkers) {
            	 System.out.println("DEBUG_w:" + workers);
            	 System.out.println("DEBUG_wp:" + workersInProduction);
            	 System.out.println("DEBUG_f:" + (workers+workersInProduction));
                myUnit.train(UnitType.Terran_SCV);
                //System.out.println(myUnit.getTrainingQueue());
                
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
            }
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
}	