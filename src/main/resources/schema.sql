CREATE TABLE IF NOT EXISTS app_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    role TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS smart_device (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    room TEXT NOT NULL,
    kind TEXT NOT NULL,
    compatible INTEGER NOT NULL DEFAULT 1,
    is_on INTEGER NOT NULL DEFAULT 0,
    level INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS automation_rule (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    condition_text TEXT NOT NULL,
    action_text TEXT NOT NULL,
    enabled INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS environment_reading (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    temperature REAL NOT NULL,
    security_status TEXT NOT NULL,
    recorded_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT OR IGNORE INTO app_user(name,email,role) VALUES
('Alex Morgan','alex@example.com','HOMEOWNER'),
('System Administrator','admin@example.com','ADMIN');
