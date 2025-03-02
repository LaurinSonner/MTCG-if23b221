package at.MTCG.httpserver.services;

import at.MTCG.DataBase.DataBaseConnection;
import at.MTCG.Game.Card;
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
import java.util.*;

public class PackageService implements Service {

    @Override
    public Response handleRequest(Request request) {
        if (Method.POST.equals(request.getMethod()) && "/packages".equals(request.getPathname())) {
            return createPackage(request);
        }
        if (Method.POST.equals(request.getMethod()) && "/transactions/packages".equals(request.getPathname())) {
            return acquirePackage(request);
        }
        if (Method.POST.equals(request.getMethod()) && "/transactions/sellCard".equals(request.getPathname())) {
            return sellCard(request);
        }
        return new Response(HttpStatus.NOT_FOUND, ContentType.JSON, "{\"error\": \"Endpoint not found\"}");
    }


    private Response createPackage(Request request) {
        AuthenticateService authenticateService = new AuthenticateService();
        String username = authenticateService.authenticate(request);

        if (!("admin".equals(username))) {
            return new Response(HttpStatus.FORBIDDEN, ContentType.JSON, "{\"error\": \"Provided user is not admin\"}");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        List<Card> cards;

        try {
            cards = Arrays.asList(objectMapper.readValue(request.getBody(), Card[].class));
        } catch (JsonProcessingException e) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "{\"error\": \"" + e.getMessage() + "\"}");
        }

