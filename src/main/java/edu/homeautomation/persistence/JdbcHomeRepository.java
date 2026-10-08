package edu.homeautomation.persistence;

import edu.homeautomation.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import edu.homeautomation.service.PasswordSecurity;

/** SQLite-backed repository; each operation uses prepared statements and a short-lived connection. */
public final class JdbcHomeRepository implements HomeRepository {
    private final String url;

    public JdbcHomeRepository(String url) {
        this.url = url;
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS app_user (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, email TEXT NOT NULL UNIQUE, role TEXT NOT NULL, password_hash TEXT NOT NULL DEFAULT '')");
            ensurePasswordColumn(connection);
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS smart_device (id TEXT PRIMARY KEY, name TEXT NOT NULL, room TEXT NOT NULL, kind TEXT NOT NULL, compatible INTEGER NOT NULL DEFAULT 1, is_on INTEGER NOT NULL DEFAULT 0, level INTEGER NOT NULL DEFAULT 0)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS automation_rule (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, condition_text TEXT NOT NULL, action_text TEXT NOT NULL, enabled INTEGER NOT NULL DEFAULT 1)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS system_setting (setting_key TEXT PRIMARY KEY, setting_value TEXT NOT NULL)");
            seedUser(connection, "Alex Morgan", "alex@example.com", "HOMEOWNER", "Home123!");
            seedUser(connection, "System Administrator", "admin@haven.local", "ADMIN", "Admin123!");
            statement.executeUpdate("INSERT OR IGNORE INTO smart_device(id,name,room,kind,is_on,level) VALUES ('L-101','Living room lights','Living Room','Light',1,78),('T-201','Smart thermostat','Hallway','Thermostat',1,22),('S-301','Front door lock','Entry','Security',1,0),('L-102','Bedroom lamp','Bedroom','Light',0,40),('S-302','Motion sensor','Garage','Security',1,0)");
            statement.executeUpdate("INSERT INTO automation_rule(name,condition_text,action_text,enabled) SELECT 'Good night','At 10:30 PM','Turn off living room lights',1 WHERE NOT EXISTS (SELECT 1 FROM automation_rule)");
            statement.executeUpdate("INSERT OR IGNORE INTO system_setting(setting_key,setting_value) VALUES ('home_name','Alex''s Home'),('temperature_limit','28')");
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to initialize SQLite database: " + exception.getMessage(), exception);
        }
    }

    private Connection open() throws SQLException { return DriverManager.getConnection(url); }

    private void ensurePasswordColumn(Connection connection) throws SQLException {
        boolean found = false;
        try (Statement statement = connection.createStatement(); ResultSet columns = statement.executeQuery("PRAGMA table_info(app_user)")) {
            while (columns.next()) if ("password_hash".equalsIgnoreCase(columns.getString("name"))) found = true;
        }
        if (!found) try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE app_user ADD COLUMN password_hash TEXT NOT NULL DEFAULT ''");
        }
    }

    private void seedUser(Connection connection, String name, String email, String role, String password) throws SQLException {
        String hash = PasswordSecurity.hash(password.toCharArray());
        try (PreparedStatement insert = connection.prepareStatement("INSERT OR IGNORE INTO app_user(name,email,role,password_hash) VALUES(?,?,?,?)")) {
            insert.setString(1, name); insert.setString(2, email); insert.setString(3, role); insert.setString(4, hash); insert.executeUpdate();
        }
        try (PreparedStatement update = connection.prepareStatement("UPDATE app_user SET password_hash=? WHERE email=? AND password_hash=''")) {
            update.setString(1, hash); update.setString(2, email); update.executeUpdate();
        }
    }

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
        try(Connection c=open(); Statement s=c.createStatement(); ResultSet rs=s.executeQuery("SELECT name,email,role,password_hash FROM app_user ORDER BY name")) {
            while(rs.next()) items.add(new AppUser(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4)));
            return items;
        } catch(SQLException exception) { throw failure(exception); }
    }

    @Override public synchronized AppUser findUserByEmail(String email) {
        try (Connection c=open(); PreparedStatement p=c.prepareStatement("SELECT name,email,role,password_hash FROM app_user WHERE lower(email)=lower(?)")) {
            p.setString(1,email);
            try (ResultSet rs=p.executeQuery()) {
                return rs.next() ? new AppUser(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4)) : null;
            }
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
        try(Connection c=open();PreparedStatement p=c.prepareStatement("INSERT INTO app_user(name,email,role,password_hash) VALUES(?,?,?,?) ON CONFLICT(email) DO UPDATE SET name=excluded.name,role=excluded.role,password_hash=CASE WHEN excluded.password_hash='' THEN app_user.password_hash ELSE excluded.password_hash END")) {
            p.setString(1,u.name());p.setString(2,u.email());p.setString(3,u.role());p.setString(4,u.passwordHash());p.executeUpdate();
        } catch(SQLException exception) { throw failure(exception); }
    }
    @Override public synchronized void deleteUser(String email) {
        try(Connection c=open();PreparedStatement p=c.prepareStatement("DELETE FROM app_user WHERE email=?")) { p.setString(1,email);p.executeUpdate(); }
        catch(SQLException exception) { throw failure(exception); }
    }
    @Override public synchronized String getSetting(String key, String defaultValue) {
        try(Connection c=open();PreparedStatement p=c.prepareStatement("SELECT setting_value FROM system_setting WHERE setting_key=?")) {
            p.setString(1,key);try(ResultSet rs=p.executeQuery()){return rs.next()?rs.getString(1):defaultValue;}
        } catch(SQLException exception) { throw failure(exception); }
    }
    @Override public synchronized void saveSetting(String key, String value) {
        try(Connection c=open();PreparedStatement p=c.prepareStatement("INSERT INTO system_setting(setting_key,setting_value) VALUES(?,?) ON CONFLICT(setting_key) DO UPDATE SET setting_value=excluded.setting_value")) {
            p.setString(1,key);p.setString(2,value);p.executeUpdate();
        } catch(SQLException exception) { throw failure(exception); }
    }
    @Override public boolean isPersistent() { return true; }
    private IllegalStateException failure(SQLException exception) { return new IllegalStateException("Database operation failed: "+exception.getMessage(),exception); }
}
