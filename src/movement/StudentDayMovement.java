/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;

/**
 *
 * This movement model makes use of several other movement models to simulate
 * movement with daily routines. People wake up in the morning, go to work,
 * go shopping or similar activities in the evening and finally go home to
 * sleep.
 *
 * @author Frans Ekman
 */
public class StudentDayMovement extends ExtendedMovementModel {
	public static final String PROBABILITY_TO_BREAK_SETTING = "breakProb";
	public static final String PROBABILITY_TO_COURSE_SETTING = "courseProb";
	public static final String PROBABILITY_TO_UBAHN_SETTING = "uBahnProb";

	private UBahnActivityMovement uBahnActivityMovement;
	private CourseActivityMovement courseActivityMovement;
	private BreakActivityMovement breakActivityMovement;

	private CarMovement carMM;
	private TransportMovement movementUsedForTransfers;

	private static final int UBAHN_MODE = 0;
	private static final int COURSE_MODE = 1;
	private static final int BREAK_MODE = 2;

	private static final int TO_UBAHN_MODE = 3;
	private static final int TO_COURSE_MODE = 4;
	private static final int TO_BREAK_MODE = 5;

	private int mode;

	private double breakProb;
	private double courseProb;
	private double uBahnProb;

	/**
	 * Creates a new instance of StudentDayMovement
	 * @param settings
	 */
	public StudentDayMovement(Settings settings) {
		super(settings);
		courseActivityMovement = new CourseActivityMovement(settings);
		uBahnActivityMovement = new UBahnActivityMovement(settings);
		breakActivityMovement = new BreakActivityMovement(settings);

		breakProb = settings.getDouble(PROBABILITY_TO_BREAK_SETTING);
		courseProb = settings.getDouble(PROBABILITY_TO_COURSE_SETTING);
		uBahnProb = settings.getDouble(PROBABILITY_TO_UBAHN_SETTING);

		carMM = new CarMovement(settings);
		movementUsedForTransfers = carMM;

		setCurrentMovementModel(uBahnActivityMovement);
		mode = UBAHN_MODE;
	}

	/**
	 * Creates a new instance of StudentDayMovement from a prototype
	 * @param proto
	 */
	public StudentDayMovement(StudentDayMovement proto) {
		super(proto);
		courseActivityMovement = new CourseActivityMovement(proto.courseActivityMovement);
		uBahnActivityMovement = new UBahnActivityMovement(proto.uBahnActivityMovement);
		breakActivityMovement = new BreakActivityMovement(proto.breakActivityMovement);

		breakProb = proto.breakProb;
		courseProb = proto.courseProb;
		uBahnProb = proto.uBahnProb;

		carMM = new CarMovement(proto.carMM);
		movementUsedForTransfers = carMM;

		setCurrentMovementModel(uBahnActivityMovement);
		mode = proto.mode;
	}

	@Override
	public boolean newOrders() {
		switch (mode) {
		case UBAHN_MODE:
			if (uBahnActivityMovement.isReady()) {
				setCurrentMovementModel(movementUsedForTransfers);
				if (courseProb > rng.nextDouble()) {
					movementUsedForTransfers.setNextRoute(
							uBahnActivityMovement.getUBahnLocation(),
							courseActivityMovement.getCourseLocation());
					mode = TO_COURSE_MODE;
				} else if (breakProb > rng.nextDouble()) {
					movementUsedForTransfers.setNextRoute(
							uBahnActivityMovement.getUBahnLocation(),
							breakActivityMovement.getBreakLocationAndGetReady());
					mode = TO_BREAK_MODE;
				}
			}
			break;
		case COURSE_MODE:
			if (courseActivityMovement.isReady()) {
				setCurrentMovementModel(movementUsedForTransfers);
				if (breakProb > rng.nextDouble()) {
					movementUsedForTransfers.setNextRoute(
							courseActivityMovement.getCourseLocation(),
							breakActivityMovement.
									getBreakLocationAndGetReady());
					mode = TO_BREAK_MODE;
				} else if (uBahnProb > rng.nextDouble()) {
					movementUsedForTransfers.setNextRoute(
							courseActivityMovement.getCourseLocation(),
							uBahnActivityMovement.getUBahnLocation());
					mode = TO_UBAHN_MODE;
				}
			}
			break;
		case BREAK_MODE:
			if (breakActivityMovement.isReady()) {
				setCurrentMovementModel(movementUsedForTransfers);
				if (courseProb > rng.nextDouble()) {
					movementUsedForTransfers.setNextRoute(
							breakActivityMovement.getBreakLocationAndGetReady(),
							courseActivityMovement.getCourseLocation());
					mode = TO_COURSE_MODE;
				} else if (uBahnProb > rng.nextDouble()) {
					movementUsedForTransfers.setNextRoute(
							breakActivityMovement.getBreakLocationAndGetReady(),
							uBahnActivityMovement.getUBahnLocation());
					mode = TO_UBAHN_MODE;
				}
			}
			break;
		case TO_COURSE_MODE:
			if (movementUsedForTransfers.isReady()) {
				setCurrentMovementModel(courseActivityMovement);
				mode = COURSE_MODE;
			}
			break;
		case TO_BREAK_MODE:
			if (movementUsedForTransfers.isReady()) {
				setCurrentMovementModel(breakActivityMovement);
				mode = BREAK_MODE;
			}
			break;
		case TO_UBAHN_MODE:
			if (movementUsedForTransfers.isReady()) {
				setCurrentMovementModel(uBahnActivityMovement);
				mode = UBAHN_MODE;
			}
			break;
		default:
			break;
		}
		return true;
	}

	@Override
	public Coord getInitialLocation() {
		Coord ubahnLoc = uBahnActivityMovement.getUBahnLocation().clone();
		uBahnActivityMovement.setLocation(ubahnLoc);
		return ubahnLoc;
	}

	@Override
	public MovementModel replicate() {
		return new StudentDayMovement(this);
	}
}
