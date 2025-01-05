package at.MTCG.httpserver.services;

import at.MTCG.DataBase.DataBaseConnection;
import at.MTCG.Game.Card;
import at.MTCG.Game.Package;
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
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class PackageService implements Service {

    @Override
    public Response handleRequest(Request request) {
        // Route handling based on method and path
        if (Method.POST.equals(request.getMethod()) && "/packages".equals(request.getPathname())) {
            return createCardPackage(request);
        } else {
            return new Response(HttpStatus.NOT_FOUND, ContentType.JSON, "{\"error\": \"Endpoint not found\"}");
        }
    }


    private Response createCardPackage(Request request) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Card> cards;
        try {
            // Parse JSON body to User object
            cards = Arrays.asList(objectMapper.readValue(request.getBody(), Card[].class));
        } catch (JsonProcessingException e) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "{\"error\": \"" + e.getMessage() + "\"}");
        }

        Response response = new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "{\"error\": \"" + "No cards provided" + "\"}");

        for (Card card : cards) {
            response = saveCardToDB(card);
            if (response.getStatus() != 201) {
                break;
            }
        }
        return response;
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
            return new Response(HttpStatus.NOT_FOUND, ContentType.JSON, "{\"error\": \"At least one card in the packages already exists\"}");
        }

        return new Response(HttpStatus.CREATED, ContentType.JSON, "{\"message\": \"Package and cards successfully created\"}");
    }
}



