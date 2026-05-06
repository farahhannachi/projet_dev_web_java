-- Migration script to add password reset tokens table
-- Run this in your MySQL database: pharmacie

-- Create password reset tokens table
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token VARCHAR(36) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES utilisateur(id_utilisateur) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_email (email),
    INDEX idx_expires (expires_at)
);

-- Add some comments explaining the table
ALTER TABLE password_reset_tokens
    COMMENT = 'Stores password reset tokens for users. Tokens expire after 24 hours.';

-- Optional: Add a trigger to automatically delete expired tokens (maintenance)
-- This would need to be set up as a scheduled event in MySQL

