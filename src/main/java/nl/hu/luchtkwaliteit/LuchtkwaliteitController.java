package nl.hu.luchtkwaliteit;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
public class LuchtkwaliteitController {
    @GetMapping("/hello")
    public String sayHello(@RequestParam(value = "myName", defaultValue = "World") String name) {
        return String.format("Hello %s!", name);
    }

    @GetMapping("/things")
    public ResponseEntity<String> getThings() {
        String apiUrl = "https://api-samenmeten.rivm.nl/v1.0/Things?$filter=contains(name,'USP')";
        RestTemplate restTemplate = new RestTemplate();

        return restTemplate.getForEntity(apiUrl, String.class);
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

            String[] givenSensorNames = {"USP_pu002", "USP_pu009", "USP_pu016", "SSK_USP01", "SSK_USP02", "SSK_USP03",
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

            for (JsonNode value : allValues) {
                double observation = this.getObservations(value.path("Observations@iot.navigationLink").asText()
                        .replace("\"", ""));

                DatastreamDTO datastreamDTO = new DatastreamDTO();
                datastreamDTO.setName(value.path("name").asText());
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
}
