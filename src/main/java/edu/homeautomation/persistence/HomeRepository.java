package edu.homeautomation.persistence;

import edu.homeautomation.model.AppUser;
import edu.homeautomation.model.AutomationRule;
import edu.homeautomation.model.SmartDevice;
import java.util.List;

public interface HomeRepository {
    List<SmartDevice> findDevices();
    List<AutomationRule> findRules();
    List<AppUser> findUsers();
    void saveDevice(SmartDevice device);
    void saveRule(AutomationRule rule);
    void updateRule(AutomationRule rule);
    void saveUser(AppUser user);
    void deleteUser(String email);
    boolean isPersistent();
}
