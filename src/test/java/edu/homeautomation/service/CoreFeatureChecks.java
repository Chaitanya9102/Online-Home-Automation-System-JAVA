package edu.homeautomation.service;

import edu.homeautomation.model.LightDevice;
import edu.homeautomation.model.SmartDevice;
import edu.homeautomation.persistence.InMemoryHomeRepository;

/** Small dependency-free checks for the shared application services. */
public final class CoreFeatureChecks {
    private CoreFeatureChecks() { }

    public static void main(String[] args) throws Exception {
        automationChangesTheNamedDeviceAndRecordsActivity();
        unsupportedAutomationActionsAreRejected();
        activityHistoryIsBoundedAndNewestFirst();
        temperatureHistoryAndStatusFollowTheConfiguredLimit();
        System.out.println("Core feature checks passed.");
    }

    private static void automationChangesTheNamedDeviceAndRecordsActivity() throws Exception {
        var repository = new InMemoryHomeRepository();
        var activity = new ActivityHistory();
        var service = new DeviceService(repository, activity);
        SmartDevice light = repository.findDevices().stream().filter(LightDevice.class::isInstance).findFirst().orElseThrow();
        service.runAutomation("Turn off " + light.getName());
        check(!light.isOn(), "Automation should turn off its named device.");
        check(activity.latest(1).getFirst().action().equals("Automation run"), "Automation should be recorded.");
    }

    private static void unsupportedAutomationActionsAreRejected() {
        var service = new DeviceService(new InMemoryHomeRepository(), new ActivityHistory());
        expectFailure(() -> service.validateAutomationAction("Unlock the front door"));
        expectFailure(() -> service.validateAutomationAction("Turn on unknown device"));
    }

    private static void activityHistoryIsBoundedAndNewestFirst() {
        var history = new ActivityHistory();
        for (int i=0; i<105; i++) history.record("Tester", "Check", "event " + i);
        check(history.latest(100).size() == 100, "History should retain at most 100 events.");
        check(history.latest(1).getFirst().details().equals("event 104"), "Newest event should be returned first.");
    }

    private static void temperatureHistoryAndStatusFollowTheConfiguredLimit() {
        var environment = new EnvironmentService();
        environment.setTemperatureLimit(28);
        check(environment.temperatureHistory().size() == 1, "Initial simulated reading should be captured.");
        check(environment.temperatureStatus().equals("Within range"), "Default reading should be within range.");
        environment.setTemperatureLimit(20);
        check(environment.temperatureHistory().size() == 2, "A limit update should append a reading.");
        check(environment.temperatureStatus().equals("Alert"), "A reading above the configured limit should alert.");
        expectFailure(() -> environment.setTemperatureLimit(Double.NaN));
    }

    private static void expectFailure(Runnable action) {
        try { action.run(); throw new AssertionError("Expected invalid input to be rejected."); }
        catch (IllegalArgumentException expected) { /* expected */ }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
