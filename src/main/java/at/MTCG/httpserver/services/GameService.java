package at.MTCG.httpserver.services;

import at.MTCG.DataBase.DataBaseConnection;
import at.MTCG.Game.BattleResult;
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameService implements Service {

    private static final Object lobbyLock = new Object();
    private static User waitingUser = null;

    @Override
    public Response handleRequest(Request request) {
        if (Method.GET.equals(request.getMethod()) && "/stats".equals(request.getPathname())) {
            return showStats(request);
        }
        if (Method.GET.equals(request.getMethod()) && "/scoreboard".equals(request.getPathname())) {
            return showScoreboard(request);
        }
        if (Method.POST.equals(request.getMethod()) && "/battles".equals(request.getPathname())) {
            return enterLobby(request);
        }
        return new Response(HttpStatus.NOT_FOUND, ContentType.JSON, "{\"error\": \"Endpoint not found\"}");
    }

    public Response showStats(Request request) {
        AuthenticateService authenticateService = new AuthenticateService();
        String username = authenticateService.authenticate(request);

        // Check if the user is authenticated
        if (username == null) {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.JSON, "{\"error\": \"Access token is missing or invalid\"}");
        }

        // SQL query to retrieve the user's battle statistics
        String query = "SELECT elo, wins, losses FROM battle_stats WHERE username = ?";

        try (Connection connection = DataBaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, username);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                int elo = rs.getInt("elo");
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");

                // Create JSON response
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonResponse = objectMapper.writeValueAsString(Map.of("Name", username, "Elo", elo, "Wins", wins, "Losses", losses));

                return new Response(HttpStatus.OK, ContentType.JSON, jsonResponse);
            } else {
                return new Response(HttpStatus.NOT_FOUND, ContentType.JSON, "{\"error\": \"User stats not found\"}");
            }

        } catch (SQLException | JsonProcessingException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error\"}");
        }
    }


    public Response showScoreboard(Request request) {
        AuthenticateService authenticateService = new AuthenticateService();
        String username = authenticateService.authenticate(request);

        // Check if the user is authenticated
        if (username == null) {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.JSON, "{\"error\": \"Access token is missing or invalid\"}");
        }

        // SQL query to retrieve the scoreboard data (sorted by ELO in descending order)
        String query = "SELECT username, elo, wins, losses FROM battle_stats ORDER BY elo DESC";

        try (Connection connection = DataBaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(query); ResultSet rs = statement.executeQuery()) {

            List<Map<String, Object>> scoreboard = new ArrayList<>();

            while (rs.next()) {
                Map<String, Object> userStats = Map.of("Name", rs.getString("username"), "Elo", rs.getInt("elo"), "Wins", rs.getInt("wins"), "Losses", rs.getInt("losses"));
                scoreboard.add(userStats);
            }

            // Create JSON response
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResponse = objectMapper.writeValueAsString(scoreboard);

            return new Response(HttpStatus.OK, ContentType.JSON, jsonResponse);

        } catch (SQLException | JsonProcessingException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "{\"error\": \"Database error\"}");
        }
    }


    public Response enterLobby(Request request) {
        AuthenticateService authenticateService = new AuthenticateService();
        String username = authenticateService.authenticate(request);

        // Check if the user is authenticated
        if (username == null) {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.JSON, "Access token is missing or invalid");
        }

        try (Connection connection = DataBaseConnection.getConnection()) {
            // Load the player's deck
            List<Card> playerDeck = loadDeck(connection, username);
            if (playerDeck.isEmpty()) {
                return new Response(HttpStatus.FORBIDDEN, ContentType.JSON, "User has no valid deck for battle");
            }

            User player = new User(username, playerDeck);

            synchronized (lobbyLock) {
                if (waitingUser == null) {
                    // Place the player in the lobby and wait
                    waitingUser = player;
                    lobbyLock.wait();
                    return new Response(HttpStatus.OK, ContentType.PLAIN_TEXT, "Battle finished, check logs.");
                } else {
                    // Start the battle when an opponent is available
                    User opponent = waitingUser;
                    waitingUser = null;
                    BattleResult result = player.battle(opponent);
                    updateDatabaseAfterBattle(connection, result);
                    lobbyLock.notify();

                    return new Response(HttpStatus.OK, ContentType.PLAIN_TEXT, result.getBattleLog());
                }
            }
        } catch (SQLException | InterruptedException e) {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.JSON, "Database or server error");
        }
    }

    private List<Card> loadDeck(Connection connection, String username) throws SQLException {
        String query = "SELECT c.id, c.name, c.damage, c.elementType, c.cardType FROM cards c " + "JOIN deck d ON c.id = d.card_id WHERE d.username = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            List<Card> deck = new ArrayList<>();
            while (rs.next()) {
                deck.add(new Card(rs.getString("id"), rs.getString("name"), rs.getInt("damage"), Card.ElementType.valueOf(rs.getString("elementType").toUpperCase()), Card.CardType.valueOf(rs.getString("cardType").toUpperCase())));
            }
            return deck;
        }
    }


    private void updateDatabaseAfterBattle(Connection connection, BattleResult result) throws SQLException {
        if (!result.isDraw()) {
            updatePlayerStats(connection, result.getWinner(), true);
            updatePlayerStats(connection, result.getLoser(), false);
        }
    }

    private void updatePlayerStats(Connection connection, User player, boolean won) throws SQLException {
        String query = "UPDATE battle_stats SET elo = elo + ?, wins = wins + ?, losses = losses + ? WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, won ? 3 : -5);
            stmt.setInt(2, won ? 1 : 0);
            stmt.setInt(3, won ? 0 : 1);
            stmt.setString(4, player.getUsername());
            stmt.executeUpdate();
        }
    }
}


