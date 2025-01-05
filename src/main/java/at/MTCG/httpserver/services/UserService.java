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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserService implements Service {

    @Override
    public Response handleRequest(Request request) {
        // Route handling based on method and path
        if (Method.POST.equals(request.getMethod()) && "/users".equals(request.getPathname())) {
            return handleUserRegistration(request);
        } else if (Method.GET.equals(request.getMethod()) && request.getPathname().startsWith("/users/")) {
            String username = request.getPathname().substring("/users/".length());
            return handleGetUser(username);
        } else if (Method.PUT.equals(request.getMethod()) && request.getPathname().startsWith("/users/")) {
            String username = request.getPathname().substring("/users/".length());
            return handleUpdateUser(request, username);
        } else if (Method.POST.equals(request.getMethod()) && "/sessions".equals(request.getPathname())) {
            return handleUserLogin(request);
        } else {
            return new Response(HttpStatus.NOT_FOUND, ContentType.JSON, "{\"error\": \"Endpoint not found\"}");
        }
    }

    public List<User> getUsers() {
        List<User> users = new ArrayList<>();
        String query = "SELECT username, password FROM users";
        try (Connection connection = DataBaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                User user = new User();
                user.setUsername(resultSet.getString("username"));
                user.setPassword(resultSet.getString("password"));
                users.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // --- Endpoint: POST /users ---
    private Response handleUserRegistration(Request request) {
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

        // Insert user into the database
        String query = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection connection = DataBaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, newUser.getUsername());
            statement.setString(2, newUser.getPassword());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error\"}");
        }

        return new Response(HttpStatus.CREATED, ContentType.JSON, "{\"message\": \"User successfully created\"}");
    }

    // --- Endpoint: GET /users/{username} ---
    private Response handleGetUser(String username) {
        String query = "SELECT username FROM users WHERE username = ?";
        try (Connection connection = DataBaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            // If user exists, return their data
            if (resultSet.next()) {
                String userData = "{\"username\": \"" + resultSet.getString("username") + "\"}";
                return new Response(HttpStatus.OK, ContentType.JSON, userData);
            } else {
                return new Response(HttpStatus.NOT_FOUND, ContentType.JSON, "{\"error\": \"User not found\"}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error\"}");
        }
    }

    // --- Endpoint: PUT /users/{username} ---
    private Response handleUpdateUser(Request request, String username) {
        ObjectMapper objectMapper = new ObjectMapper();
        User updatedUser;
        try {
            // Parse JSON body to User object
            updatedUser = objectMapper.readValue(request.getBody(), User.class);
        } catch (JsonProcessingException e) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "{\"error\": \"" + e.getMessage() + "\"}");
        }

        // Update user information in the database
        String query = "UPDATE users SET password = ? WHERE username = ?";
        try (Connection connection = DataBaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, updatedUser.getPassword());
            statement.setString(2, username);
            int rowsUpdated = statement.executeUpdate();

            // Check if the update was successful
            if (rowsUpdated > 0) {
                return new Response(HttpStatus.OK, ContentType.JSON, "{\"message\": \"User successfully updated\"}");
            } else {
                return new Response(HttpStatus.NOT_FOUND, ContentType.JSON, "{\"error\": \"User not found\"}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error\"}");
        }
    }

    // --- Endpoint: POST /sessions ---
    private Response handleUserLogin(Request request) {
        ObjectMapper objectMapper = new ObjectMapper();
        User loginUser;
        try {
            // Parse JSON body to User object
            loginUser = objectMapper.readValue(request.getBody(), User.class);
        } catch (JsonProcessingException e) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "{\"error\": \"" + e.getMessage() + "\"}");
        }

        // Verify username and password
        String query = "SELECT password FROM users WHERE username = ?";
        try (Connection connection = DataBaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, loginUser.getUsername());
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String storedPassword = resultSet.getString("password");
                if (storedPassword.equals(loginUser.getPassword())) {
                    String token = "Basic " + loginUser.getUsername() + "-mtcgToken";
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

    // --- Helper Method: Check if username exists ---
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
