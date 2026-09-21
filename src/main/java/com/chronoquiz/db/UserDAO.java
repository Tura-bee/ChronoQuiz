package com.chronoquiz.db;

import com.chronoquiz.model.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Data Access Object for Player/User entities.
 */
public class UserDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public Player getOrCreateUser(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = "Player";
        }
        name = name.trim();

        // 1. Try to find existing
        String selectSql = "SELECT id, name, created_at FROM users WHERE name = ? COLLATE NOCASE LIMIT 1;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Player p = new Player(rs.getInt("id"), rs.getString("name"));
                    p.setCreatedAt(rs.getString("created_at"));
                    return p;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching user: " + e.getMessage());
        }

        // 2. Insert new user
        String insertSql = "INSERT INTO users (name) VALUES (?);";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return new Player(rs.getInt(1), name);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
        }

        return new Player(1, name);
    }
}
