package at.MTCG.httpserver.services;


import at.MTCG.Game.User;
import at.MTCG.httpserver.http.ContentType;
import at.MTCG.httpserver.http.HttpStatus;
import at.MTCG.httpserver.http.Method;
import at.MTCG.httpserver.server.Request;
import at.MTCG.httpserver.server.Response;
import at.MTCG.httpserver.server.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UserService implements Service {
    private List<User> users = new ArrayList<>();

    public String getUserPassword(String username) {
        for (User existingUser : users) {
            if (Objects.equals(username, existingUser.getUsername())) {
                return existingUser.getPassword();
            }
        }
        return null;
    }

    public Boolean usernameAlreadyExists(String username) {
        for (User existingUser : users) {
            if (Objects.equals(username, existingUser.getUsername())) {
                return true;
            }
        }
        return false;
    }

    public List<User> getUsers() {
        return users;
    }


    @Override
    public Response handleRequest(Request request) {
        if (Method.POST.equals(request.getMethod())) {

            ObjectMapper objectMapper = new ObjectMapper();
            User newUser = null;
            try {
                newUser = objectMapper.readValue(request.getBody(), User.class);
            } catch (JsonProcessingException e) {
                return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, e.getMessage());
            }

            if (usernameAlreadyExists(newUser.getUsername())) {
                return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "User with username " + newUser.getUsername() + " already exists");
            }

            users.add(newUser);

            String username = newUser.getUsername();//debug
            String password = newUser.getPassword();//debug
            System.out.println("Username: " + username);//debug
            System.out.println("Password: " + password);//debug


            return new Response(HttpStatus.CREATED, ContentType.JSON, "User succesfully created");
        } else {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "Method Not Allowed");
        }
    }
}