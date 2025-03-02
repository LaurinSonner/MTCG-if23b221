package at.MTCG.httpserver.services;

import at.MTCG.DataBase.DataBaseConnection;
import at.MTCG.httpserver.server.Request;
import at.MTCG.httpserver.server.UnauthorizedException;

import java.sql.*;

public class AuthenticateService {

    public String authenticate(Request request) {
        String token = request.getHeader("Authorization");

        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Access token is missing or invalid");
        }

        token = token.replace("Bearer ", "");

        if (isTokenValid(token)) {
            updateTokenTimestamp(token);
            return getUsernameFromToken(token);
        } else {
            throw new UnauthorizedException("Access token is missing or invalid");
        }
    }

    private boolean isTokenValid(String token) {
        String query = "SELECT valid_until FROM users WHERE token = ?";
        try (Connection connection = DataBaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, token);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                Timestamp validUntil = resultSet.getTimestamp("valid_until");
                return validUntil != null && validUntil.after(new Timestamp(System.currentTimeMillis()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void updateTokenTimestamp(String token) {
        String query = "UPDATE users SET valid_until = ? WHERE token = ?";
        try (Connection connection = DataBaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setTimestamp(1, new Timestamp(System.currentTimeMillis() + 3600000)); // +1 Stunde
            statement.setString(2, token);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getUsernameFromToken(String token) {
        String query = "SELECT username FROM users WHERE token = ?";
        try (Connection connection = DataBaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, token);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("username");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
