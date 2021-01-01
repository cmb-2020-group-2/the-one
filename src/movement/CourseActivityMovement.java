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
import util.ParetoRNG;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * This class models movement at an office. If the node happens to be at some
 * other location than the office, it first walks the shortest path to the
 * office and then stays there until the end of the work day. A node has only
 * works at one office.
 *
 * @author Frans Ekman
 *
 */
public class CourseActivityMovement extends MapBasedMovement implements
	SwitchableMovement {

	private static final int WALKING_TO_COURSE_MODE = 0;
	private static final int AT_COURSE_MODE = 1;

	public static final String COURSE_LENGTH_SETTING = "courseLength";
	public static final String NR_OF_COURSES_SETTING = "nrOfCourses";

	public static final String COURSE_SIZE_SETTING = "courseSize";
	public static final String COURSE_WAIT_TIME_PARETO_COEFF_SETTING =
		"courseWaitTimeParetoCoeff";
	public static final String COURSE_MIN_WAIT_TIME_SETTING =
		"courseMinWaitTime";
	public static final String COURSE_MAX_WAIT_TIME_SETTING =
		"courseMaxWaitTime";
	public static final String COURSE_LOCATIONS_FILE_SETTING =
		"courseLocationsFile";

	private static int nrOfCourses = 30;

	private int mode;
	private int[] courseLength;
	private double specificWaitTime;
	private int startedWorkingTime;
	private boolean ready;
	private DijkstraPathFinder pathFinder;

	private ParetoRNG paretoRNG;

	private int distance;
	private double courseWaitTimeParetoCoeff;
	private double courseMinWaitTime;
	private double courseMaxWaitTime;

	private List<Coord> allCourses;

	private Coord lastWaypoint;
	private Coord courseLocation;
	private Coord deskLocation;

	private boolean sittingAtDesk;

	/**
	 * CourseActivityMovement constructor
	 * @param settings
	 */
	public CourseActivityMovement(Settings settings) {
		super(settings);

		courseLength = settings.getCsvInts(COURSE_LENGTH_SETTING);
		nrOfCourses = settings.getInt(NR_OF_COURSES_SETTING);

		distance = settings.getInt(COURSE_SIZE_SETTING);
		courseWaitTimeParetoCoeff = settings.getDouble(
				COURSE_WAIT_TIME_PARETO_COEFF_SETTING);
		courseMinWaitTime = settings.getDouble(COURSE_MIN_WAIT_TIME_SETTING);
		courseMaxWaitTime = settings.getDouble(COURSE_MAX_WAIT_TIME_SETTING);

		startedWorkingTime = -1;
		pathFinder = new DijkstraPathFinder(null);
		mode = WALKING_TO_COURSE_MODE;

		String courseLocationsFile = null;
		try {
			courseLocationsFile = settings.getSetting(
					COURSE_LOCATIONS_FILE_SETTING);
		} catch (Throwable t) {
			// Do nothing;
		}

		if (courseLocationsFile == null) {
			MapNode[] mapNodes = (MapNode[])getMap().getNodes().
				toArray(new MapNode[0]);
			int officeIndex = rng.nextInt(mapNodes.length - 1) /
				(mapNodes.length/ nrOfCourses);
			courseLocation = mapNodes[officeIndex].getLocation().clone();
		} else {
			try {
				allCourses = new LinkedList<Coord>();
				List<Coord> locationsRead = (new WKTReader()).
					readPoints(new File(courseLocationsFile));
				for (Coord coord : locationsRead) {
					SimMap map = getMap();
					Coord offset = map.getOffset();
					// mirror points if map data is mirrored
					if (map.isMirrored()) {
						coord.setLocation(coord.getX(), -coord.getY());
					}
					coord.translate(offset.getX(), offset.getY());
					allCourses.add(coord);
				}
				courseLocation = allCourses.get(
						rng.nextInt(allCourses.size())).clone();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		deskLocation = getRandomCoorinateInsideClass();
		paretoRNG = new ParetoRNG(rng, courseWaitTimeParetoCoeff,
				courseMinWaitTime, courseMaxWaitTime);
	}

	/**
	 * Copyconstructor
	 * @param proto
	 */
	public CourseActivityMovement(CourseActivityMovement proto) {
		super(proto);
		this.courseLength = proto.courseLength;
		startedWorkingTime = -1;
		this.distance = proto.distance;
		this.pathFinder = proto.pathFinder;
		this.mode = proto.mode;

		if (proto.allCourses == null) {
			MapNode[] mapNodes = (MapNode[])getMap().getNodes().
				toArray(new MapNode[0]);
			int officeIndex = rng.nextInt(mapNodes.length - 1) /
				(mapNodes.length/ nrOfCourses);
			courseLocation = mapNodes[officeIndex].getLocation().clone();
		} else {
			this.allCourses = proto.allCourses;
			courseLocation = allCourses.get(
					rng.nextInt(allCourses.size())).clone();
		}

		courseWaitTimeParetoCoeff = proto.courseWaitTimeParetoCoeff;
		courseMinWaitTime = proto.courseMinWaitTime;
		courseMaxWaitTime = proto.courseMaxWaitTime;

		deskLocation = getRandomCoorinateInsideClass();
		this.paretoRNG = proto.paretoRNG;
	}

	public Coord getRandomCoorinateInsideClass() {
		double x_coord = courseLocation.getX() +
			(0.5 - rng.nextDouble()) * distance;
		if (x_coord > getMaxX()) {
			x_coord = getMaxX();
		} else if (x_coord < 0) {
			x_coord = 0;
		}
		double y_coord = courseLocation.getY() +
			(0.5 - rng.nextDouble()) * distance;
		if (y_coord > getMaxY()) {
			y_coord = getMaxY();
		} else if (y_coord < 0) {
			y_coord = 0;
		}
		return new Coord(x_coord, y_coord);
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
		if (mode == WALKING_TO_COURSE_MODE) {
			// Try to find to the office
			SimMap map = super.getMap();
			if (map == null) {
				return null;
			}
			MapNode thisNode = map.getNodeByCoord(lastWaypoint);
			MapNode destinationNode = map.getNodeByCoord(courseLocation);
			List<MapNode> nodes = pathFinder.getShortestPath(thisNode,
					destinationNode);
			Path path = new Path(generateSpeed());
			for (MapNode node : nodes) {
				path.addWaypoint(node.getLocation());
			}

			Coord c;
			if (sittingAtDesk) {
				c = getRandomCoorinateInsideClass();
				sittingAtDesk = false;
			} else {
				c = deskLocation.clone();
				sittingAtDesk = true;
			}
			path.addWaypoint(c);

			lastWaypoint = courseLocation.clone();
			mode = AT_COURSE_MODE;
			return path;
		}

		if (startedWorkingTime == -1) {
			startedWorkingTime = SimClock.getIntTime();
		}
		if (SimClock.getIntTime() >= startedWorkingTime + specificWaitTime) {
			Path path =  new Path(1);
			path.addWaypoint(lastWaypoint.clone());
			ready = true;
 			return path;
		}
		Coord c;
		if (sittingAtDesk) {
			c = getRandomCoorinateInsideClass();
			sittingAtDesk = false;
		} else {
			c = deskLocation.clone();
			sittingAtDesk = true;
		}

		Path path =  new Path(1);
		path.addWaypoint(c);
		return path;
	}

	@Override
	protected double generateWaitTime() {
		int lower = courseLength[0];
		int upper = courseLength[1];

		specificWaitTime = (upper - lower) *
				rng.nextDouble() + lower;
		return specificWaitTime;
	}

	@Override
	public MapBasedMovement replicate() {
		return new CourseActivityMovement(this);
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
		startedWorkingTime = -1;
		ready = false;
		mode = WALKING_TO_COURSE_MODE;
	}

	/**
	 * @return The location of the office
	 */
	public Coord getCourseLocation() {
		return courseLocation.clone();
	}

}
