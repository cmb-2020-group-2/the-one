/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;
import input.WKTReader;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.SimMap;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * A Class to model movement when people are out shopping or doing other
 * activities with friends. If the node happens to be at some other location
 * than the place where the shopping starts (where it meets its friends), it
 * first travels to the destination along the shortest path.
 *
 * @author Frans Ekman
 */
public class BreakActivityMovement extends MapBasedMovement
        implements SwitchableMovement {

    private static final int WALKING_TO_BREAK_MODE = 0;
    private static final int BREAK_MODE = 1;

    public static final String NR_OF_TABLE_SETTING = "nrOfTables";
    public static final String BREAK_ACTIVITY_CONTROL_SYSTEM_NR_SETTING =
            "breakControlSystemNr";

    public static final String TABLE_FILE_SETTING = "tablesFile";

    public static final String MIN_GROUP_SIZE_SETTING = "minGroupSize";
    public static final String MAX_GROUP_SIZE_SETTING = "maxGroupSize";

    public static final String MIN_WAIT_TIME_SETTING =
            "minAfterBreakStopTime";
    public static final String MAX_WAIT_TIME_SETTING =
            "maxAfterBreakStopTime";

    private static int nrOfTables = 5;

    private int mode;
    private boolean ready;
    private DijkstraPathFinder pathFinder;

    private Coord lastWaypoint;
    private Coord startAtLocation;

    private BreakActivityControlSystem scs;
    private BreakTrip trip;

    private boolean readyToBreak;

    private int id;

    private static int nextID = 0;

    private int minGroupSize;
    private int maxGroupSize;

    /**
     * Creates a new instance of EveningActivityMovement
     *
     * @param settings
     */
    public BreakActivityMovement(Settings settings) {
        super(settings);
        super.backAllowed = false;
        pathFinder = new DijkstraPathFinder(null);
        mode = WALKING_TO_BREAK_MODE;

        nrOfTables = settings.getInt(NR_OF_TABLE_SETTING);

        minGroupSize = settings.getInt(MIN_GROUP_SIZE_SETTING);
        maxGroupSize = settings.getInt(MAX_GROUP_SIZE_SETTING);

        MapNode[] mapNodes = (MapNode[]) getMap().getNodes().
                toArray(new MapNode[0]);

        String tablesFile = null;
        try {
            tablesFile = settings.getSetting(TABLE_FILE_SETTING);
        } catch (Throwable t) {
            // Do nothing;
        }

        List<Coord> tableSpotLocations = null;

        if (tablesFile == null) {
            tableSpotLocations = new LinkedList<Coord>();
            for (int i = 0; i < mapNodes.length; i++) {
                if ((i % (mapNodes.length / nrOfTables)) == 0) {
                    startAtLocation = mapNodes[i].getLocation().clone();
                    tableSpotLocations.add(startAtLocation.clone());
                }
            }
        } else {
            try {
                tableSpotLocations = new LinkedList<Coord>();
                List<Coord> locationsRead = (new WKTReader()).readPoints(
                        new File(tablesFile));
                for (Coord coord : locationsRead) {
                    SimMap map = getMap();
                    Coord offset = map.getOffset();
                    // mirror points if map data is mirrored
                    if (map.isMirrored()) {
                        coord.setLocation(coord.getX(), -coord.getY());
                    }
                    coord.translate(offset.getX(), offset.getY());
                    tableSpotLocations.add(coord);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.id = nextID++;

        int scsID = settings.getInt(BREAK_ACTIVITY_CONTROL_SYSTEM_NR_SETTING);

        scs = BreakActivityControlSystem.getBreakActivityControlSystem(scsID);
        scs.setRandomNumberGenerator(rng);
        scs.addBreakActivityNode(this);
        scs.setTableSpots(tableSpotLocations);

        maxPathLength = 100;
        minPathLength = 10;

        maxWaitTime = settings.getInt(MAX_WAIT_TIME_SETTING);
        minWaitTime = settings.getInt(MIN_WAIT_TIME_SETTING);
    }

    /**
     * Creates a new instance of EveningActivityMovement from a prototype
     *
     * @param proto
     */
    public BreakActivityMovement(BreakActivityMovement proto) {
        super(proto);
        this.pathFinder = proto.pathFinder;
        this.mode = proto.mode;
        this.id = nextID++;
        scs = proto.scs;
        scs.addBreakActivityNode(this);
        this.setMinGroupSize(proto.getMinGroupSize());
        this.setMaxGroupSize(proto.getMaxGroupSize());
    }

    /**
     * @return Unique ID of the shopper
     */
    public int getID() {
        return this.id;
    }

    @Override
    public Coord getInitialLocation() {

        MapNode[] mapNodes = (MapNode[]) getMap().getNodes().
                toArray(new MapNode[0]);
        int index = rng.nextInt(mapNodes.length - 1);
        lastWaypoint = mapNodes[index].getLocation().clone();
        return lastWaypoint.clone();
    }

    @Override
    public Path getPath() {
        if (mode == WALKING_TO_BREAK_MODE) {
            // Try to find to the shopping center
            SimMap map = super.getMap();
            if (map == null) {
                return null;
            }
            MapNode thisNode = map.getNodeByCoord(lastWaypoint);
            MapNode destinationNode = map.getNodeByCoord(startAtLocation);

            List<MapNode> nodes = pathFinder.getShortestPath(thisNode,
                    destinationNode);
            Path path = new Path(generateSpeed());
            for (MapNode node : nodes) {
                path.addWaypoint(node.getLocation());
            }
            lastWaypoint = startAtLocation.clone();
            mode = BREAK_MODE;
            return path;
        } else if (mode == BREAK_MODE) {
            readyToBreak = true;
            Path path = trip.getPath();
            if (path == null) {
                super.lastMapNode = super.getMap().
                        getNodeByCoord(lastWaypoint);
                path = super.getPath(); // TODO Create levy walk path
                lastWaypoint = super.lastMapNode.getLocation();
                trip.setPath(path);
                double waitTimeAtEnd = (maxWaitTime - minWaitTime) *
                        rng.nextDouble() + minWaitTime;

//                System.out.println(waitTimeAtEnd);
                trip.setWaitTimeAtEnd(waitTimeAtEnd);
                trip.setDestination(lastWaypoint);
            }
            lastWaypoint = trip.getDestination();
            ready = true;
            return path;
        }

        return null;
    }

    @Override
    protected double generateWaitTime() {
        if (ready) {
            double wait = trip.getWaitTimeAtEnd();
            return wait;
        } else {
            return 0;
        }
    }

    @Override
    public MapBasedMovement replicate() {
        return new BreakActivityMovement(this);
    }

    /**
     * @see SwitchableMovement
     */
    public Coord getLastLocation() {
        return lastWaypoint.clone();
    }

    /**
     * @see SwitchableMovement
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * @see SwitchableMovement
     */
    public void setLocation(Coord lastWaypoint) {
        this.lastWaypoint = lastWaypoint.clone();
        ready = false;
        mode = WALKING_TO_BREAK_MODE;
    }

    /**
     * Sets the node ready to start a shopping trip.
     *
     * @return The coordinate of the place where the shopping trip starts
     */
    public Coord getBreakLocationAndGetReady() {
        readyToBreak = false; // New shopping round starts
        trip = scs.getBreakInstructions(id);
        startAtLocation = trip.getLocation().clone();
        return startAtLocation.clone();
    }

    /**
     * Checks if a node is at the correct place where the shopping begins
     *
     * @return true if node is ready and waiting for the rest of the group to
     * arrive
     */
    public boolean isReadyToBreak() {
        return readyToBreak;
    }

    public static void reset() {
        nextID = 0;
    }

    public int getMinGroupSize() {
        return minGroupSize;
    }

    public void setMinGroupSize(int minGroupSize) {
        this.minGroupSize = minGroupSize;
    }

    public int getMaxGroupSize() {
        return maxGroupSize;
    }

    public void setMaxGroupSize(int maxGroupSize) {
        this.maxGroupSize = maxGroupSize;
    }

}
