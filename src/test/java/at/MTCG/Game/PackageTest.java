package at.MTCG.Game;

import org.junit.jupiter.api.Test;

import java.util.List;

import static at.MTCG.Game.Card.CardType.MONSTER;
import static at.MTCG.Game.Card.CardType.SPELL;
import static at.MTCG.Game.Card.ElementType.*;
import static org.junit.jupiter.api.Assertions.*;

class PackageTest {

    @Test
    void testPackageCreationWithValidCards() {
        // Arrange
        List<Card> validCards = List.of(
                new Card("Card1", MONSTER, FIRE, 50),
                new Card("Card2", SPELL, WATER, 40),
                new Card("Card3", MONSTER, NORMAL, 30),
                new Card("Card4", SPELL, FIRE, 20),
                new Card("Card5", MONSTER, WATER, 10)
        );

        // Act
        Package cardPackage = new Package(validCards);

        // Assert
        assertNotNull(cardPackage, "Package should not be null");
        assertEquals(5, cardPackage.openPackage().size(), "Package should contain exactly 5 cards");
    }

    @Test
    void testPackageCreationWithLessThanFiveCards() {
        // Arrange
        List<Card> invalidCards = List.of(
                new Card("Card1", MONSTER, FIRE, 50),
                new Card("Card2", SPELL, WATER, 40)
        );

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> new Package(invalidCards));
        assertEquals("A package must contain exactly 5 cards.", exception.getMessage());
    }

    @Test
    void testPackageCreationWithMoreThanFiveCards() {
        // Arrange
        List<Card> invalidCards = List.of(
                new Card("Card1", MONSTER, FIRE, 50),
                new Card("Card2", SPELL, WATER, 40),
                new Card("Card3", MONSTER, NORMAL, 30),
                new Card("Card4", SPELL, FIRE, 20),
                new Card("Card5", MONSTER, WATER, 10),
                new Card("Card6", MONSTER, FIRE, 60)
        );

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> new Package(invalidCards));
        assertEquals("A package must contain exactly 5 cards.", exception.getMessage());
    }

    @Test
    void testOpenPackageReturnsAllCards() {
        // Arrange
        List<Card> validCards = List.of(
                new Card("Card1", MONSTER, FIRE, 50),
                new Card("Card2", SPELL, WATER, 40),
                new Card("Card3", MONSTER, NORMAL, 30),
                new Card("Card4", SPELL, FIRE, 20),
                new Card("Card5", MONSTER, WATER, 10)
        );
        Package cardPackage = new Package(validCards);

        // Act
        List<Card> openedCards = cardPackage.openPackage();

        // Assert
        assertEquals(5, openedCards.size(), "Opening package should return exactly 5 cards");
        assertEquals(validCards, openedCards, "Cards in the package should match the original cards");
    }

    @Test
    void testToStringReturnsCorrectRepresentation() {
        // Arrange
        List<Card> validCards = List.of(
                new Card("Card1", MONSTER, FIRE, 50),
                new Card("Card2", SPELL, WATER, 40),
                new Card("Card3", MONSTER, NORMAL, 30),
                new Card("Card4", SPELL, FIRE, 20),
                new Card("Card5", MONSTER, WATER, 10)
        );
        Package cardPackage = new Package(validCards);

        // Act
        String packageString = cardPackage.toString();

        // Assert
        assertTrue(packageString.contains("Package contains:"), "String representation should start with 'Package contains:'");
        assertTrue(packageString.contains("Card1 (MONSTER, FIRE, Damage: 50)"), "String representation should include details of Card1");
        assertTrue(packageString.contains("Card5 (MONSTER, WATER, Damage: 10)"), "String representation should include details of Card5");
    }
}
