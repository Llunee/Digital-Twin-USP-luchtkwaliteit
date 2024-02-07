package nl.hu.luchtkwaliteit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@RestController
public class LuchtkwaliteitApplication {

	public static void main(String[] args) {
		SpringApplication.run(LuchtkwaliteitApplication.class, args);
	}

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
	public void getMeasurements() {
		ResponseEntity<String> uspData = this.getThings();
		String responseBody = uspData.getBody();

		ObjectMapper objectMapper = new ObjectMapper();
		try {
			JsonNode jsonNode = objectMapper.readTree(responseBody);

			JsonNode allValues = jsonNode.path("value");

			for (JsonNode value : allValues) {
				System.out.println(value.path("@iot.id").asInt());
				String datastreamsUrl = value.path("Datastreams@iot.navigationLink").toString();
				String correctUrl = datastreamsUrl.replace("\"", "");
				getDatastreams(correctUrl);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void getDatastreams(String url) {
		RestTemplate datastreamsForValue = new RestTemplate();
		ResponseEntity<String> data = datastreamsForValue.getForEntity(url, String.class);
		String responseBody = data.getBody();

		ObjectMapper objectMapper = new ObjectMapper();
		try {
			JsonNode datastreamJson = objectMapper.readTree(responseBody);
			JsonNode allValues = datastreamJson.path("value");

			for (JsonNode value : allValues) {
				System.out.println(value.path("name").toString() + ", unit of measurement: "
						+ value.path("unitOfMeasurement").path("symbol").toString()
						+ ", observation at index 0: "
						+ this.getObservations(value.path("Observations@iot.navigationLink").toString()
								.replace("\"", "")));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private double getObservations(String url) {
		RestTemplate datastreamsForValue = new RestTemplate();
		ResponseEntity<String> data = datastreamsForValue.getForEntity(url, String.class);
		String responseBody = data.getBody();

		ObjectMapper objectMapper = new ObjectMapper();
		try {
			JsonNode datastreamJson = objectMapper.readTree(responseBody);
			JsonNode firstValue = datastreamJson.path("value").get(0);
			if (firstValue == null) {
				return -1.0;
			}
			return firstValue.path("result").asDouble();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}
}
