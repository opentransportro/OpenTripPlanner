package org.opentripplanner.updater.bike_rental;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import com.google.common.base.Strings;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.util.NonLocalizedString;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Kaakau (Kuopio, Finland) bike rental data source.
 * url: https://kaupunkipyorat.kuopio.fi/tkhs-export-map.html?format=xml
 */
public class KaakauBikeRentalDataSource extends GenericXmlBikeRentalDataSource {

    private String networkName;

    public KaakauBikeRentalDataSource(String networkName) {
        super("//station");
        this.networkName = Strings.isNullOrEmpty(networkName) ? "kaakau" : networkName;
    }

    public BikeRentalStation makeStation(Map<String, String> attributes) {
        BikeRentalStation station = new BikeRentalStation();
        station.networks = new HashSet<>(Collections.singleton(this.networkName));
        station.id = attributes.get("name");
        station.name = new NonLocalizedString(attributes.get("name"));
        station.state = "Station on";

        try {
            station.y = Double.parseDouble(node.path("coordinates").asText().split(",")[0].trim());
            station.x = Double.parseDouble(node.path("coordinates").asText().split(",")[1].trim());
        } catch (NumberFormatException e) {
            // E.g. coordinates is empty
            return null;
        }
        return station;
    }
}
