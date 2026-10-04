package edu.homeautomation.persistence;

import edu.homeautomation.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class JdbcHomeRepository implements HomeRepository {
    private final String url;

    public JdbcHomeRepository(String url) {
        this.url = url;
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS app_user (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, email TEXT NOT NULL UNIQUE, role TEXT NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS smart_device (id TEXT PRIMARY KEY, name TEXT NOT NULL, room TEXT NOT NULL, kind TEXT NOT NULL, compatible INTEGER NOT NULL DEFAULT 1, is_on INTEGER NOT NULL DEFAULT 0, level INTEGER NOT NULL DEFAULT 0)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS automation_rule (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, condition_text TEXT NOT NULL, action_text TEXT NOT NULL, enabled INTEGER NOT NULL DEFAULT 1)");
            statement.executeUpdate("INSERT OR IGNORE INTO app_user(name,email,role) VALUES ('Alex Morgan','alex@example.com','HOMEOWNER'),('System Administrator','admin@example.com','ADMIN')");
            statement.executeUpdate("INSERT OR IGNORE INTO smart_device(id,name,room,kind,is_on,level) VALUES ('L-101','Living room lights','Living Room','Light',1,78),('T-201','Smart thermostat','Hallway','Thermostat',1,22),('S-301','Front door lock','Entry','Security',1,0),('L-102','Bedroom lamp','Bedroom','Light',0,40),('S-302','Motion sensor','Garage','Security',0,0)");
            statement.executeUpdate("INSERT INTO automation_rule(name,condition_text,action_text,enabled) SELECT 'Good night','At 10:30 PM','Turn off living room lights',1 WHERE NOT EXISTS (SELECT 1 FROM automation_rule)");
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to initialize SQLite database: " + exception.getMessage(), exception);
        }
    }

    private Connection open() throws SQLException { return DriverManager.getConnection(url); }

    @Override public synchronized List<SmartDevice> findDevices() {
        List<SmartDevice> items = new ArrayList<>();
        try (Connection c = open(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT * FROM smart_device ORDER BY id")) {
            while (rs.next()) {
                String id=rs.getString("id"), name=rs.getString("name"), room=rs.getString("room");
                boolean on=rs.getInt("is_on") != 0;
                SmartDevice d = switch (rs.getString("kind")) {
                    case "Thermostat" -> new ThermostatDevice(id,name,room,on,Math.max(10,Math.min(32,rs.getInt("level"))));
                    case "Security" -> new SecurityDevice(id,name,room,on);
                    default -> new LightDevice(id,name,room,on,Math.max(0,Math.min(100,rs.getInt("level"))));
                };
                d.setCompatible(rs.getInt("compatible") != 0); items.add(d);
            }
            return items;
        } catch (SQLException exception) { throw failure(exception); }
    }

    @Override public synchronized List<AutomationRule> findRules() {
        List<AutomationRule> items = new ArrayList<>();
        try (Connection c=open(); Statement s=c.createStatement(); ResultSet rs=s.executeQuery("SELECT * FROM automation_rule ORDER BY id")) {
            while (rs.next()) items.add(new AutomationRule(rs.getString("name"),rs.getString("condition_text"),rs.getString("action_text"),rs.getInt("enabled")!=0));
            return items;
        } catch(SQLException exception) { throw failure(exception); }
    }

    @Override public synchronized List<AppUser> findUsers() {
        List<AppUser> items=new ArrayList<>();
        try(Connection c=open(); Statement s=c.createStatement(); ResultSet rs=s.executeQuery("SELECT name,email,role FROM app_user ORDER BY name")) {
            while(rs.next()) items.add(new AppUser(rs.getString(1),rs.getString(2),rs.getString(3)));
            return items;
        } catch(SQLException exception) { throw failure(exception); }
    }

    @Override public synchronized void saveDevice(SmartDevice d) {
        String sql="INSERT INTO smart_device(id,name,room,kind,compatible,is_on,level) VALUES(?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET name=excluded.name,room=excluded.room,kind=excluded.kind,compatible=excluded.compatible,is_on=excluded.is_on,level=excluded.level";
        int level=d instanceof LightDevice l ? l.getBrightness() : d instanceof ThermostatDevice t ? t.getTargetTemperature() : 0;
        try(Connection c=open(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setString(1,d.getId());p.setString(2,d.getName());p.setString(3,d.getRoom());p.setString(4,d.getKind());p.setInt(5,d.isCompatible()?1:0);p.setInt(6,d.isOn()?1:0);p.setInt(7,level);p.executeUpdate();
        } catch(SQLException exception) { throw failure(exception); }
    }
    @Override public synchronized void saveRule(AutomationRule r) {
        try(Connection c=open();PreparedStatement p=c.prepareStatement("INSERT INTO automation_rule(name,condition_text,action_text,enabled) VALUES(?,?,?,?)")) {
            p.setString(1,r.getName());p.setString(2,r.getCondition());p.setString(3,r.getAction());p.setInt(4,r.isEnabled()?1:0);p.executeUpdate();
        } catch(SQLException exception) { throw failure(exception); }
    }
    @Override public synchronized void updateRule(AutomationRule r) {
        try(Connection c=open();PreparedStatement p=c.prepareStatement("UPDATE automation_rule SET enabled=? WHERE name=?")) {
            p.setInt(1,r.isEnabled()?1:0);p.setString(2,r.getName());p.executeUpdate();
        } catch(SQLException exception) { throw failure(exception); }
    }
    @Override public synchronized void saveUser(AppUser u) {
        try(Connection c=open();PreparedStatement p=c.prepareStatement("INSERT INTO app_user(name,email,role) VALUES(?,?,?) ON CONFLICT(email) DO UPDATE SET name=excluded.name,role=excluded.role")) {
            p.setString(1,u.name());p.setString(2,u.email());p.setString(3,u.role());p.executeUpdate();
        } catch(SQLException exception) { throw failure(exception); }
    }
    @Override public synchronized void deleteUser(String email) {
        try(Connection c=open();PreparedStatement p=c.prepareStatement("DELETE FROM app_user WHERE email=?")) { p.setString(1,email);p.executeUpdate(); }
        catch(SQLException exception) { throw failure(exception); }
    }
    @Override public boolean isPersistent() { return true; }
    private IllegalStateException failure(SQLException exception) { return new IllegalStateException("Database operation failed: "+exception.getMessage(),exception); }
}
