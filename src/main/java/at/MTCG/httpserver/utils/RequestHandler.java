package at.MTCG.httpserver.utils;

import at.MTCG.httpserver.http.ContentType;
import at.MTCG.httpserver.http.HttpStatus;
import at.MTCG.httpserver.server.Request;
import at.MTCG.httpserver.server.Response;
import at.MTCG.httpserver.server.UnauthorizedException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class RequestHandler implements Runnable {
    private Socket clientSocket;
    private Router router;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;

    public RequestHandler(Socket clientSocket, Router router) throws IOException {
        this.clientSocket = clientSocket;
        this.bufferedReader = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
        this.printWriter = new PrintWriter(this.clientSocket.getOutputStream(), true);
        this.router = router;
    }

    @Override
    public void run() {
        Response response;

        try {

            Request request = new RequestBuilder().buildRequest(this.bufferedReader);

            if (request.getPathname() == null) {
                response = new Response(
                        HttpStatus.BAD_REQUEST,
                        ContentType.JSON,
                        "[]"
                );
            } else {
                try {
                    response = this.router.resolve(request.getServiceRoute()).handleRequest(request);
                } catch (UnauthorizedException e) {
                    response = new Response(
                            HttpStatus.UNAUTHORIZED,
                            ContentType.JSON,
                            e.getMessage());
                }

            }
            printWriter.write(response.get());
        } catch (IOException e) {
            System.err.println(Thread.currentThread().getName() + " Error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (printWriter != null) {
                    printWriter.close();
                }
                if (bufferedReader != null) {
                    bufferedReader.close();
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
