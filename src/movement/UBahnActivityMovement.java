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
 * A Class to model movement at u-bahn. If the node happens to be at some other
 * location than its home, it first walks the shortest path home location and
 * then stays there until morning. A node has only one home
 *
 * @author Frans Ekman
 */
public class UBahnActivityMovement extends MapBasedMovement
	implements SwitchableMovement {

	private static final int WALKING_UBAHN_MODE = 0;
	private static final int AT_UBAHN_MODE = 1;
	private static final int READY_MODE = 2;

	private static final int F_15_MINUTES = 900;

	public static final String UBAHN_LOCATION = "ubahnLocation";

	private int mode;
	private DijkstraPathFinder pathFinder;

	private int distance;

	private Coord lastWaypoint;
	private Coord ubahnLocation;

	/**
	 * Creates a new instance of UBahnActivityMovement
	 * @param settings
	 */
	public UBahnActivityMovement(Settings settings) {
		super(settings);
		distance = 0;
		pathFinder = new DijkstraPathFinder(null);
		mode = AT_UBAHN_MODE;

		if (settings.contains(UBAHN_LOCATION)) {
			try {
				double[] xy = settings.getCsvDoubles(UBAHN_LOCATION);
				ubahnLocation = new Coord(xy[0], xy[1]).clone();
				SimMap map = getMap();
				Coord offset = map.getOffset();
				// mirror points if map data is mirrored
				if (map.isMirrored()) {
					ubahnLocation.setLocation(ubahnLocation.getX(), -ubahnLocation.getY());
				}
				ubahnLocation.translate(offset.getX(), offset.getY());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Creates a new instance of UBahnActivityMovement from a prototype
	 * @param proto
	 */
	public UBahnActivityMovement(UBahnActivityMovement proto) {
		super(proto);
		this.distance = proto.distance;
		this.pathFinder = proto.pathFinder;
		this.mode = proto.mode;

		this.ubahnLocation = proto.ubahnLocation;
	}

	@Override
	public Coord getInitialLocation() {
		double x = rng.nextDouble() * getMaxX();
		double y = rng.nextDouble() * getMaxY();
		Coord c = new Coord(x,y);

		this.lastWaypoint = c;
		return c.clone();
	}

	@Override
	public Path getPath() {
		if (mode == WALKING_UBAHN_MODE) {
			// Try to find home
			SimMap map = super.getMap();
			if (map == null) {
				return null;
			}
			MapNode thisNode = map.getNodeByCoord(lastWaypoint);
			MapNode destinationNode = map.getNodeByCoord(ubahnLocation);
			List<MapNode> nodes = pathFinder.getShortestPath(thisNode,
					destinationNode);
			Path path = new Path(generateSpeed());
			for (MapNode node : nodes) {
				path.addWaypoint(node.getLocation());
			}
			lastWaypoint = ubahnLocation.clone();
			mode = AT_UBAHN_MODE;

			double newX = lastWaypoint.getX() + (rng.nextDouble() - 0.5) *
				distance;
			if (newX > getMaxX()) {
				newX = getMaxX();
			} else if (newX < 0) {
				newX = 0;
			}
			double newY = lastWaypoint.getY() + (rng.nextDouble() - 0.5) *
				distance;
			if (newY > getMaxY()) {
				newY = getMaxY();
			} else if (newY < 0) {
				newY = 0;
			}
			Coord c = new Coord(newX, newY);
			path.addWaypoint(c);
			return path;
		} else {
			Path path =  new Path(1);
			path.addWaypoint(lastWaypoint.clone());
			mode = READY_MODE;
			return path;
		}

	}

	@Override
	protected double generateWaitTime() {
		if (mode == AT_UBAHN_MODE) {
			return F_15_MINUTES * rng.nextInt(20);
		} else {
			return 0;
		}
	}

	@Override
	public MapBasedMovement replicate() {
		return new UBahnActivityMovement(this);
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
		return mode == READY_MODE;
	}

	/**
	 * @see SwitchableMovement
	 */
	public void setLocation(Coord lastWaypoint) {
		this.lastWaypoint = lastWaypoint.clone();
		mode = WALKING_UBAHN_MODE;
	}

	/**
	 * @return Home location of the node
	 */
	public Coord getUBahnLocation() {
		return ubahnLocation.clone();
	}

}
