package nl.hu.luchtkwaliteit.application;

public class DatastreamDTO {
    private String name;
    private String unitOfMeasurement;
    private double mostRecentObservation;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnitOfMeasurement() {
        return unitOfMeasurement;
    }

    public void setUnitOfMeasurement(String unitOfMeasurement) {
        this.unitOfMeasurement = unitOfMeasurement;
    }

    public double getMostRecentObservation() {
        return mostRecentObservation;
    }

    public void setMostRecentObservation(double mostRecentObservation) {
        this.mostRecentObservation = mostRecentObservation;
    }
}
