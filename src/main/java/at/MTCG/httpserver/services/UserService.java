package at.MTCG.httpserver.services;

import at.MTCG.DataBase.DataBaseConnection;
import at.MTCG.Game.User;
import at.MTCG.httpserver.http.ContentType;
import at.MTCG.httpserver.http.HttpStatus;
import at.MTCG.httpserver.http.Method;
import at.MTCG.httpserver.server.Request;
import at.MTCG.httpserver.server.Response;
import at.MTCG.httpserver.server.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.util.Map;

public class UserService implements Service {

    @Override
    public Response handleRequest(Request request) {
        String path = request.getPathname();

        // Route handling based on HTTP method and path
        if (Method.POST.equals(request.getMethod()) && "/users".equals(path)) {
            return userRegistration(request);
        }

        if (Method.POST.equals(request.getMethod()) && "/sessions".equals(path)) {
            return userLogin(request);
        }

        // GET /users/{username}
        if (Method.GET.equals(request.getMethod()) && path.startsWith("/users/")) {
            String[] parts = path.split("/");
            if (parts.length == 3) { // Format: /users/{username}
                String username = parts[2];
                return getUserData(request, username);
            }
        }

        // PUT /users/{username}
        if (Method.PUT.equals(request.getMethod()) && path.startsWith("/users/")) {
            String[] parts = path.split("/");
            if (parts.length == 3) { // Format: /users/{username}
                String username = parts[2];
                return updateUser(request, username);
            }
        }

        return new Response(HttpStatus.NOT_FOUND, ContentType.JSON, "{\"error\": \"Endpoint not found\"}");
    }


