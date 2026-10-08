package edu.homeautomation.persistence;

import edu.homeautomation.model.AppUser;
import edu.homeautomation.model.AutomationRule;
import edu.homeautomation.model.SmartDevice;
import java.util.List;

/** Generic persistence boundary shared by the demo store and JDBC implementation. */
public interface HomeRepository {
    List<SmartDevice> findDevices();
    List<AutomationRule> findRules();
    List<AppUser> findUsers();
    AppUser findUserByEmail(String email);
    void saveDevice(SmartDevice device);
    void saveRule(AutomationRule rule);
    void updateRule(AutomationRule rule);
    void saveUser(AppUser user);
    void deleteUser(String email);
    String getSetting(String key, String defaultValue);
    void saveSetting(String key, String value);
    boolean isPersistent();
}
