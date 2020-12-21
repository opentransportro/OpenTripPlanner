package org.opentripplanner.updater.bike_rental.datasources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.updater.bike_rental.BikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.datasources.params.BikeRentalDataSourceParameters;
import org.opentripplanner.util.HttpUtils;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class VeloBikeRentalDataSource implements BikeRentalDataSource {

  private static final Logger log = LoggerFactory.getLogger(VeloBikeRentalDataSource.class);
  private String url;
  private final String provider;

  private List<BikeRentalStation> stations = new ArrayList<>();

  public VeloBikeRentalDataSource(BikeRentalDataSourceParameters source) {
    url = source.getUrl();
    provider = source.getNetwork("");
  }

  @Override
  public boolean update() {
    try {
      InputStream data = null;

      URL url2 = new URL(url);

      String proto = url2.getProtocol();
      if (proto.equals("http") || proto.equals("https")) {
        data = HttpUtils.getData(URI.create(url), HttpUtils.Method.POST,null, null);
      }

      if (data == null) {
        log.warn("Failed to get data from url " + url);
        return false;
      }
      parseJSON(data);
      data.close();
    }
    catch (IllegalArgumentException e) {
      log.warn("Error parsing bike rental feed from " + url, e);
      return false;
    }
    catch (JsonProcessingException e) {
      log.warn("Error parsing bike rental feed from " + url + "(bad JSON of some sort)", e);
      return false;
    }
    catch (IOException e) {
      log.warn("Error reading bike rental feed from " + url, e);
      return false;
    }
    return true;
  }

  private void parseJSON(InputStream dataStream) throws IllegalArgumentException, IOException {

    List<BikeRentalStation> out = new ArrayList<>();

    String rentalString = convertStreamToString(dataStream);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = mapper.readTree(rentalString);

    rootNode = rootNode.path("Data");

    if (rootNode.isMissingNode()) {
      throw new IllegalArgumentException("Could not find jSON elements Data");
    }

    for (int i = 0; i < rootNode.size(); i++) {
      JsonNode node = rootNode.get(i);
      if (node == null) {
        continue;
      }
      BikeRentalStation brstation = makeStation(node);
      if (brstation != null)
        out.add(brstation);
    }
    synchronized (this) {
      stations = out;
    }
  }

  private String convertStreamToString(java.io.InputStream is) {
    java.util.Scanner scanner = null;
    String result = "";
    try {

      scanner = new java.util.Scanner(is).useDelimiter("\\A");
      result = scanner.hasNext() ? scanner.next() : "";
      scanner.close();
    }
    finally {
      if (scanner != null)
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

    BikeRentalStation station = new BikeRentalStation();

    station.networks = new HashSet<>();
    station.networks.add(this.provider);

    station.id = rentalStationNode.path("Id").toString();
    station.x = rentalStationNode.path("Longitude").asDouble();
    station.y = rentalStationNode.path("Latitude").asDouble();
    station.name = new NonLocalizedString(rentalStationNode.path("StationName").asText());
    station.bikesAvailable = rentalStationNode.path("OcuppiedSpots").asInt();
    station.spacesAvailable = rentalStationNode.path("EmptySpots").asInt();
    station.realTimeData = true;

    if(station.x == 0.0 || station.y == 0)
      return null;

    return station;
  }

  @Override
  public String toString() {
    return getClass().getName() + "(" + url + ")";
  }
}
