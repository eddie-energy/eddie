package energy.eddie.regionconnector.shared.services;

public interface CommonPollingService<T> {

    void pollTimeSeriesData(T activePermission);
}
