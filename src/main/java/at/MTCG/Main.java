package at.MTCG;

import at.MTCG.httpserver.server.Server;
import at.MTCG.httpserver.services.*;
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
        PackageService packageService = new PackageService();
        CardService cardService = new CardService();
        TradingService tradingService = new TradingService();
        GameService gameService = new GameService();
        router.addService("/users", userService);
        router.addService("/sessions", userService);
        router.addService("/packages", packageService);
        router.addService("/transactions", packageService);
        router.addService("/cards", cardService);
        router.addService("/deck", cardService);
        router.addService("/tradings", tradingService);
        router.addService("/stats", gameService);
        router.addService("/scoreboard", gameService);
        router.addService("/battles", gameService);
        return router;
    }

}
