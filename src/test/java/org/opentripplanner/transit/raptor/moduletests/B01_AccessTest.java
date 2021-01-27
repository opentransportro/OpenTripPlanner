package org.opentripplanner.transit.raptor.moduletests;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.api.PathUtils;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.STANDARD;
import static org.opentripplanner.transit.raptor.api.request.SearchDirection.REVERSE;

/**
 * FEATURE UNDER TEST
 *
 * Raptor should return the optimal path with various access paths. All Raptor
 * optimizations(McRaptor, Standard and Reverse Standard) should be tested.
 */
public class B01_AccessTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(RaptorConfig.defaultConfigForTest());

  @Before
  public void setup() {
    data.add(
        route("R1", STOP_1, STOP_2, STOP_3, STOP_4, STOP_5, STOP_6)
            .withTimetable(
                schedule("0:10, 0:12, 0:14, 0:16, 0:18, 0:20")
            )
    );

    requestBuilder.searchParams()
        .addAccessPaths(
            walk(STOP_1, D1s),    // Lowest cost
            walk(STOP_2, D2m),    // Best compromise of cost and time
            walk(STOP_3, D3m),    // Latest departure time
            walk(STOP_4, D7m)     // Not optimal
        )
        .addEgressPaths(
            walk(STOP_6, D1s)
        )
        .earliestDepartureTime(T00_00)
        .latestArrivalTime(T00_30)
    ;

    // Enable Raptor debugging by configuring the requestBuilder
    // data.debugToStdErr(requestBuilder);
  }

  @Test
  public void standard() {
    requestBuilder.profile(STANDARD);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: one path with the latest departure time.
    assertEquals(
        "Walk 3m ~ 3 ~ BUS R1 0:14 0:20 ~ 6 ~ Walk 1s [00:11:00 00:20:01 9m1s]",
        PathUtils.pathsToString(response)
    );
  }

  @Test
  public void standardReverse() {
    requestBuilder
        .profile(STANDARD)
        .searchDirection(REVERSE);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: one path with the latest departure time, same as found in the forward search.
    assertEquals(
        "Walk 3m ~ 3 ~ BUS R1 0:14 0:20 ~ 6 ~ Walk 1s [00:11:00 00:20:01 9m1s]",
        PathUtils.pathsToString(response)
    );
  }

  @Test
  public void multiCriteria() {
    requestBuilder.profile(MULTI_CRITERIA)
        .searchParams().timetableEnabled(true);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: All pareto optimal paths
    assertEquals(""
            + "Walk 3m ~ 3 ~ BUS R1 0:14 0:20 ~ 6 ~ Walk 1s [00:11:00 00:20:01 9m1s, cost: 1684]\n"
            + "Walk 2m ~ 2 ~ BUS R1 0:12 0:20 ~ 6 ~ Walk 1s [00:10:00 00:20:01 10m1s, cost: 1564]\n"
            + "Walk 1s ~ 1 ~ BUS R1 0:10 0:20 ~ 6 ~ Walk 1s [00:09:59 00:20:01 10m2s, cost: 1208]",
        PathUtils.pathsToString(response)
    );
  }
}