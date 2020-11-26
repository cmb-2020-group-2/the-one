/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.DTNSim;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * This class controls the group mobility of the people meeting their friends in
 * the evening
 *
 * @author Frans Ekman
 */
public class BreakActivityControlSystem {

	private HashMap<Integer, BreakActivityMovement> breakActivityNodes;
	private List<Coord> tableSpots;
	private BreakTrip[] nextTrips;

	private Random rng;

	private static HashMap<Integer, BreakActivityControlSystem>
		controlSystems;

	static {
		DTNSim.registerForReset(BreakActivityControlSystem.class.
				getCanonicalName());
		reset();
	}

	/**
	 * Creates a new instance of EveningActivityControlSystem without any nodes
	 * or meeting spots, with the ID given as parameter
	 * @param id
	 */
	private BreakActivityControlSystem(int id) {
		breakActivityNodes = new HashMap<Integer, BreakActivityMovement>();
	}

	public static void reset() {
		controlSystems = new HashMap<Integer, BreakActivityControlSystem>();
	}

	/**
	 * Register a evening activity node with the system
	 * @param breakMovement activity movement
	 */
	public void addBreakActivityNode(BreakActivityMovement breakMovement) {
		breakActivityNodes.put(new Integer(breakMovement.getID()),
				breakMovement);
	}

	/**
	 * Sets the meeting locations the nodes can choose among
	 * @param tableSpots
	 */
	public void setTableSpots(List<Coord> tableSpots) {
		this.tableSpots = tableSpots;
		this.nextTrips = new BreakTrip[tableSpots.size()];
	}

	/**
	 * This method gets the instruction for a node, i.e. When/where and with
	 * whom to go.
	 * @param breakActivityNodeID unique ID of the node
	 * @return Instructions object
	 */
	public BreakTrip getBreakInstructions(int breakActivityNodeID) {
		BreakActivityMovement breakMovement = breakActivityNodes.get(
				new Integer(breakActivityNodeID));
		if (breakMovement != null) {
			int index = breakActivityNodeID % tableSpots.size();
			if (nextTrips[index] == null) {
				int nrOfTableMovementNodes = (int)(breakMovement.
						getMinGroupSize() +
						(double)(breakMovement.getMaxGroupSize() -
								breakMovement.getMinGroupSize()) *
								rng.nextDouble());
				Coord loc = tableSpots.get(index).clone();
				nextTrips[index] = new BreakTrip(nrOfTableMovementNodes,
						loc);
			}
			nextTrips[index].addNode(breakMovement);
			if (nextTrips[index].isFull()) {
				BreakTrip temp = nextTrips[index];
				nextTrips[index] = null;
				return temp;
			} else {
				return nextTrips[index];
			}
		}
		return null;
	}

	/**
	 * Get the meeting spot for the node
	 * @param id
	 * @return Coordinates of the spot
	 */
	public Coord getTableSpotForID(int id) {
		int index = id % tableSpots.size();
		Coord loc = tableSpots.get(index).clone();
		return loc;
	}


	/**
	 * Sets the random number generator to be used
	 * @param rand
	 */
	public void setRandomNumberGenerator(Random rand) {
		this.rng = rand;
	}

	/**
	 * Returns a reference to a EveningActivityControlSystem with ID provided as
	 * parameter. If a system does not already exist with the requested ID, a
	 * new one is created.
	 * @param id unique ID of the EveningActivityControlSystem
	 * @return The EveningActivityControlSystem with the provided ID
	 */
	public static BreakActivityControlSystem getBreakActivityControlSystem(
			int id) {
		if (controlSystems.containsKey(new Integer(id))) {
			return controlSystems.get(new Integer(id));
		} else {
			BreakActivityControlSystem scs =
				new BreakActivityControlSystem(id);
			controlSystems.put(new Integer(id), scs);
			return scs;
		}
	}

}
