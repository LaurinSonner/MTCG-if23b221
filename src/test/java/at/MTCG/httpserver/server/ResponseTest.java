package at.MTCG.httpserver.server;

import at.MTCG.httpserver.http.ContentType;
import at.MTCG.httpserver.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseTest {
    private Response response;

    @Test
    void testParseSimpleResponse() {
        // Arrange
        response = new Response(HttpStatus.OK, ContentType.PLAIN_TEXT, "Successful request");
        String expectedResponseStringStart = "HTTP/1.1 200 OK\r\n" +
                "Cache-Control: max-age=0\r\n" +
                "Connection: close\r\n";

        String expectedResponseStringEnd =
                "Content-Type: text/plain\r\n" +
                "Content-Length: 18\r\n" +
                "\r\n" +
                "Successful request";

        // Act
        String responseStringToSend = response.get();
        // Assert
        assertTrue(responseStringToSend.startsWith(expectedResponseStringStart), "String does not start with " + expectedResponseStringStart);
        assertTrue(responseStringToSend.endsWith(expectedResponseStringEnd), "String does not end with " + expectedResponseStringEnd);
    }

    @Test
    void testParseEmptyStringResponse() {
        // Arrange
        response = new Response(HttpStatus.CREATED, ContentType.PLAIN_TEXT, "");
        String expectedResponseStringStart = "HTTP/1.1 201 CREATED\r\n" +
                "Cache-Control: max-age=0\r\n" +
                "Connection: close\r\n";

        String expectedResponseStringEnd =
                "Content-Type: text/plain\r\n" +
                        "Content-Length: 0\r\n" +
                        "\r\n";

        // Act
        String responseStringToSend = response.get();
        // Assert
        assertTrue(responseStringToSend.startsWith(expectedResponseStringStart), "String does not start with " + expectedResponseStringStart);
        assertTrue(responseStringToSend.endsWith(expectedResponseStringEnd), "String does not end with " + expectedResponseStringEnd);
    }

    @Test
    void testParseEmptyBodyResponse() {
        // Arrange
        response = new Response(HttpStatus.CREATED, ContentType.PLAIN_TEXT, null);
        String expectedResponseStringStart = "HTTP/1.1 201 CREATED\r\n" +
                "Cache-Control: max-age=0\r\n" +
                "Connection: close\r\n";

        String expectedResponseStringEnd =
                "Content-Type: text/plain\r\n" +
                        "Content-Length: 0\r\n" +
                        "\r\n";

        // Act
        String responseStringToSend = response.get();
        // Assert
        assertTrue(responseStringToSend.startsWith(expectedResponseStringStart), "String does not start with " + expectedResponseStringStart);
        assertTrue(responseStringToSend.endsWith(expectedResponseStringEnd), "String does not end with " + expectedResponseStringEnd);
    }

}