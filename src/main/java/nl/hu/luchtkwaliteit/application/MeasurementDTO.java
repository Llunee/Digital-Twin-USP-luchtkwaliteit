package nl.hu.luchtkwaliteit.application;

import java.util.List;

public class MeasurementDTO {
    private int id;
    private String name;
    private List<DatastreamDTO> datastreams;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<DatastreamDTO> getDatastreams() {
        return datastreams;
    }

    public void setDatastreams(List<DatastreamDTO> datastreams) {
        this.datastreams = datastreams;
    }
}

