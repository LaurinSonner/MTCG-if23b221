package at.MTCG.Game;

import org.junit.jupiter.api.Test;

import java.util.List;

import static at.MTCG.Game.Card.CardType.MONSTER;
import static at.MTCG.Game.Card.CardType.SPELL;
import static at.MTCG.Game.Card.ElementType.*;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testAcquirePackageWithEnoughCoins() {
        // Arrange
        User user = new User("testUser", "password123");
        List<Card> cards = List.of(
                new Card("Card1", MONSTER, FIRE, 50),
                new Card("Card2", SPELL, WATER, 40),
                new Card("Card3", MONSTER, NORMAL, 30),
                new Card("Card4", SPELL, FIRE, 20),
                new Card("Card5", MONSTER, WATER, 10)
        );
        Package cardPackage = new Package(cards);

        // Act
        boolean result = user.acquirePackage(cardPackage);

        // Assert
        assertTrue(result, "User should successfully acquire the package.");
        assertEquals(15, user.getCoins(), "User should have 15 coins after acquiring the package.");
        assertEquals(5, user.getStack().size(), "User's stack should contain 5 cards.");
    }

    @Test
    void testAcquirePackageWithoutEnoughCoins() {
        // Arrange
        User user = new User("testUser", "password123");
        Package cardPackage = new Package(List.of(
                new Card("Card1", MONSTER, FIRE, 50),
                new Card("Card2", SPELL, WATER, 40),
                new Card("Card3", MONSTER, NORMAL, 30),
                new Card("Card4", SPELL, FIRE, 20),
                new Card("Card5", MONSTER, WATER, 10)
        ));

        user.acquirePackage(cardPackage);
        user.acquirePackage(cardPackage);
        user.acquirePackage(cardPackage);
        user.acquirePackage(cardPackage);


        // Act
        boolean result = user.acquirePackage(cardPackage); // Not enough coins


        // Assert
        assertFalse(result, "User should fail to acquire the package without enough coins.");
        assertEquals(0, user.getCoins(), "User's coin balance should remain unchanged.");
    }

    @Test
    void testDefineDeckWithValidCards() {
        // Arrange
        User user = new User("testUser", "password123");
        List<Card> deckCards = List.of(
                new Card("Card1", MONSTER, FIRE, 50),
                new Card("Card2", SPELL, WATER, 40),
                new Card("Card3", MONSTER, NORMAL, 30),
                new Card("Card4", SPELL, FIRE, 20)
        );

        // Act
        user.defineDeck(deckCards);

        // Assert
        assertEquals(4, user.getDeck().size(), "Deck should contain 4 cards.");
        assertEquals(deckCards, user.getDeck(), "Deck should match the selected cards.");
    }

    @Test
    void testDefineDeckWithInvalidSize() {
        // Arrange
        User user = new User("testUser", "password123");
        List<Card> invalidDeck = List.of(
                new Card("Card1", MONSTER, FIRE, 50),
                new Card("Card2", SPELL, WATER, 40)
        ); // Only 2 cards

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> user.defineDeck(invalidDeck));
        assertEquals("A deck must consist of 4 cards.", exception.getMessage());
    }

    @Test
    void testTradeCardSuccess() {
        // Arrange
        User user1 = new User("user1", "password1");
        User user2 = new User("user2", "password2");

        Card card1 = new Card("Card1", MONSTER, FIRE, 50);
        Card card2 = new Card("Card2", SPELL, WATER, 40);

        user1.getStack().add(card1);
        user2.getStack().add(card2);

        // Act
        boolean result = user1.tradeCard(card1, user2, card2);

        // Assert
        assertTrue(result, "Trade should succeed if both users own the respective cards.");
        assertTrue(user1.getStack().contains(card2), "User1 should now own Card2.");
        assertTrue(user2.getStack().contains(card1), "User2 should now own Card1.");
    }

    @Test
    void testBattleDraw() {
        // Arrange
        User user1 = new User("user1", "password1");
        User user2 = new User("user2", "password2");

        List<Card> deck1 = List.of(
                new Card("Card1", MONSTER, FIRE, 0),
                new Card("Card2", SPELL, WATER, 0),
                new Card("Card3", MONSTER, NORMAL, 0),
                new Card("Card4", SPELL, FIRE, 0)
        );

        List<Card> deck2 = List.of(
                new Card("Card5", MONSTER, WATER, 0),
                new Card("Card6", SPELL, FIRE, 0),
                new Card("Card7", MONSTER, NORMAL, 0),
                new Card("Card8", SPELL, WATER, 0)
        );

        user1.defineDeck(deck1);
        user2.defineDeck(deck2);

        // Act
        boolean result = user1.battle(user2);

        // Assert

            assertEquals(0, user1.getBattlesWon(), "User1 should have won 1 battle.");
            assertEquals(0, user2.getBattlesLost(), "User2 should have lost 1 battle.");

            assertEquals(0, user2.getBattlesWon(), "User2 should have won 1 battle.");
            assertEquals(0, user1.getBattlesLost(), "User1 should have lost 1 battle.");

    }

    @Test
    void testBattleUser1Wins() {
        // Arrange
        User user1 = new User("user1", "password1");
        User user2 = new User("user2", "password2");

        List<Card> deck1 = List.of(
                new Card("Card1", MONSTER, FIRE, 40),
                new Card("Card2", SPELL, WATER, 40),
                new Card("Card3", MONSTER, NORMAL, 40),
                new Card("Card4", SPELL, FIRE, 40)
        );

        List<Card> deck2 = List.of(
                new Card("Card5", MONSTER, WATER, 0),
                new Card("Card6", SPELL, FIRE, 0),
                new Card("Card7", MONSTER, NORMAL, 0),
                new Card("Card8", SPELL, WATER, 0)
        );

        user1.defineDeck(deck1);
        user2.defineDeck(deck2);

        // Act
        boolean result = user1.battle(user2);

        // Assert

        assertEquals(1, user1.getBattlesWon(), "User1 should have won 1 battle.");
        assertEquals(1, user2.getBattlesLost(), "User2 should have lost 1 battle.");

        assertEquals(0, user2.getBattlesWon(), "User2 should have won 1 battle.");
        assertEquals(0, user1.getBattlesLost(), "User1 should have lost 1 battle.");

    }

    @Test
    void testBattleUser2Wins() {
        // Arrange
        User user1 = new User("user1", "password1");
        User user2 = new User("user2", "password2");

        List<Card> deck1 = List.of(
                new Card("Card1", MONSTER, FIRE, 0),
                new Card("Card2", SPELL, WATER, 0),
                new Card("Card3", MONSTER, NORMAL, 0),
                new Card("Card4", SPELL, FIRE, 0)
        );

        List<Card> deck2 = List.of(
                new Card("Card5", MONSTER, WATER, 40),
                new Card("Card6", SPELL, FIRE, 40),
                new Card("Card7", MONSTER, NORMAL, 40),
                new Card("Card8", SPELL, WATER, 40)
        );

        user1.defineDeck(deck1);
        user2.defineDeck(deck2);

        // Act
        boolean result = user1.battle(user2);

        // Assert

        assertEquals(0, user1.getBattlesWon(), "User1 should have won 1 battle.");
        assertEquals(0, user2.getBattlesLost(), "User2 should have lost 1 battle.");

        assertEquals(1, user2.getBattlesWon(), "User2 should have won 1 battle.");
        assertEquals(1, user1.getBattlesLost(), "User1 should have lost 1 battle.");

    }
}