package nl.hu.luchtkwaliteit;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class GeoJSONGenerator {
    public void generateGeoJSONFile() {
        try {
            String geoJSONData = generateGeoJSON();
            //vind of maak de file
            Path jsonPath = Path.of("/geoJsonFiles/geoJSONData.json");
            Path pad = Path.of("/geoJsonFiles");
            if (!Files.exists(pad)) {
                Files.createDirectory(pad);
            }
            //open de streams
            OutputStream os = Files.newOutputStream(jsonPath);
            ObjectOutputStream oos = new ObjectOutputStream(os);

            oos.writeObject(geoJSONData);

            //schrijf en sluit de stream
            oos.flush();
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generateGeoJSON() {
        LuchtkwaliteitResource luchtkwaliteitResource = new LuchtkwaliteitResource();
        return luchtkwaliteitResource.getGeoJSON();
    }
}
