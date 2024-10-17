// SessionService.java
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

public class SessionService implements Service {
    private UserService userService;

    public SessionService(UserService userService) {
        this.userService = userService;
    }



    @Override
    public Response handleRequest(Request request) {
        if (Method.POST.equals(request.getMethod())) {

            ObjectMapper objectMapper = new ObjectMapper();
            User loginUser = null;
            try {
                loginUser = objectMapper.readValue(request.getBody(), User.class);
            } catch (JsonProcessingException e) {
                return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, e.getMessage());
            }


            for (User user : userService.getUsers()) {
                if (user.getUsername().equals(loginUser.getUsername()) &&
                        user.getPassword().equals(loginUser.getPassword())) {
                    String token = loginUser.getToken() == null ? loginUser.generateToken() : loginUser.getToken();

                    return new Response(HttpStatus.OK, ContentType.JSON, "\"Token\": \"Basic " + token + "\"");
                }
            }
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.JSON, "Invalid username/password provided");
        } else {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.JSON, "Method Not Allowed");
        }
    }
}