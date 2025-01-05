package at.MTCG.httpserver.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HeaderMapTest {

    private HeaderMap headerMap;

    @BeforeEach
     void init() {
        headerMap = new HeaderMap();
    }

    @Test
    void testParseSimpleHeader() {
        // Arrange
        String rawHeader = "Accept: text/html";

        // Act
        headerMap.ingest(rawHeader);

        // Assert
        assertEquals("text/html", headerMap.getHeader("Accept"));
    }

    @Test
    void testHeaderWithColonInValue() {
        // Arrange
        String rawHeader = "Type: test:html";

        // Act
        headerMap.ingest(rawHeader);

        // Assert
        assertEquals("test:html", headerMap.getHeader("Type"));
    }

    @Test
    void testGetNonExistingHeader() {
        // Arrange
        String rawHeader = "Accept: text/html";

        // Act
        headerMap.ingest(rawHeader);

        // Assert
        assertNull(headerMap.getHeader("Type"));
    }
}