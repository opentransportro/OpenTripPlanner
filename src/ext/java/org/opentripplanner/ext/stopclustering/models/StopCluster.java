package org.opentripplanner.ext.stopclustering.models;

import com.google.common.collect.Lists;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TransitEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * Groups stops by geographic proximity and name similarity.
 * This will at least half the number of distinct stop places. In profile routing this means a lot less branching
 * and a lot less transfers to consider.
 *
 * It seems to work quite well for both the Washington DC region and Portland. Locations outside the US would require
 * additional stop name normalizer modules.
 */
public class StopCluster {

    private static final Logger LOG = LoggerFactory.getLogger(StopCluster.class);

    private final FeedScopedId id;
    private final String name;

    private double lon;
    private double lat;
    private final List<Stop> children = Lists.newArrayList();

    public StopCluster(FeedScopedId id, String name) {
        super();
        this.id = id;
        this.name = name;
    }

    public void setCoordinates(double lat, double lon){
        this.lat = lat;
        this.lon = lon;
    }

    public void computeCenter() {
        double lonSum = 0, latSum = 0;
        for (Stop stop : children) {
            lonSum += stop.getLon();
            latSum += stop.getLat();
        }
        lon = lonSum / children.size();
        lat = latSum / children.size();
    }

    public FeedScopedId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getLon() {
        return lon;
    }

    public double getLat() {
        return lat;
    }


    public Collection<Stop> getChildStops() {
        return children;
    }
    @Override
    public String toString() {
        return name;
    }

}