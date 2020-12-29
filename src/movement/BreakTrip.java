/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;

/**
 * A class to encapsulate information about a shopping trip
 * 1. Where the trip begins
 * 2. Where it ends
 * 3. The path
 * 4. All nodes in the group
 *
 * @author Frans Ekman
 */
public class BreakTrip {
	private BreakActivityMovement[] breakActivityNodes;
	private int breakActivityNodesInBuffer;
	private Path path;
	private Coord location;
	private Coord destination;
	private double waitTimeAtEnd;

	/**
	 * Create a new instance of a EveningTrip
	 * @param nrOfbreakActivityNodes The number of shoppers in the group
	 * @param location Where the trip starts
	 */
	public BreakTrip(int nrOfbreakActivityNodes, Coord location) {
		breakActivityNodes =
			new BreakActivityMovement[nrOfbreakActivityNodes];
		this.location = location;
		breakActivityNodesInBuffer = 0;
	}

	/**
	 * Add an evening activity node to the group
	 * @param breakActivityNode
	 * @return true if there was room in the group
	 */
	public boolean addNode(BreakActivityMovement breakActivityNode) {
		if (isFull()) {
			return false;
		} else {
			breakActivityNodes[breakActivityNodesInBuffer] =
					breakActivityNode;
			breakActivityNodesInBuffer++;
			return true;
		}
	}

	/**
	 * Sets the shopping path for the group
	 * @param path
	 */
	public void setPath(Path path) {
		this.path = new Path(path);
	}

	/**
	 * @return The shopping trip path
	 */
	public Path getPath() {
		if (path != null) {
			return new Path(this.path);
		} else {
			return null;
		}
	}

	/**
	 * @return The location where the shopping trip starts
	 */
	public Coord getLocation() {
		return location;
	}

	/**
	 * @return true if the group is full
	 */
	public boolean isFull() {
		return breakActivityNodesInBuffer >= breakActivityNodes.length;
	}

	/**
	 * @return The destination square of the shopping trip
	 */
	public Coord getDestination() {
		return destination;
	}

	/**
	 * Sets the destination square of the trip. MUST be the same as the last
	 * node in the path
	 * @param destination
	 */
	public void setDestination(Coord destination) {
		this.destination = destination;
	}

	public double getWaitTimeAtEnd() {
		return waitTimeAtEnd;
	}

	public void setWaitTimeAtEnd(double waitTimeAtEnd) {
		this.waitTimeAtEnd = waitTimeAtEnd;
	}
}

