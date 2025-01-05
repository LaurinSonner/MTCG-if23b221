package at.MTCG;

import at.MTCG.httpserver.server.Server;
import at.MTCG.httpserver.services.PackageService;
import at.MTCG.httpserver.services.SessionService;
import at.MTCG.httpserver.services.UserService;
import at.MTCG.httpserver.utils.Router;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Server server = new Server(10001, configureRouter());
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static Router configureRouter() {
        Router router = new Router();
        UserService userService = new UserService();
        router.addService("/users", userService);
        router.addService("/sessions", new SessionService(userService));

        router.addService("/packages", new PackageService());


        return router;
    }

}
