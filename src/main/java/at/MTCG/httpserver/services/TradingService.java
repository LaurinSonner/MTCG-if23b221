package at.MTCG.httpserver.services;

import at.MTCG.DataBase.DataBaseConnection;
import at.MTCG.Game.TradingDeal;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradingService implements Service {

    @Override
    public Response handleRequest(Request request) {
        String path = request.getPathname();

        if (Method.GET.equals(request.getMethod()) && "/tradings".equals(path)) {
            return showDeals(request);
        }

        if (Method.POST.equals(request.getMethod()) && "/tradings".equals(path)) {
            return createDeal(request);
        }

        if (path.startsWith("/tradings/")) {
            String[] parts = path.split("/");
            if (parts.length == 3) {
                String tradingDealId = parts[2];

                if (Method.DELETE.equals(request.getMethod())) {
                    return deleteDeal(request, tradingDealId);
                }

                if (Method.POST.equals(request.getMethod())) {
                    return makeDeal(request, tradingDealId);
                }
            }
        }


        return new Response(HttpStatus.NOT_FOUND, ContentType.JSON, "{\"error\": \"Endpoint not found\"}");
    }


    public Response showDeals(Request request) {
        AuthenticateService authenticateService = new AuthenticateService();
        String username = authenticateService.authenticate(request);

        // Check if the user is authenticated
        if (username == null) {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.JSON, "{\"error\": \"Access token is missing or invalid\"}");
        }

        List<Map<String, Object>> tradingDeals = new ArrayList<>();

        try (Connection connection = DataBaseConnection.getConnection()) {
            String query = "SELECT deal_id, offered_card, required_type, required_damage FROM trading_deals";

            try (PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {
                    Map<String, Object> deal = new HashMap<>();
                    deal.put("Id", rs.getString("deal_id"));
                    deal.put("CardToTrade", rs.getString("offered_card"));
                    deal.put("Type", rs.getString("required_type"));  // Monster oder Spell
                    deal.put("MinimumDamage", rs.getInt("required_damage"));

                    tradingDeals.add(deal);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error\"}");
        }

        // If no deals are available, return status 204
        if (tradingDeals.isEmpty()) {
            return new Response(HttpStatus.NO_CONTENT, ContentType.JSON, "{\"message\": \"The request was fine, but there are no trading deals available\"}");
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResponse = objectMapper.writeValueAsString(tradingDeals);
            return new Response(HttpStatus.OK, ContentType.JSON, jsonResponse);
        } catch (JsonProcessingException e) {
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Failed to serialize response\"}");
        }
    }


    public Response createDeal(Request request) {
        AuthenticateService authenticateService = new AuthenticateService();
        String username = authenticateService.authenticate(request);

        if (username == null) {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.JSON, "{\"error\": \"Access token is missing or invalid\"}");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        TradingDeal deal;

        try {
            deal = objectMapper.readValue(request.getBody(), TradingDeal.class);
        } catch (Exception e) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "{\"error\": \"Invalid JSON format\"}");
        }

        // Validation: Was the card provided by the user?
        if (deal.getId() == null || deal.getCardToTrade() == null || deal.getType() == null) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "{\"error\": \"Missing required fields\"}");
        }

        try (Connection connection = DataBaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            // Check if the deal ID already exists
            String checkDealQuery = "SELECT COUNT(*) FROM trading_deals WHERE deal_id = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkDealQuery)) {
                checkStmt.setString(1, deal.getId());
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return new Response(HttpStatus.CONFLICT, ContentType.JSON, "{\"error\": \"A deal with this deal ID already exists\"}");
                }
            }

            // Check if the user owns the card and it is not in the deck
            String checkCardQuery = "SELECT COUNT(*) FROM user_cards WHERE username = ? AND card_id = ? " +
                    "AND NOT EXISTS (SELECT 1 FROM deck WHERE username = ? AND card_id = ?)";
            try (PreparedStatement checkCardStmt = connection.prepareStatement(checkCardQuery)) {
                checkCardStmt.setString(1, username);
                checkCardStmt.setString(2, deal.getCardToTrade());
                checkCardStmt.setString(3, username);
                checkCardStmt.setString(4, deal.getCardToTrade());
                ResultSet rs = checkCardStmt.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    return new Response(HttpStatus.FORBIDDEN, ContentType.JSON, "{\"error\": \"The deal contains a card that is not owned by the user or is locked in the deck\"}");
                }
            }

            // Insert new trading deal into the database
            String insertDealQuery = "INSERT INTO trading_deals (deal_id, offering_user, offered_card, required_type, required_damage) " +
                    "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = connection.prepareStatement(insertDealQuery)) {
                insertStmt.setString(1, deal.getId());
                insertStmt.setString(2, username);
                insertStmt.setString(3, deal.getCardToTrade());
                insertStmt.setString(4, deal.getType().toUpperCase());
                insertStmt.setInt(5, deal.getMinimumDamage());
                insertStmt.executeUpdate();
            }

            connection.commit();
            return new Response(HttpStatus.CREATED, ContentType.JSON, "{\"message\": \"Trading deal successfully created\"}");

        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error\"}");
        }
    }


    public Response deleteDeal(Request request, String tradingDealId) {
        AuthenticateService authenticateService = new AuthenticateService();
        String username = authenticateService.authenticate(request);

        // Check if the user is authenticated
        if (username == null) {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.JSON, "{\"error\": \"Access token is missing or invalid\"}");
        }

        try (Connection connection = DataBaseConnection.getConnection()) {
            // Check if the deal exists and who owns it
            String selectQuery = "SELECT offered_card, offering_user FROM trading_deals WHERE deal_id = ?";
            String offeringUser = null;
            String cardToTrade = null;

            try (PreparedStatement selectStmt = connection.prepareStatement(selectQuery)) {
                selectStmt.setString(1, tradingDealId);
                ResultSet rs = selectStmt.executeQuery();

                if (rs.next()) {
                    offeringUser = rs.getString("offering_user");
                    cardToTrade = rs.getString("offered_card");
                } else {
                    return new Response(HttpStatus.NOT_FOUND, ContentType.JSON, "{\"error\": \"The provided deal ID was not found\"}");
                }
            }

            // Check if the user is the creator of the deal
            if (!username.equals(offeringUser)) {
                return new Response(HttpStatus.FORBIDDEN, ContentType.JSON, "{\"error\": \"The deal contains a card that is not owned by the user\"}");
            }

            // Remove the deal from the table
            String deleteQuery = "DELETE FROM trading_deals WHERE deal_id = ?";
            try (PreparedStatement deleteStmt = connection.prepareStatement(deleteQuery)) {
                deleteStmt.setString(1, tradingDealId);
                deleteStmt.executeUpdate();
            }

            return new Response(HttpStatus.OK, ContentType.JSON, "{\"message\": \"Trading deal successfully deleted\"}");

        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error\"}");
        }
    }


    public Response makeDeal(Request request, String tradingDealId) {
        AuthenticateService authenticateService = new AuthenticateService();
        String username = authenticateService.authenticate(request);

        // Check if the user is authenticated
        if (username == null) {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.JSON, "{\"error\": \"Access token is missing or invalid\"}");
        }

        // Read the body of the request (the card the user wants to offer)
        ObjectMapper objectMapper = new ObjectMapper();
        String offeredCardId;

        try {
            offeredCardId = objectMapper.readValue(request.getBody(), String.class);
        } catch (Exception e) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "{\"error\": \"Invalid JSON format\"}");
        }

        try (Connection connection = DataBaseConnection.getConnection()) {
            // Check if the deal exists
            String selectQuery = "SELECT offering_user, offered_card, required_type, required_damage FROM trading_deals WHERE deal_id = ?";
            String offeringUser = null;
            String requiredType = null;
            int requiredDamage = 0;
            String originalOfferedCard = null;

            try (PreparedStatement selectStmt = connection.prepareStatement(selectQuery)) {
                selectStmt.setString(1, tradingDealId);
                ResultSet rs = selectStmt.executeQuery();

                if (rs.next()) {
                    offeringUser = rs.getString("offering_user");
                    originalOfferedCard = rs.getString("offered_card");
                    requiredType = rs.getString("required_type");
                    requiredDamage = rs.getInt("required_damage");
                } else {
                    return new Response(HttpStatus.NOT_FOUND, ContentType.JSON, "{\"error\": \"The provided deal ID was not found\"}");
                }
            }

            // A user cannot trade with themselves
            if (username.equals(offeringUser)) {
                return new Response(HttpStatus.FORBIDDEN, ContentType.JSON, "{\"error\": \"Trading with self is not allowed\"}");
            }

            // Check if the user owns the card and if it meets the requirements
            String checkCardQuery = "SELECT id, name, damage FROM user_cards JOIN cards ON user_cards.card_id = cards.id WHERE user_cards.username = ? AND user_cards.card_id = ?";
            String cardName = null;
            int cardDamage = 0;

            try (PreparedStatement checkCardStmt = connection.prepareStatement(checkCardQuery)) {
                checkCardStmt.setString(1, username);
                checkCardStmt.setString(2, offeredCardId);
                ResultSet rs = checkCardStmt.executeQuery();

                if (rs.next()) {
                    cardName = rs.getString("name");
                    cardDamage = rs.getInt("damage");
                } else {
                    return new Response(HttpStatus.FORBIDDEN, ContentType.JSON, "{\"error\": \"The offered card is not owned by the user or does not exist\"}");
                }
            }

            // Determine the type
            String cardType = cardName.toLowerCase().contains("spell") ? "spell" : "monster";

            // Check if the card meets the requirements
            if (!cardType.equalsIgnoreCase(requiredType) || cardDamage < requiredDamage) {
                return new Response(HttpStatus.FORBIDDEN, ContentType.JSON, "{\"error\": \"The offered card does not meet the deal requirements\"}");
            }

            // Start the transaction
            connection.setAutoCommit(false);

            // Update card ownership instead of deleting and reinserting
            String updateCardOwnerQuery = "UPDATE user_cards SET username = ? WHERE card_id = ?";

            try (PreparedStatement updateStmt = connection.prepareStatement(updateCardOwnerQuery)) {
                // Change the owner of the offered card (User -> Provider)
                updateStmt.setString(1, offeringUser);
                updateStmt.setString(2, offeredCardId);
                updateStmt.executeUpdate();

                // Change the owner of the original card (Provider -> User)
                updateStmt.setString(1, username);
                updateStmt.setString(2, originalOfferedCard);
                updateStmt.executeUpdate();
            }

            // Delete the trading deal
            String deleteDealQuery = "DELETE FROM trading_deals WHERE deal_id = ?";
            try (PreparedStatement deleteStmt = connection.prepareStatement(deleteDealQuery)) {
                deleteStmt.setString(1, tradingDealId);
                deleteStmt.executeUpdate();
            }

            connection.commit();
            return new Response(HttpStatus.OK, ContentType.JSON, "{\"message\": \"Trading deal successfully executed\"}");

        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error\"}");
        }
    }


}
