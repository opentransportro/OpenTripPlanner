package org.opentripplanner.ext.stopclustering;

import com.google.common.collect.Maps;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.ext.stopclustering.models.StopCluster;
import org.opentripplanner.ext.stopclustering.models.StopClusterMode;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class StopClusterHolder {
    private static final Logger LOG = LoggerFactory.getLogger(StopClusterHolder.class);
    private final GraphIndex graph;
    private final StopClusterMode mode;

    private static final int CLUSTER_RADIUS = 300; // meters
    public final Map<Stop, StopCluster> stopClusterForStop = Maps.newHashMap();
    public final Map<String, StopCluster> stopClusterForId = Maps.newHashMap();

    private HashGridSpatialIndex<StopCluster> stopClusterSpatialIndex = null;

    public StopClusterHolder(GraphIndex graph, StopClusterMode mode) {
        this.graph = graph;
        this.mode = mode;
    }


    /**
     * Stop clustering is slow to perform and only used in profile routing for the moment.
     * Therefore it is not done automatically, and any method requiring stop clusters should call this method
     * to ensure that the necessary indexes are lazy-initialized.
     */
    public synchronized void clusterStopsAsNeeded() {
        if (stopClusterSpatialIndex == null) {
            clusterStops();
            LOG.info("Creating a spatial index for stop clusters.");
            stopClusterSpatialIndex = new HashGridSpatialIndex<>();

            for (StopCluster cluster : stopClusterForId.values()) {
                Envelope envelope = new Envelope(new Coordinate(cluster.getLon(), cluster.getLat()));
                stopClusterSpatialIndex.insert(envelope, cluster);
            }
        }
    }

    /**
     * Stop clusters can be built in one of two ways, either by geographical proximity and name, or
     * according to a parent/child station topology, if it exists.
     */
    private void clusterStops() {
        if (this.mode == StopClusterMode.parentStation) {
            clusterByParentStation();
        } else {
            clusterByProximityAndName();
        }
    }

    /**
     * Cluster stops by proximity and name.
     * This functionality was developed for the Washington, DC area and probably will not work anywhere else in the
     * world. It depends on the exact way stops are named and the way street intersections are named in that geographic
     * region and in the GTFS data sets which represent it. Based on comments, apparently it might work for TriMet
     * as well.
     *
     * We can't use a name similarity comparison, we need exact matches. This is because many street names differ by
     * only one letter or number, e.g. 34th and 35th or Avenue A and Avenue B. Therefore normalizing the names before
     * the comparison is essential. The agency must provide either parent station information or a well thought out stop
     * naming scheme to cluster stops -- no guessing is reasonable without that information.
     */
    private void clusterByProximityAndName() {
        int psIdx = 0; // unique index for next parent stop
        LOG.info("Clustering stops by geographic proximity and name...");

        var stops = this.graph.getAllStops();
        var stopSpatialIndex = this.graph.getStopSpatialIndex();

        // Each stop without a cluster will greedily claim other stops without clusters.
        for (Stop s0 : stops/*stopForId.values()*/) {
            if (stopClusterForStop.containsKey(s0))
                continue; // skip stops that have already been claimed by a cluster

            StopCluster cluster = new StopCluster(String.format("C%03d", psIdx++), s0.getName());

            // No need to explicitly add s0 to the cluster. It will be found in the spatial index query below.
            Envelope env = new Envelope(new Coordinate(s0.getLon(), s0.getLat()));
            env.expandBy(SphericalDistanceLibrary.metersToLonDegrees(CLUSTER_RADIUS, s0.getLat()),
                    SphericalDistanceLibrary.metersToDegrees(CLUSTER_RADIUS));

            for (TransitStopVertex ts1 : stopSpatialIndex.query(env)) {
                Stop s1 = ts1.getStop();
                double geoDistance = SphericalDistanceLibrary.fastDistance(s0.getLat(), s0.getLon(), s1.getLat(), s1.getLon());
                if (geoDistance < CLUSTER_RADIUS || s1.getName().equals(s0.getName())) {

                    JaroWinklerDistance winklerDistance = new JaroWinklerDistance();
                    if(winklerDistance.apply(s1.getName(), s0.getName()) > 0.5) {
                        // Create a bidirectional relationship between the stop and its cluster
                        cluster.getChildStops().add(s1);
                        LOG.info("adding stop {} to cluster {}, cluster size = {}",
                                s1.getName(),
                                cluster.getName(),
                                cluster.getChildStops().size());
                        stopClusterForStop.put(s1, cluster);
                    }
                }
            }
            cluster.computeCenter();
            stopClusterForId.put(cluster.getId(), cluster);
        }
    }

    /**
     * Rather than using the names and geographic locations of stops to cluster them, group them by their declared
     * parent station in the GTFS data. This should be a much more reliable method where these fields have been
     * included in the GTFS data. However:
     *
     * FIXME OBA parentStation field is a string, not an AgencyAndId, so it has no agency/feed scope.
     * That means it would only work reliably if there is only one GTFS feed loaded.
     * The DC regional graph has no parent stations pre-defined, so we use the alternative proximity / name method.
     * Trimet stops have "landmark" or Transit Center parent stations, so we don't use the parent stop field.
     */
    private void clusterByParentStation() {
        LOG.info("Clustering stops by parent station...");
        var stops = this.graph.getAllStops();
        for (Stop stop : stops /*stopForId.values()*/) {
            Station ps = stop.getParentStation();
            if (ps == null) {
                continue;
            }
            StopCluster cluster;
            if (stopClusterForId.containsKey(ps.getName())) {
                cluster = stopClusterForId.get(ps.getName());
            } else {
                cluster = new StopCluster(ps.getName(), stop.getName());

                cluster.setCoordinates(ps.getLat(), ps.getLon());
                stopClusterForId.put(ps.getName(), cluster);
            }
            cluster.getChildStops().add(stop);
            stopClusterForStop.put(stop, cluster);
        }
    }

}
