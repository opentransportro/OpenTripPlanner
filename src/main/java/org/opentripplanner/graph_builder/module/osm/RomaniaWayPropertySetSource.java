package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 * OSM way properties for Norwegian roads.
 * The main difference compared to the default property set is that most of the highway=trunk roads also allows walking and biking,
 * where as some does not.
 * http://wiki.openstreetmap.org/wiki/Tag:highway%3Dtrunk
 * http://wiki.openstreetmap.org/wiki/Highway:International_equivalence
 *
 *
 * @author seime
 * @see WayPropertySetSource
 * @see DefaultWayPropertySetSource
 */
public class RomaniaWayPropertySetSource implements WayPropertySetSource {
    @Override
    public void populateProperties(WayPropertySet props) {
        // Replace existing matching properties as the logic is that the first statement registered takes precedence over later statements
        props.setProperties("highway=trunk_link", StreetTraversalPermission.ALL, 2.06,
                2.06);
        props.setProperties("highway=trunk", StreetTraversalPermission.ALL, 7.47, 7.47);

        // Don't recommend walking in trunk road tunnels (although actually legal unless explicitly forbidden)
        props.setProperties("highway=trunk;tunnel=yes", StreetTraversalPermission.CAR, 7.47, 7.47);

        // Do not walk on "Motortrafikkvei" ("motorvei klasse b")
        props.setProperties("motorroad=yes", StreetTraversalPermission.CAR, 7.47, 7.47);

        /*
         * Automobile speeds in Norway. General speed limit is 80kph unless signs says otherwise
         *
         */
        props.setCarSpeed("highway=motorway", 36.11f); // 130kph
        props.setCarSpeed("highway=motorway_link", 15); // = 54kph
        props.setCarSpeed("highway=trunk", 27.77f); // 100kph
        props.setCarSpeed("highway=trunk_link", 13.88f); // = 50kph
        props.setCarSpeed("highway=primary", 25); // 80kph
        props.setCarSpeed("highway=primary_link", 13.88f); // = 50kph

        // Read the rest from the default set
        new DefaultWayPropertySetSource().populateProperties(props);
    }
}
