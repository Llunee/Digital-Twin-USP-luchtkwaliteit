package nl.hu.luchtkwaliteit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.hu.luchtkwaliteit.application.DatastreamDTO;
import nl.hu.luchtkwaliteit.application.MeasurementDTO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Path("luchtkwaliteit")
public class LuchtkwaliteitResource {

    @GET
    @Path("geojson")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGeoJSONFile() {
        File file = new File("geoJsonFiles/geoJSONData.json");
        if (!file.exists()) {
            return Response.status(Response.Status.NOT_FOUND).entity("GeoJSON file not found").build();
        }

        try (FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            bufferedReader.readLine();
            while ((line = bufferedReader.readLine()) != null) {
                line = line.strip();
                stringBuilder.append(line);
            }

            int indexOfOpeningBrace = stringBuilder.indexOf("{");
            if (indexOfOpeningBrace != -1) {
                stringBuilder.delete(0, indexOfOpeningBrace); // Remove substring before "{"
            }

            return Response.ok(stringBuilder.toString()).build();
        } catch (IOException e) {
            e.printStackTrace();
            return Response.serverError().entity("Error occurred while reading GeoJSON file").build();
        }
    }

    public String getGeoJSON() {
        String responseBody = getThings();

        ObjectMapper objectMapper = new ObjectMapper();
        List<MeasurementDTO> measurementsList = new ArrayList<>();

        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode allValues = jsonNode.path("value");

            String[] givenSensorNames = {"SSK_USP01", "SSK_USP02",
                    "SSK_USP04", "SSK_USP05", "SSK_USP06"};
            List<String> sensorList = Arrays.asList(givenSensorNames);

            for (JsonNode value : allValues) {
                if (sensorList.contains(value.path("name").asText())) {
                    MeasurementDTO measurementDTO = new MeasurementDTO();
                    measurementDTO.setId(value.path("@iot.id").asInt());
                    measurementDTO.setName(value.path("name").asText());

                    String locationUrl = value.path("Locations@iot.navigationLink")
                            .asText()
                            .replace("\"", "");

                    String locationResponseBody = getClientResponse(locationUrl);
                    JsonNode locationJson = objectMapper.readTree(locationResponseBody);
                    JsonNode location = locationJson.path("value").get(0);

                    measurementDTO.setLongitude(location.path("location").path("coordinates").get(0).asDouble());
                    measurementDTO.setLatitude(location.path("location").path("coordinates").get(1).asDouble());

                    String datastreamsUrl = value.path("Datastreams@iot.navigationLink").asText();
                    String correctUrl = datastreamsUrl.replace("\"", "");
                    measurementDTO.setDatastreams(getDatastreams(correctUrl));

                    measurementsList.add(measurementDTO);
                }
            }

            List<Map<String, Object>> features = new ArrayList<>();
            for (MeasurementDTO measurement : measurementsList) {
                Map<String, Object> feature = new HashMap<>();
                feature.put("type", "Feature");

                Map<String, Object> geometry = new HashMap<>();
                geometry.put("type", "Point");

                List<Double> coordinates = new ArrayList<>();
                coordinates.add(measurement.getLongitude());
                coordinates.add(measurement.getLatitude());

                geometry.put("coordinates", coordinates);
                feature.put("geometry", geometry);

                Map<String, Object> properties = new HashMap<>();
                properties.put("name", measurement.getName());
                for (DatastreamDTO datastream : measurement.getDatastreams()) {
                    properties.put(datastream.getName(), datastream.getMostRecentObservation());
                }
                feature.put("properties", properties);
                features.add(feature);
            }

            // Construct FeatureCollection
            Map<String, Object> geoJSON = new HashMap<>();
            geoJSON.put("type", "FeatureCollection");
            geoJSON.put("features", features);

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(geoJSON);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @GET
    @Path("measurements")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMeasurements() {
        List<MeasurementDTO> measurementsList = new ArrayList<>();

        String responseBody = getThings();

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode allValues = jsonNode.path("value");

            String[] givenSensorNames = {"USP_pu002", "USP_pu009", "USP_pu016", "SSK_USP01", "SSK_USP02",
                    "SSK_USP04", "SSK_USP05", "SSK_USP06"};
            List<String> sensorList = Arrays.asList(givenSensorNames);

            for (JsonNode value : allValues) {
                if (sensorList.contains(value.path("name").asText())) {
                    MeasurementDTO measurementDTO = new MeasurementDTO();
                    measurementDTO.setId(value.path("@iot.id").asInt());
                    measurementDTO.setName(value.path("name").asText());

                    String datastreamsUrl = value.path("Datastreams@iot.navigationLink").asText();
                    String correctUrl = datastreamsUrl.replace("\"", "");
                    measurementDTO.setDatastreams(getDatastreams(correctUrl));

                    measurementsList.add(measurementDTO);
                }
            }

            // Serialize the list of measurements into JSON
            String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(measurementsList);

            return Response.ok(jsonResponse).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("Error occurred while processing data.").build();
        }
    }

