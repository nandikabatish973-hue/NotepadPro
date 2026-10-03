-- NotepadPro-Advanced database schema (MySQL)
-- Run this once, e.g.:  mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS notepadpro
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE notepadpro;

CREATE TABLE IF NOT EXISTS categories (
    id   INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS notes (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(255) NOT NULL,
    body       TEXT,
    category   VARCHAR(100) DEFAULT 'General',
    tags       VARCHAR(255) DEFAULT '',
    favorite   TINYINT(1) DEFAULT 0,
    archived   TINYINT(1) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_archived (archived)
);

INSERT IGNORE INTO categories (name) VALUES
    ('General'), ('Work'), ('Personal'), ('Ideas'), ('Archive');
