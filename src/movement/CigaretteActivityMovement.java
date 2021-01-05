/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;
import core.SimClock;
import input.WKTReader;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.SimMap;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Frans Ekman
 */
public class CigaretteActivityMovement extends MapBasedMovement
        implements SwitchableMovement {

    private static final int WALKING_TO_CIGARETTE_MODE = 0;
    private static final int CIGARETTE_MODE = 1;

    public static final String CIGARETTE_LENGTH_SETTING = "cigaretteLength";
    public static final String NR_OF_CIGARETTE_SPOT_SETTING = "nrOfCigaretteSpots";

    public static final String CIGARETTE_FILE_SETTING = "cigaretteSpotsFile";
    public static final String CIGARETTE_SPOT_SIZE_SETTING = "cigaretteSpotSize";

    private static int nrOfCigaretteSpots = 2;

    private int distance;
    private int mode;
    private int[] cigaretteLength;
    private double specificWaitTime;
    private int startedCigaretteTime;
    private boolean ready;
    private DijkstraPathFinder pathFinder;

    private List<Coord> allCigaretteSpots;

    private Coord lastWaypoint;
    private Coord cigaretteSpotLocation;

    /**
     * Creates a new instance of EveningActivityMovement
     *
     * @param settings
     */
    public CigaretteActivityMovement(Settings settings) {
        super(settings);
        super.backAllowed = false;
        pathFinder = new DijkstraPathFinder(null);
        mode = WALKING_TO_CIGARETTE_MODE;

        distance = settings.getInt(CIGARETTE_SPOT_SIZE_SETTING);


        nrOfCigaretteSpots = settings.getInt(NR_OF_CIGARETTE_SPOT_SETTING);

        cigaretteLength = settings.getCsvInts(CIGARETTE_LENGTH_SETTING);

        startedCigaretteTime = -1;

        String cigaretteFile = null;
        try {
            cigaretteFile = settings.getSetting(CIGARETTE_FILE_SETTING);
        } catch (Throwable t) {
            // Do nothing;
        }

        try {
            allCigaretteSpots = new LinkedList<Coord>();
            List<Coord> locationsRead = (new WKTReader()).
                    readPoints(new File(cigaretteFile));
            for (Coord coord : locationsRead) {
                SimMap map = getMap();
                Coord offset = map.getOffset();
                // mirror points if map data is mirrored
                if (map.isMirrored()) {
                    coord.setLocation(coord.getX(), -coord.getY());
                }
                coord.translate(offset.getX(), offset.getY());
                allCigaretteSpots.add(coord);
            }
            cigaretteSpotLocation = allCigaretteSpots.get(
                    rng.nextInt(allCigaretteSpots.size())).clone();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new instance of EveningActivityMovement from a prototype
     *
     * @param proto
     */
    public CigaretteActivityMovement(CigaretteActivityMovement proto) {
        super(proto);
        this.cigaretteLength = proto.cigaretteLength;
        startedCigaretteTime = -1;
        this.distance = proto.distance;
        this.pathFinder = proto.pathFinder;
        this.mode = proto.mode;

        this.allCigaretteSpots = proto.allCigaretteSpots;
        cigaretteSpotLocation = allCigaretteSpots.get(
                rng.nextInt(allCigaretteSpots.size())).clone();
    }

    @Override
    public Coord getInitialLocation() {
        double x = rng.nextDouble() * getMaxX();
        double y = rng.nextDouble() * getMaxY();
        Coord c = new Coord(x, y);

        this.lastWaypoint = c;
        return c.clone();
    }

    @Override
    public Path getPath() {
        if (mode == WALKING_TO_CIGARETTE_MODE) {
            // Try to find to the office
            SimMap map = super.getMap();
            if (map == null) {
                return null;
            }
            MapNode thisNode = map.getNodeByCoord(lastWaypoint);
            MapNode destinationNode = map.getNodeByCoord(cigaretteSpotLocation);
            List<MapNode> nodes = pathFinder.getShortestPath(thisNode,
                    destinationNode);
            Path path = new Path(generateSpeed());
            for (MapNode node : nodes) {
                path.addWaypoint(node.getLocation());
            }

            Coord c;
            c = getRandomCoorinateInsideClass();
            path.addWaypoint(c);

            lastWaypoint = cigaretteSpotLocation.clone();
            mode = CIGARETTE_MODE;
            return path;
        }
        if (startedCigaretteTime == -1) {
            startedCigaretteTime = SimClock.getIntTime();
        }
        if (SimClock.getIntTime() >= startedCigaretteTime + specificWaitTime) {
            Path path = new Path(1);
            path.addWaypoint(lastWaypoint.clone());
            ready = true;
            return path;
        }
        return null;
    }

    public Coord getRandomCoorinateInsideClass() {
        double x_coord = cigaretteSpotLocation.getX() +
                (0.5 - rng.nextDouble()) * distance;
        if (x_coord > getMaxX()) {
            x_coord = getMaxX();
        } else if (x_coord < 0) {
            x_coord = 0;
        }
        double y_coord = cigaretteSpotLocation.getY() +
                (0.5 - rng.nextDouble()) * distance;
        if (y_coord > getMaxY()) {
            y_coord = getMaxY();
        } else if (y_coord < 0) {
            y_coord = 0;
        }
        return new Coord(x_coord, y_coord);
    }

    @Override
    protected double generateWaitTime() {
        int lower = cigaretteLength[0];
        int upper = cigaretteLength[1];

        specificWaitTime = (upper - lower) *
                rng.nextDouble() + lower;
        return specificWaitTime;
    }

    @Override
    public MapBasedMovement replicate() {
        return new CigaretteActivityMovement(this);
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
        startedCigaretteTime = -1;
        ready = false;
        mode = WALKING_TO_CIGARETTE_MODE;
    }

    /**
     * @return The location of the office
     */
    public Coord getTableLocation() {
        return cigaretteSpotLocation.clone();
    }
}
