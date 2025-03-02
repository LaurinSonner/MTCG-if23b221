package at.MTCG.httpserver.services;

import at.MTCG.DataBase.DataBaseConnection;
import at.MTCG.Game.Card;
import at.MTCG.httpserver.http.ContentType;
import at.MTCG.httpserver.http.HttpStatus;
import at.MTCG.httpserver.http.Method;
import at.MTCG.httpserver.server.Request;
import at.MTCG.httpserver.server.Response;
import at.MTCG.httpserver.server.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.util.*;

public class CardService implements Service {

    @Override
    public Response handleRequest(Request request) {
        if (Method.GET.equals(request.getMethod()) && "/cards".equals(request.getPathname())) {
            return showCards(request);
        }
        if (Method.GET.equals(request.getMethod()) && "/deck".equals(request.getPathname())) {
            return showDeck(request);
        }
        if (Method.PUT.equals(request.getMethod()) && "/deck".equals(request.getPathname())) {
            return configureDeck(request);
        }
        return new Response(HttpStatus.NOT_FOUND, ContentType.JSON, "{\"error\": \"Endpoint not found\"}");
    }

    public Response showCards(Request request) {
        AuthenticateService authenticateService = new AuthenticateService();
        String username = authenticateService.authenticate(request);

        if (username == null) {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.JSON, "{\"error\": \"Authentication failed\"}");
        }

        List<Card> cards = new ArrayList<>();
        try (Connection connection = DataBaseConnection.getConnection()) {
            String query = "SELECT id, name, damage, elementType, cardType " + "FROM user_cards JOIN cards ON user_cards.card_id = cards.id " + "WHERE username = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, username);
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    Card.ElementType elementType = Card.ElementType.valueOf(rs.getString("elementType").toUpperCase());
                    Card.CardType cardType = Card.CardType.valueOf(rs.getString("cardType").toUpperCase());

                    cards.add(new Card(rs.getString("id"), rs.getString("name"), rs.getInt("damage"), elementType, cardType));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error\"}");
        } catch (IllegalArgumentException | NullPointerException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Invalid elementType or cardType in database\"}");
        }

        if (cards.isEmpty()) {
            return new Response(HttpStatus.NO_CONTENT, ContentType.JSON, "{\"message\": \"No cards found\"}");
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResponse = objectMapper.writeValueAsString(cards);
            return new Response(HttpStatus.OK, ContentType.JSON, jsonResponse);
        } catch (Exception e) {
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Failed to serialize response\"}");
        }
    }

    public Response showDeck(Request request) {
        AuthenticateService authenticateService = new AuthenticateService();
        String username = authenticateService.authenticate(request);

        if (username == null) {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.JSON, "{\"error\": \"Access token is missing or invalid\"}");
        }

        List<Card> deck = new ArrayList<>();
        try (Connection connection = DataBaseConnection.getConnection()) {
            String query = "SELECT c.id, c.name, c.damage, c.elementType, c.cardType " + "FROM deck d " + "JOIN cards c ON d.card_id = c.id " + "WHERE d.username = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, username);
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    Card.ElementType elementType = Card.ElementType.valueOf(rs.getString("elementType").toUpperCase());
                    Card.CardType cardType = Card.CardType.valueOf(rs.getString("cardType").toUpperCase());

                    deck.add(new Card(rs.getString("id"), rs.getString("name"), rs.getInt("damage"), elementType, cardType));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error\"}");
        } catch (IllegalArgumentException | NullPointerException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Invalid elementType or cardType in database\"}");
        }

        if (deck.isEmpty()) {
            return new Response(HttpStatus.NO_CONTENT, ContentType.JSON, "{\"message\": \"The request was fine, but the deck doesn't have any cards\"}");
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResponse = objectMapper.writeValueAsString(deck);
            return new Response(HttpStatus.OK, ContentType.JSON, jsonResponse);
        } catch (Exception e) {
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Failed to serialize response\"}");
        }
    }


    public Response configureDeck(Request request) {
        AuthenticateService authenticateService = new AuthenticateService();
        String username = authenticateService.authenticate(request);

        if (username == null) {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.JSON, "{\"error\": \"Access token is missing or invalid\"}");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        List<String> cardIds;

        try {
            cardIds = objectMapper.readValue(request.getBody(), List.class);
        } catch (Exception e) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "{\"error\": \"Invalid JSON format\"}");
        }

        if (cardIds == null || cardIds.size() != 4) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "{\"error\": \"The provided deck did not include the required amount of cards\"}");
        }

        try (Connection connection = DataBaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            // Prüfen, ob der Benutzer alle Karten besitzt
            String checkQuery = "SELECT COUNT(*) FROM user_cards WHERE username = ? AND card_id IN (?, ?, ?, ?)";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, username);
                for (int i = 0; i < 4; i++) {
                    checkStmt.setString(i + 2, cardIds.get(i));
                }
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) != 4) {
                    return new Response(HttpStatus.FORBIDDEN, ContentType.JSON, "{\"error\": \"At least one of the provided cards does not belong to the user or is not available.\"}");
                }
            }

            // Altes Deck des Benutzers löschen
            String deleteOldDeckQuery = "DELETE FROM deck WHERE username = ?";
            try (PreparedStatement deleteStmt = connection.prepareStatement(deleteOldDeckQuery)) {
                deleteStmt.setString(1, username);
                deleteStmt.executeUpdate();
            }

            // Neue Karten zum Deck hinzufügen
            String insertQuery = "INSERT INTO deck (username, card_id) VALUES (?, ?)";
            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                for (String cardId : cardIds) {
                    insertStmt.setString(1, username);
                    insertStmt.setString(2, cardId);
                    insertStmt.executeUpdate();
                }
            }

            connection.commit();
            return new Response(HttpStatus.OK, ContentType.JSON, "{\"message\": \"The deck has been successfully configured\"}");

        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error\"}");
        }
    }

}

