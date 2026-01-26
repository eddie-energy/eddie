CREATE SCHEMA IF NOT EXISTS user_management;

CREATE TABLE IF NOT EXISTS user_management.roles (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS user_management.users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role_id INTEGER,
    FOREIGN KEY (role_id) REFERENCES user_management.roles(role_id)
);


-- Insert roles if they don't exist
INSERT INTO user_management.roles (role_name)
SELECT 'ROLE_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM user_management.roles WHERE role_name = 'ROLE_ADMIN');

INSERT INTO user_management.roles (role_name)
SELECT 'ROLE_USER'
WHERE NOT EXISTS (SELECT 1 FROM user_management.roles WHERE role_name = 'ROLE_USER');

-- Insert users if they don't exist
INSERT INTO user_management.users (username, password, role_id)
SELECT 'admin', '$2a$10$h35E6aM/M3NIHhHzd4nd8ev1MT22QJ5NlyaZHapOqqF//QSJb4Riq', (SELECT role_id FROM user_management.roles WHERE role_name = 'ROLE_ADMIN')
WHERE NOT EXISTS (SELECT 1 FROM user_management.users WHERE username = 'admin');

INSERT INTO user_management.users (username, password, role_id)
SELECT 'user1', '$2a$10$Z7s0haJJW9g55oH3FiejZe7hOGZqTDTlU6.1AHbH/fO1bshmER476', (SELECT role_id FROM user_management.roles WHERE role_name = 'ROLE_USER')
WHERE NOT EXISTS (SELECT 1 FROM user_management.users WHERE username = 'user1');

INSERT INTO user_management.users (username, password, role_id)
SELECT 'user2', '$2a$10$AcchM71F0ef2ntaKPeLl1eLAnpOjcbw4/dSkrvxgQ8RUTIh7GuhMO', (SELECT role_id FROM user_management.roles WHERE role_name = 'ROLE_USER')
WHERE NOT EXISTS (SELECT 1 FROM user_management.users WHERE username = 'user2');

INSERT INTO user_management.users (username, password, role_id)
SELECT 'user3', '$2a$10$SObhqGFXDUvEzkjdEmOeNOsgoKYZrVS079yXqq2tYnRObl8kBycTq', (SELECT role_id FROM user_management.roles WHERE role_name = 'ROLE_USER')
WHERE NOT EXISTS (SELECT 1 FROM user_management.users WHERE username = 'user3');