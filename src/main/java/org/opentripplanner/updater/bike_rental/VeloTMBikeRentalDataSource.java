package org.opentripplanner.updater.bike_rental;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.util.HttpUtils;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class VeloTMBikeRentalDataSource implements BikeRentalDataSource, JsonConfigurable {

    private static final Logger log = LoggerFactory.getLogger(VeloTMBikeRentalDataSource.class);
    private String url;
    private String headerName;
    private String headerValue;

    private String jsonParsePath;

    private List<BikeRentalStation> stations = new ArrayList<BikeRentalStation>();

    /**
     * Construct superclass where rental list is on the top level of JSON code
     *
     */
    public VeloTMBikeRentalDataSource() {
        jsonParsePath = "Data";
    }

    @Override
    public boolean update() {
        try {
            InputStream data;

            URL url2 = new URL(url);

            String proto = url2.getProtocol();
            if (proto.equals("http") || proto.equals("https")) {

                data = HttpUtils.getDataWithPost(url, headerName, headerValue);
            } else {
                // Local file probably, try standard java
                data = url2.openStream();
            }
            if (data == null) {
                log.warn("Failed to get data from url " + url);
                return false;
            }
            parseJSON(data);
            data.close();
        } catch (IllegalArgumentException e) {
            log.warn("Error parsing bike rental feed from " + url, e);
            return false;
        } catch (JsonProcessingException e) {
            log.warn("Error parsing bike rental feed from " + url + "(bad JSON of some sort)", e);
            return false;
        } catch (IOException e) {
            log.warn("Error reading bike rental feed from " + url, e);
            return false;
        }
        return true;
    }

    private void parseJSON(InputStream dataStream) throws IllegalArgumentException, IOException {

        ArrayList<BikeRentalStation> out = new ArrayList<>();

        String rentalString = convertStreamToString(dataStream);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(rentalString);

        if (!jsonParsePath.equals("")) {
            String delimiter = "/";
            String[] parseElement = jsonParsePath.split(delimiter);
            for(int i =0; i < parseElement.length ; i++) {
                rootNode = rootNode.path(parseElement[i]);
            }

            if (rootNode.isMissingNode()) {
                throw new IllegalArgumentException("Could not find jSON elements " + jsonParsePath);
            }
        }

        for (int i = 0; i < rootNode.size(); i++) {
            // TODO can we use foreach? for (JsonNode node : rootNode) ...
            JsonNode node = rootNode.get(i);
            if (node == null) {
                continue;
            }
            BikeRentalStation brstation = makeStation(node);
            if (brstation != null)
                out.add(brstation);
        }
        synchronized(this) {
            stations = out;
        }
    }

    private String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner scanner = null;
        String result="";
        try {

            scanner = new java.util.Scanner(is).useDelimiter("\\A");
            result = scanner.hasNext() ? scanner.next() : "";
            scanner.close();
        }
        finally
        {
            if(scanner!=null)
                scanner.close();
        }
        return result;

    }

    @Override
    public synchronized List<BikeRentalStation> getStations() {
        return stations;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public BikeRentalStation makeStation(JsonNode rentalStationNode) {
        if (!rentalStationNode.path("Status").asText().equals("Functionala")) {
            return null;
        }

        if (!rentalStationNode.path("IsValid").asText().equals("true")) {
            return null;
        }

        BikeRentalStation brstation = new BikeRentalStation();

        brstation.networks = new HashSet<String>();
        brstation.networks.add("Velo TM");

        brstation.id = rentalStationNode.path("Id").toString();
        brstation.x = rentalStationNode.path("Longitude").asDouble();
        brstation.y = rentalStationNode.path("Latitude").asDouble();
        brstation.name =  new NonLocalizedString(rentalStationNode.path("StationName").asText());
        brstation.bikesAvailable = rentalStationNode.path("OcuppiedSpots").asInt();
        brstation.spacesAvailable = rentalStationNode.path("EmptySpots").asInt();
        brstation.realTimeData = true;

        return brstation;
    }

    @Override
    public String toString() {
        return getClass().getName() + "(" + url + ")";
    }

    /**
     * Note that the JSON being passed in here is for configuration of the OTP component, it's completely separate
     * from the JSON coming in from the update source.
     */
    @Override
    public void configure (Graph graph, JsonNode jsonNode) {
        String url = jsonNode.path("url").asText(); // path() returns MissingNode not null.
        if (url == null) {
            throw new IllegalArgumentException("Missing mandatory 'url' configuration.");
        }
        this.url = url;
    }
}
