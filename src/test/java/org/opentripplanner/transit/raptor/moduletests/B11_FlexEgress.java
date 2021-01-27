package org.opentripplanner.transit.raptor.moduletests;


import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.request.SearchDirection;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.transit.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.flex;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.flexAndWalk;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider.defaultSlackProvider;

/**
 * FEATURE UNDER TEST
 * <p>
 * With FLEX access and egress Raptor must support access/egress paths with more then one leg.
 * These access paths have more transfers that regular paths, hence should not dominate
 * access/egress walking, but only get accepted when they are better on time and/or cost.
 */
public class B11_FlexEgress implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
      RaptorConfig.defaultConfigForTest()
  );

  @Before
  public void setup() {
    data.add(
        route("R1", STOP_1, STOP_2, STOP_3, STOP_4, STOP_5)
            .withTimetable(
                schedule("0:10, 0:12, 0:14, 0:16, 0:18")
            )
    );
    requestBuilder.searchParams()
        .addAccessPaths(
            walk(STOP_1, D1m)
        )
        // All egress paths are all pareto-optimal (McRaptor).
        .addEgressPaths(
            flexAndWalk(STOP_2, D7m),     // best on combination of transfers and time
            flex(STOP_3, D3m, 2),         // earliest arrival time
            flexAndWalk(STOP_4, D2m1s, 2),  // lowest cost
            walk(STOP_5, D10m)            // lowest num-of-transfers (0)
        );
    requestBuilder.searchParams()
        .earliestDepartureTime(T00_00)
        .latestArrivalTime(T00_30);

    // We will test board- and alight-slack in a separate test
    requestBuilder.slackProvider(
        defaultSlackProvider(60, 0, 0)
    );

    // Enable Raptor debugging by configuring the requestBuilder
    // data.debugToStdErr(requestBuilder);
  }

  @Test
  public void standard() {
    requestBuilder.profile(RaptorProfile.STANDARD);

    var response = raptorService.route(requestBuilder.build(), data);

    assertEquals(
        "Walk 1m ~ 1 ~ BUS R1 0:10 0:14 ~ 3 ~ Flex 3m 2tx [00:09:00 00:18:00 9m]",
        pathsToString(response)
    );
  }

  @Test
  public void standardReverse() {
    requestBuilder
        .profile(RaptorProfile.STANDARD)
        .searchDirection(SearchDirection.REVERSE);

    var response = raptorService.route(requestBuilder.build(), data);

    assertEquals(
        "Walk 1m ~ 1 ~ BUS R1 0:10 0:14 ~ 3 ~ Flex 3m 2tx [00:09:00 00:18:00 9m]",
        pathsToString(response)
    );
  }

  @Test
  public void multiCriteria() {
    requestBuilder.profile(RaptorProfile.MULTI_CRITERIA);

    var response = raptorService.route(requestBuilder.build(), data);

    assertEquals(""
            + "Walk 1m ~ 1 ~ BUS R1 0:10 0:14 ~ 3 ~ Flex 3m 2tx [00:09:00 00:18:00 9m, cost: 1860]\n"
            + "Walk 1m ~ 1 ~ BUS R1 0:10 0:16 ~ 4 ~ Flex 2m1s 2tx [00:09:00 00:19:01 10m1s, cost: 1744]\n"
            + "Walk 1m ~ 1 ~ BUS R1 0:10 0:12 ~ 2 ~ Flex 7m 1tx [00:09:00 00:20:00 11m, cost: 2700]\n"
            + "Walk 1m ~ 1 ~ BUS R1 0:10 0:18 ~ 5 ~ Walk 10m [00:09:00 00:28:00 19m, cost: 3720]",
        pathsToString(response)
    );
  }
}
