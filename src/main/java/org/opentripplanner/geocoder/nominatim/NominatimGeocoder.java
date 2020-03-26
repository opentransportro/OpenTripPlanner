package org.opentripplanner.geocoder.nominatim;

import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.geocoder.Geocoder;
import org.opentripplanner.geocoder.GeocoderResult;
import org.opentripplanner.geocoder.GeocoderResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class NominatimGeocoder implements Geocoder {
    private static final Logger LOG = LoggerFactory.getLogger(NominatimGeocoder.class);

    private String nominatimUrl = "https://nominatim.openstreetmap.org/";
    private Integer resultLimit;
    private String viewBox;
    private String emailAddress;
    
    private NominatimJsonDeserializer nominatimJsonDeserializer; 
    
    public NominatimGeocoder() {
        nominatimJsonDeserializer = new NominatimJsonDeserializer();
    }

    public NominatimGeocoder(String nominatimUrl){
        nominatimJsonDeserializer = new NominatimJsonDeserializer();
        this.nominatimUrl = nominatimUrl;
    }
    
    public String getNominatimUrl() {
        return nominatimUrl;
    }
    
    public void setNominatimUrl(String nominatimUrl) {
        this.nominatimUrl = nominatimUrl; 
    }
    
    public Integer getResultLimit() {
        return resultLimit;
    }

    public void setResultLimit(Integer resultLimit) {
        this.resultLimit = resultLimit;
    }

    public String getViewBox() {
        return viewBox;
    }

    public void setViewBox(String viewBox) {
        this.viewBox = viewBox;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override 
    public GeocoderResults geocode(String address, Envelope bbox) {
        String content = null;
        try {
            // make json request
            URL nominatimGeocoderUrl = getNominatimGeocoderUrl(address, bbox);
            URLConnection conn = nominatimGeocoderUrl.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder(128);
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            reader.close();
            content = sb.toString();

        } catch (IOException e) {
            LOG.error("Error parsing nominatim geocoder response", e);
            return noGeocoderResult("Error parsing geocoder response");
        }
           
        List<NominatimGeocoderResult> nominatimResults = nominatimJsonDeserializer.parseResults(content);
        List<GeocoderResult> geocoderResults = new ArrayList<>();
        for (NominatimGeocoderResult nominatimGeocoderResult : nominatimResults) {
            double lat = nominatimGeocoderResult.getLatDouble();
            double lng = nominatimGeocoderResult.getLngDouble();
            String displayName = nominatimGeocoderResult.getDisplay_name();
            GeocoderResult geocoderResult = new GeocoderResult(lat, lng, displayName);
            geocoderResults.add(geocoderResult);
        }
        return new GeocoderResults(geocoderResults);
    }
    
    private URL getNominatimGeocoderUrl(String address, Envelope bbox) throws IOException {
        UriBuilder uriBuilder = UriBuilder.fromUri(nominatimUrl);
        uriBuilder.queryParam("q", address);
        uriBuilder.queryParam("format", "json");
        if (bbox != null) {
            uriBuilder.queryParam("viewbox", bbox.getMinX() + "," + bbox.getMinY() + "," + bbox.getMaxX() + "," + bbox.getMaxY());
            uriBuilder.queryParam("bounded", 1);
        } else if (viewBox != null) {
            uriBuilder.queryParam("viewbox", viewBox);
            uriBuilder.queryParam("bounded", 1);
        }
        if (resultLimit != null) {
            uriBuilder.queryParam("limit", resultLimit);
        }
        if (emailAddress != null) {
            uriBuilder.queryParam("email", emailAddress);
        }
        
        URI uri = uriBuilder.build();
        return new URL(uri.toString());
    }  
    
    private GeocoderResults noGeocoderResult(String error) {
        return new GeocoderResults(error);
    }

}
