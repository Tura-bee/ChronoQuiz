package com.chronoquiz.db;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for Administrator Authentication and Management.
 * Protects credentials using SHA-256 cryptographic hashing.
 */
public class AdminDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Authenticates an admin by username and plain text password.
     */
    public boolean authenticate(String username, String plainPassword) {
        if (username == null || plainPassword == null || username.trim().isEmpty()) {
            return false;
        }

        String inputHash = hashPassword(plainPassword);
        String sql = "SELECT password_hash FROM admins WHERE username = ? COLLATE NOCASE LIMIT 1;";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    return inputHash.equalsIgnoreCase(storedHash);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error during admin authentication: " + e.getMessage());
        }
        return false;
    }

    /**
     * Changes the admin password.
     */
    public boolean changePassword(String username, String newPlainPassword) {
        if (username == null || newPlainPassword == null || newPlainPassword.trim().isEmpty()) {
            return false;
        }

        String newHash = hashPassword(newPlainPassword.trim());
        String sql = "UPDATE admins SET password_hash = ? WHERE username = ? COLLATE NOCASE;";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, username.trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error changing admin password: " + e.getMessage());
            return false;
        }
    }
}