        if (cards == null || cards.isEmpty()) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "{\"error\": \"No cards provided\"}");
        }

        try (Connection connection = DataBaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            String packageId = UUID.randomUUID().toString();
            Timestamp createdAt = new Timestamp(System.currentTimeMillis());

            String insertPackageQuery = "INSERT INTO packages (package_id, created_at) VALUES (?, ?)";
            try (PreparedStatement insertPackageStmt = connection.prepareStatement(insertPackageQuery)) {
                insertPackageStmt.setString(1, packageId);
                insertPackageStmt.setTimestamp(2, createdAt);
                insertPackageStmt.executeUpdate();
            }

            String insertCardQuery = "INSERT INTO cards (id, name, damage, cardtype, elementtype) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement insertCardStmt = connection.prepareStatement(insertCardQuery)) {
                for (Card card : cards) {
                    String cardType = card.getName().toLowerCase().contains("spell") ? "Spell" : "Monster";
                    String elementType = "Normal";

                    if (cardType.equals("Spell")) {
                        if (card.getName().toLowerCase().contains("fire")) {
                            elementType = "Fire";
                        } else if (card.getName().toLowerCase().contains("water")) {
                            elementType = "Water";
                        }
                    } else { // Monster
                        if (card.getName().equalsIgnoreCase("Dragon") || card.getName().toLowerCase().contains("fire")) {
                            elementType = "Fire";
                        } else if (card.getName().toLowerCase().startsWith("water")) {
                            elementType = "Water";
                        }
                    }


                    insertCardStmt.setString(1, card.getId());
                    insertCardStmt.setString(2, card.getName());
                    insertCardStmt.setDouble(3, card.getDamage());
                    insertCardStmt.setString(4, cardType);
                    insertCardStmt.setString(5, elementType);
                    insertCardStmt.executeUpdate();
                }
            }

            String insertPackageCardQuery = "INSERT INTO package_cards (package_id, card_id) VALUES (?, ?)";
            try (PreparedStatement insertPackageCardStmt = connection.prepareStatement(insertPackageCardQuery)) {
                for (Card card : cards) {
                    insertPackageCardStmt.setString(1, packageId);
                    insertPackageCardStmt.setString(2, card.getId());
                    insertPackageCardStmt.executeUpdate();
                }
            }

            connection.commit();
            return new Response(HttpStatus.CREATED, ContentType.JSON, "{\"message\": \"Card package created successfully\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error: " + e.getMessage() + "\"}");
        }
    }


    public Response acquirePackage(Request request) {
        AuthenticateService authenticateService = new AuthenticateService();
        String username = authenticateService.authenticate(request);

        try (Connection connection = DataBaseConnection.getConnection()) {

            // Retrieve the user from the database
            String getUserQuery = "SELECT coins FROM users WHERE username = ?";
            int userCoins = 0;

            try (PreparedStatement getUserStmt = connection.prepareStatement(getUserQuery)) {
                getUserStmt.setString(1, username);
                ResultSet rs = getUserStmt.executeQuery();
                if (rs.next()) {
                    userCoins = rs.getInt("coins");
                } else {
                    return new Response(HttpStatus.NOT_FOUND, ContentType.JSON, "User not found");
                }
            }

            // Check if the user has enough coins
            if (userCoins < 5) {
                return new Response(HttpStatus.FORBIDDEN, ContentType.JSON, "Not enough money for buying a card package");
            }

            // Retrieve the oldest package
            String getPackageQuery = "SELECT package_id FROM packages ORDER BY created_at ASC LIMIT 1";
            String packageId = null;

            try (PreparedStatement getPackageStmt = connection.prepareStatement(getPackageQuery);
                 ResultSet rs = getPackageStmt.executeQuery()) {
                if (rs.next()) {
                    packageId = rs.getString("package_id");
                }
            }

            if (packageId == null) {
                return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "No card package available for buying");
            }

            // Deduct coins from the user
            String updateCoinsQuery = "UPDATE users SET coins = coins - 5 WHERE username = ?";
            try (PreparedStatement updateCoinsStmt = connection.prepareStatement(updateCoinsQuery)) {
                updateCoinsStmt.setString(1, username);
                updateCoinsStmt.executeUpdate();
            }

            // Retrieve cards from the package
            String getCardsQuery = "SELECT card_id FROM package_cards WHERE package_id = ?";
            List<String> cardIDs = new ArrayList<>();

            try (PreparedStatement getCardsStmt = connection.prepareStatement(getCardsQuery)) {
                getCardsStmt.setString(1, packageId);
                ResultSet rs = getCardsStmt.executeQuery();
                while (rs.next()) {
                    cardIDs.add(rs.getString("card_id"));
                }
            }

            // Delete the package from the database
            String deletePackageQuery = "DELETE FROM packages WHERE package_id = ?";
            try (PreparedStatement deletePackageStmt = connection.prepareStatement(deletePackageQuery)) {
                deletePackageStmt.setString(1, packageId);
                deletePackageStmt.executeUpdate();
            }

            // Add cards to the user
            String insertUserCardQuery = "INSERT INTO user_cards (username, card_id) VALUES (?, ?)";
            try (PreparedStatement insertUserCardStmt = connection.prepareStatement(insertUserCardQuery)) {
                for (String cardId : cardIDs) {
                    insertUserCardStmt.setString(1, username);
                    insertUserCardStmt.setString(2, cardId);
                    insertUserCardStmt.executeUpdate();
                }
            }

            return new Response(HttpStatus.CREATED, ContentType.JSON, "{\"message\": \"A package has been successfully bought\"}");

        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "Database error");
        }
    }


    public Response sellCard(Request request) {
        AuthenticateService authenticateService = new AuthenticateService();
        String username = authenticateService.authenticate(request);

        if (username == null) {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.JSON, "{\"error\": \"Authentication failed\"}");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        List<Card> cards;

        try {
            cards = Arrays.asList(objectMapper.readValue(request.getBody(), Card[].class));
        } catch (JsonProcessingException e) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "{\"error\": \"Invalid JSON format\"}");
        }

        if (cards.isEmpty()) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "{\"error\": \"No card provided\"}");
        }

        Card cardToSell = cards.get(0);

        try (Connection connection = DataBaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            // Check if the user owns the card
            String checkOwnershipQuery = "SELECT * FROM user_cards WHERE username = ? AND card_id = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkOwnershipQuery)) {
                checkStmt.setString(1, username);
                checkStmt.setString(2, cardToSell.getId());
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next()) {
                    return new Response(HttpStatus.FORBIDDEN, ContentType.JSON, "{\"error\": \"User does not own this card\"}");
                }
            }

            // Remove the card from user_cards
            String deleteCardQuery = "DELETE FROM user_cards WHERE username = ? AND card_id = ?";
            try (PreparedStatement deleteStmt = connection.prepareStatement(deleteCardQuery)) {
                deleteStmt.setString(1, username);
                deleteStmt.setString(2, cardToSell.getId());
                deleteStmt.executeUpdate();
            }

            // Add a coin to the user
            String updateCoinsQuery = "UPDATE users SET coins = coins + 1 WHERE username = ?";
            try (PreparedStatement updateCoinsStmt = connection.prepareStatement(updateCoinsQuery)) {
                updateCoinsStmt.setString(1, username);
                updateCoinsStmt.executeUpdate();
            }

            connection.commit();
            return new Response(HttpStatus.OK, ContentType.JSON, "{\"message\": \"Card successfully sold\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error\"}");
        }
    }


    private Response saveCardToDB(Card card) {
        // Update user information in the database
        String query = "INSERT INTO cards (id, damage, owner, name) VALUES (?, ?, ?, ?)";
        try (Connection connection = DataBaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, card.getId());
            statement.setInt(2, card.getDamage());
            statement.setString(3, null);
            statement.setString(4, card.getName());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(HttpStatus.CONFLICT, ContentType.JSON, "{\"error\": \"At least one card in the packages already exists\"}");
        }

        return new Response(HttpStatus.CREATED, ContentType.JSON, "{\"message\": \"Package and cards successfully created\"}");
    }


    public Response getRandomPackageId() {
        String query = "SELECT package_id FROM packages ORDER BY RANDOM() LIMIT 1"; // Wählt ein zufälliges Paket aus

        try (Connection connection = DataBaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {

            if (rs.next()) {
                String packageId = rs.getString("package_id");
                return new Response(HttpStatus.OK, ContentType.JSON, "{\"package_id\": \"" + packageId + "\"}");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error: " + e.getMessage() + "\"}");
        }

        return new Response(HttpStatus.NOT_FOUND, ContentType.JSON, "{\"error\": \"No package found\"}");
    }


    public User findUserByUsername(String username) {
        String query = "SELECT username, password FROM users WHERE username = ?";

        try (Connection connection = DataBaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, username);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getString("username"),
                        rs.getString("password")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Failed to search for user: " + e.getMessage(), e);
        }

        return null; // If no user was found
    }


}