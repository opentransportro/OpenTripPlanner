package org.opentripplanner.transit.raptor.rangeraptor.transit;


import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;

/**
 * The responsibility for the cost calculator is to calculate the default  multi-criteria cost.
 * <P/>
 * This class is immutable and thread safe.
 */
public class DefaultCostCalculator<T extends RaptorTripSchedule> implements CostCalculator<T> {
    private final int boardCost;
    private final int walkFactor;
    private final int waitFactor;
    private final int transitFactor;
    private final int[] stopVisitCost;

    /**
     * We only apply the wait factor between transits, not between access and transit;
     * Hence we start with 0 (zero) and after the first round we set this to the
     * provided {@link #waitFactor}. We assume we can time-shift the access to get rid
     * of the wait time.
     */
    private int waitFactorApplied = 0;


    public DefaultCostCalculator(
            int[] stopVisitCost,
            int boardCost,
            double walkReluctanceFactor,
            double waitReluctanceFactor,
            WorkerLifeCycle lifeCycle
    ) {
        this.stopVisitCost = stopVisitCost;
        this.boardCost = RaptorCostConverter.toRaptorCost(boardCost);
        this.walkFactor = RaptorCostConverter.toRaptorCost(walkReluctanceFactor);
        this.waitFactor = RaptorCostConverter.toRaptorCost(waitReluctanceFactor);
        this.transitFactor = RaptorCostConverter.toRaptorCost(1.0);
        lifeCycle.onPrepareForNextRound(this::initWaitFactor);
    }

    @Override
    public int onTripRidingCost(
        ArrivalView<T> previousArrival,
        int waitTime,
        int boardTime,
        T trip
    ) {
        // The relative-transit-time is time spent on transit. We do not know the alight-stop, so
        // it is impossible to calculate the "correct" time. But the only thing that maters is that
        // the relative difference between to boardings are correct, assuming riding the same trip.
        // So, we can use the negative board time as relative-transit-time.
        final int relativeTransitTime =  - boardTime;

        int cost = previousArrival.cost()
            + waitFactorApplied * waitTime
            + transitFactor * relativeTransitTime
            + boardCost;

        if(stopVisitCost != null) {
            cost += stopVisitCost[previousArrival.stop()];
        }
        return cost;
    }

    @Override
    public int transitArrivalCost(
        ArrivalView<T> previousArrival,
        int waitTime,
        int transitTime,
        int toStop,
        T trip
    ) {
        int cost = waitFactorApplied * waitTime + transitFactor * transitTime + boardCost;
        if(stopVisitCost != null) {
            cost += stopVisitCost[previousArrival.stop()] + stopVisitCost[toStop];
        }
        return cost;
    }

    @Override
    public int walkCost(int walkTimeInSeconds) {
        return walkFactor * walkTimeInSeconds;
    }

    @Override
    public int waitCost(int waitTimeInSeconds) {
        return waitFactor * waitTimeInSeconds;
    }

    @Override
    public int calculateMinCost(int minTravelTime, int minNumTransfers) {
        return  boardCost * (minNumTransfers + 1) + transitFactor * minTravelTime;
    }

    private void initWaitFactor(int round) {
        // For access(round 0) and the first transit round(1) skip adding a cost for waiting,
        // we assume we can time-shift the access path.
        this.waitFactorApplied = round < 2 ? 0 : waitFactor;
    }
}
