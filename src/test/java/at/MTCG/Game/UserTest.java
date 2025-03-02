package at.MTCG.Game;

import at.MTCG.Game.BattleResult;
import at.MTCG.Game.Card;
import at.MTCG.Game.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static at.MTCG.Game.Card.CardType.MONSTER;
import static at.MTCG.Game.Card.CardType.SPELL;
import static at.MTCG.Game.Card.ElementType.*;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testBattleDraw() {
        // Arrange
        User user1 = new User("user1", "password1");
        User user2 = new User("user2", "password2");

        List<Card> deck1 = List.of(
                new Card("Card1", MONSTER, NORMAL, 0),
                new Card("Card2", MONSTER, NORMAL, 0),
                new Card("Card3", MONSTER, NORMAL, 0),
                new Card("Card4", MONSTER, NORMAL, 0)
        );

        List<Card> deck2 = List.of(
                new Card("Card5", MONSTER, NORMAL, 0),
                new Card("Card6", MONSTER, NORMAL, 0),
                new Card("Card7", MONSTER, NORMAL, 0),
                new Card("Card8", MONSTER, NORMAL, 0)
        );

        user1.defineDeck(deck1);
        user2.defineDeck(deck2);

        // Act
        BattleResult result = user1.battle(user2);

        // Assert
        assertTrue(result.isDraw(), "The battle should result in a draw.");
        assertNull(result.getWinner(), "There should be no winner in a draw.");
        assertNull(result.getLoser(), "There should be no loser in a draw.");
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
        BattleResult result = user1.battle(user2);

        // Assert
        assertFalse(result.isDraw(), "The battle should not be a draw.");
        assertEquals(user1, result.getWinner(), "User1 should be the winner.");
        assertEquals(user2, result.getLoser(), "User2 should be the loser.");
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
        BattleResult result = user1.battle(user2);

        // Assert
        assertFalse(result.isDraw(), "The battle should not be a draw.");
        assertEquals(user2, result.getWinner(), "User2 should be the winner.");
        assertEquals(user1, result.getLoser(), "User1 should be the loser.");
    }
}