    private String getThings() {
        String apiUrl = "https://api-samenmeten.rivm.nl/v1.0/Things?$filter=contains(name,'USP')";
        return getClientResponse(apiUrl);
    }

    private List<DatastreamDTO> getDatastreams(String url) {
        List<DatastreamDTO> datastreamsList = new ArrayList<>();

        String responseBody = getClientResponse(url);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode datastreamJson = objectMapper.readTree(responseBody);
            JsonNode allValues = datastreamJson.path("value");
            HashMap<String, String> names = createNamesFromUnits();

            for (JsonNode value : allValues) {
                double observation = getObservations(value.path("Observations@iot.navigationLink").asText()
                        .replace("\"", ""));

                DatastreamDTO datastreamDTO = new DatastreamDTO();

                for (int i = 0; i < names.keySet().size(); i++) {
                    String currentKey = names.keySet().toArray()[i].toString();
                    if (value.path("name").asText().contains(currentKey)) {
                        if (value.path("name").asText().contains("kal") && !currentKey.contains("kal")) {
                            continue;
                        }
                        String nameValue = names.get(currentKey);
                        datastreamDTO.setName(nameValue);
                    }
                }

                datastreamDTO.setUnitOfMeasurement(value.path("unitOfMeasurement").path("symbol").asText());
                datastreamDTO.setMostRecentObservation(observation);

                datastreamsList.add(datastreamDTO);
            }

            return datastreamsList;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private double getObservations(String url) {
        String responseBody = getClientResponse(url);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode datastreamJson = objectMapper.readTree(responseBody);
            JsonNode allValues = datastreamJson.path("value");

            ArrayList<String> observationTimes = new ArrayList<>();
            for (JsonNode value : allValues) {
                String valueTime = value.path("phenomenonTime").asText().replace("\"", "");
                observationTimes.add(valueTime);
            }

            observationTimes.sort(Collections.reverseOrder());

            String mostRecentTime = observationTimes.get(0);
            JsonNode mostRecentObservation = null;

            for (JsonNode value : allValues) {
                if (value.path("phenomenonTime")
                        .asText().replace("\"", "")
                        .equals(mostRecentTime)) {
                    mostRecentObservation = value;
                }
            }

            return mostRecentObservation.path("result").asDouble();

        } catch (Exception e) {
            e.printStackTrace();
            return -1.0;
        }
    }

    private HashMap<String, String> createNamesFromUnits() {
        HashMap<String, String> namesAndUnits = new HashMap<>();
        namesAndUnits.put("pres", "pressure");
        namesAndUnits.put("rh", "relative humidity");
        namesAndUnits.put("temp", "temperature");
        namesAndUnits.put("no2", "NO2");
        namesAndUnits.put("pm10_kal", "Particle matter (PM1,0) calibrated");
        namesAndUnits.put("pm10", "Particle matter (PM1,0)");
        namesAndUnits.put("pm25_kal", "Particle matter (PM2,5) calibrated");
        namesAndUnits.put("pm25", "Particle matter (PM2,5)");

        return namesAndUnits;
    }

    private String getClientResponse(String url) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(url);
        return target.request().get(String.class);
    }
}