    public Response userRegistration(Request request) {
        ObjectMapper objectMapper = new ObjectMapper();
        User newUser;
        try {
            // Parse JSON body to User object
            newUser = objectMapper.readValue(request.getBody(), User.class);
        } catch (JsonProcessingException e) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "{\"error\": \"" + e.getMessage() + "\"}");
        }

        // Check if the username already exists
        if (usernameAlreadyExists(newUser.getUsername())) {
            return new Response(HttpStatus.CONFLICT, ContentType.JSON, "{\"error\": \"User already exists\"}");
        }

        Connection connection = null;
        try {
            connection = DataBaseConnection.getConnection();
            connection.setAutoCommit(false); // Transaktion starten

            // Insert user into the database
            String insertUserQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (PreparedStatement userStatement = connection.prepareStatement(insertUserQuery)) {
                userStatement.setString(1, newUser.getUsername());
                userStatement.setString(2, newUser.getPassword());
                userStatement.executeUpdate();
            }

            // Insert initial battle stats
            String insertStatsQuery = "INSERT INTO battle_stats (username, elo, wins, losses) VALUES (?, 100, 0, 0)";
            try (PreparedStatement statsStatement = connection.prepareStatement(insertStatsQuery)) {
                statsStatement.setString(1, newUser.getUsername());
                statsStatement.executeUpdate();
            }

            connection.commit(); // Transaktion abschließen
            return new Response(HttpStatus.CREATED, ContentType.JSON, "{\"message\": \"User successfully created\"}");

        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback(); // Rollback bei Fehlern
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error\"}");
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }


    public Response getUserData(Request request, String username) {
        AuthenticateService authenticateService = new AuthenticateService();
        String authenticatedUser = authenticateService.authenticate(request);

        // Check if the user is authenticated
        if (authenticatedUser == null) {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.JSON, "{\"error\": \"Access token is missing or invalid\"}");
        }

        // Only the user themselves or an admin is allowed to retrieve the data
        if (!authenticatedUser.equals(username) && !authenticatedUser.equals("admin")) {
            return new Response(HttpStatus.FORBIDDEN, ContentType.JSON, "{\"error\": \"You are not authorized to view this user's data\"}");
        }

        String query = "SELECT username, bio, image FROM users WHERE username = ?";
        try (Connection connection = DataBaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                String name = rs.getString("username");
                String bio = rs.getString("bio");
                String image = rs.getString("image");

                // Construct JSON response
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonResponse = objectMapper.writeValueAsString(Map.of(
                        "Name", name,
                        "Bio", bio != null ? bio : "",
                        "Image", image != null ? image : ""
                ));

                return new Response(HttpStatus.OK, ContentType.JSON, jsonResponse);
            } else {
                return new Response(HttpStatus.NOT_FOUND, ContentType.JSON, "{\"error\": \"User not found\"}");
            }
        } catch (SQLException | JsonProcessingException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error\"}");
        }
    }


    public Response updateUser(Request request, String username) {
        AuthenticateService authenticateService = new AuthenticateService();
        String authenticatedUser = authenticateService.authenticate(request);

        // Check if the user is authenticated
        if (authenticatedUser == null) {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.JSON, "{\"error\": \"Access token is missing or invalid\"}");
        }

        // Check if the authenticated user is the same or an admin
        if (!authenticatedUser.equals(username) && !authenticatedUser.equals("admin")) {
            return new Response(HttpStatus.FORBIDDEN, ContentType.JSON, "{\"error\": \"You are not authorized to update this user's data\"}");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> userData;

        try {
            userData = objectMapper.readValue(request.getBody(), Map.class);
        } catch (JsonProcessingException e) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "{\"error\": \"Invalid JSON format\"}");
        }

        // Extract the new values from the JSON body
        String newName = userData.get("Name");
        String newBio = userData.get("Bio");
        String newImage = userData.get("Image");

        // Check if the user exists
        String checkUserQuery = "SELECT username FROM users WHERE username = ?";
        try (Connection connection = DataBaseConnection.getConnection();
             PreparedStatement checkStmt = connection.prepareStatement(checkUserQuery)) {

            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                return new Response(HttpStatus.NOT_FOUND, ContentType.JSON, "{\"error\": \"User not found\"}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error\"}");
        }

        // Update user information
        String updateQuery = "UPDATE users SET bio = ?, image = ? WHERE username = ?";
        try (Connection connection = DataBaseConnection.getConnection();
             PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {

            updateStmt.setString(1, newBio);
            updateStmt.setString(2, newImage);
            updateStmt.setString(3, username);

            int updatedRows = updateStmt.executeUpdate();

            if (updatedRows == 0) {
                return new Response(HttpStatus.NOT_FOUND, ContentType.JSON, "{\"error\": \"User not found\"}");
            }

            return new Response(HttpStatus.OK, ContentType.JSON, "{\"message\": \"User successfully updated\"}");

        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error\"}");
        }
    }

    public Response userLogin(Request request) {
        ObjectMapper objectMapper = new ObjectMapper();
        User loginUser;
        try {
            loginUser = objectMapper.readValue(request.getBody(), User.class);
        } catch (JsonProcessingException e) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "{\"error\": \"" + e.getMessage() + "\"}");
        }

        String query = "SELECT password FROM users WHERE username = ?";
        try (Connection connection = DataBaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, loginUser.getUsername());
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String storedPassword = resultSet.getString("password");
                if (storedPassword.equals(loginUser.getPassword())) {
                    String token = loginUser.generateToken();
                    long validUntilTime = System.currentTimeMillis() + 3600000; // 1 hour from now (3600000ms = 1 hour)
                    Timestamp validUntil = new Timestamp(validUntilTime);

                    // Store token and valid_until in the database
                    String updateQuery = "UPDATE users SET token = ?, valid_until = ? WHERE username = ?";
                    try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                        updateStatement.setString(1, token);
                        updateStatement.setTimestamp(2, validUntil); // Setzen des valid_until Werts
                        updateStatement.setString(3, loginUser.getUsername());
                        int updatedRows = updateStatement.executeUpdate();

                        if (updatedRows == 0) {
                            // Logging for debugging
                            System.err.println("Failed to update token: No rows affected for username " + loginUser.getUsername());
                            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Failed to update token\"}");
                        }
                    } catch (SQLException e) {
                        // Log the exception with detailed error information
                        System.err.println("Database error occurred while updating token: " + e.getMessage());
                        e.printStackTrace();
                        return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Internal server error\"}");
                    }

                    return new Response(HttpStatus.OK, ContentType.JSON, "{\"token\": \"" + token + "\"}");
                } else {
                    return new Response(HttpStatus.UNAUTHORIZED, ContentType.JSON, "{\"error\": \"Invalid username/password\"}");
                }
            } else {
                return new Response(HttpStatus.NOT_FOUND, ContentType.JSON, "{\"error\": \"User not found\"}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error\"}");
        }
    }


    private Boolean usernameAlreadyExists(String username) {
        String query = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection connection = DataBaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
