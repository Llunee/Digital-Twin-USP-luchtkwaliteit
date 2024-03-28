package nl.hu.luchtkwaliteit.presentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.hu.luchtkwaliteit.application.DatastreamDTO;
import nl.hu.luchtkwaliteit.application.MeasurementDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
public class LuchtkwaliteitController {
    @GetMapping("/things")
    public ResponseEntity<String> getThings() {
        String apiUrl = "https://api-samenmeten.rivm.nl/v1.0/Things?$filter=contains(name,'USP')";
        RestTemplate restTemplate = new RestTemplate();

        return restTemplate.getForEntity(apiUrl, String.class);
    }

    @GetMapping("/geojson")
    public ResponseEntity<String> getGeoJSON() throws JsonProcessingException {
        ResponseEntity<String> uspData = this.getThings();
        String responseBody = uspData.getBody();

        ObjectMapper objectMapper = new ObjectMapper();
        List<MeasurementDTO> measurementsList = new ArrayList<>();
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

                    String locationUrl = value.path("Locations@iot.navigationLink")
                            .asText()
                            .replace("\"", "");

                    RestTemplate latAndLong = new RestTemplate();
                    ResponseEntity<String> data = latAndLong.getForEntity(locationUrl, String.class);
                    String locationResponseBody = data.getBody();
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
                System.out.println(measurement.getDatastreams());
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

            // Serialize the list of measurements into JSON
            String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(geoJSON);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            return new ResponseEntity<>(jsonResponse, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }

    @GetMapping("/measurements")
    public ResponseEntity<String> getMeasurements() {
        List<MeasurementDTO> measurementsList = new ArrayList<>();

        ResponseEntity<String> uspData = this.getThings();
        String responseBody = uspData.getBody();

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

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            return new ResponseEntity<>(jsonResponse, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred while processing data.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private List<DatastreamDTO> getDatastreams(String url) {
        List<DatastreamDTO> datastreamsList = new ArrayList<>();

        RestTemplate datastreamsForValue = new RestTemplate();
        ResponseEntity<String> data = datastreamsForValue.getForEntity(url, String.class);
        String responseBody = data.getBody();

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode datastreamJson = objectMapper.readTree(responseBody);
            JsonNode allValues = datastreamJson.path("value");
            HashMap<String, String> names = this.createNamesFromUnits();

            for (JsonNode value : allValues) {
                double observation = this.getObservations(value.path("Observations@iot.navigationLink").asText()
                        .replace("\"", ""));

                DatastreamDTO datastreamDTO = new DatastreamDTO();

                for (int i = 0; i < names.keySet().size(); i++) {
                    String currentKey = names.keySet().toArray()[i].toString();
                    if (value.path("name").asText().contains(currentKey)) {
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
        RestTemplate datastreamsForValue = new RestTemplate();
        ResponseEntity<String> data = datastreamsForValue.getForEntity(url, String.class);
        String responseBody = data.getBody();

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
}
