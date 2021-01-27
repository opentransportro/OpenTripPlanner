package org.opentripplanner.ext.stopclustering.models;

public enum StopClusterMode {
    /** Group stops by their declared parent station in the GTFS data. */
    parentStation,
    /** Cluster stops by proximity and name. */
    proximity
}
