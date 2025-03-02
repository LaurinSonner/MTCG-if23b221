package at.MTCG.Game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)


public class User {


    @JsonProperty("Username")
    private String username;
    @JsonProperty("Password")
    private String password;
    private int coins;
    private List<Card> stack; // The user's full collection of cards
    private List<Card> deck;  // The user's selected deck for battles
    private int wins;
    private int losses;
    private String token;
    private int elo;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.coins = 20; // Every user starts with 20 coins
        this.stack = new ArrayList<>();
        this.deck = new ArrayList<>();
        this.wins = 0;
        this.losses = 0;
    }

    public User() {
    }

    public User(String username, List<Card> deck) {
        this.username = username;
        this.deck = deck;
    }

    public boolean authenticate(String username, String password) {
        return this.username.equals(username) && this.password.equals(password);
    }

    // Get the username (needed for some comparisons)
    public String getUsername() {
        return this.username;
    }

    // Game.User can acquire card packages by spending coins
    public boolean acquirePackage(Package cardPackage) {
        if (coins < 5) {
            return false; // Not enough coins
        }
        coins -= 5; // Deduct the cost of the package
        List<Card> newCards = cardPackage.openPackage(); // Get cards from the package

        this.stack.addAll(newCards); // Add all cards to the stack

        return true;
    }


    // Get the user's current coin balance
    public int getCoins() {
        return coins;
    }

    // Game.User can define their deck by selecting their best 4 cards from the stack
    public void defineDeck(List<Card> selectedCards) {
        if (selectedCards.size() != 4) {
            throw new IllegalArgumentException("A deck must consist of 4 cards.");
        }
        // Clear current deck and assign new cards
        this.deck.clear();
        this.deck.addAll(selectedCards);
    }

    // Get the user's current deck
    public List<Card> getDeck() {
        return new ArrayList<>(deck);  // Return a copy of the deck to ensure immutability
    }

    // Game.User can trade cards
    public boolean tradeCard(Card myCard, User otherUser, Card theirCard) {
        if (!stack.contains(myCard) || !otherUser.stack.contains(theirCard)) {
            return false; // Can't trade if one of the users doesn't own the card
        }
        // Execute the trade
        this.stack.remove(myCard);
        otherUser.stack.remove(theirCard);

        this.stack.add(theirCard);
        otherUser.stack.add(myCard);

        return true;
    }

    // Battle against another user
    public BattleResult battle(User opponent) {
        List<Card> playerDeck = new ArrayList<>(this.deck);
        List<Card> opponentDeck = new ArrayList<>(opponent.deck);

        StringBuilder battleLog = new StringBuilder();
        battleLog.append("Battle between ").append(this.username).append(" and ").append(opponent.username).append("\n");

        int round = 0;
        while (!playerDeck.isEmpty() && !opponentDeck.isEmpty() && round < 100) {
            round++;
            battleLog.append("Round ").append(round).append(":\n");

            // Wählt zufällig eine Karte aus beiden Decks
            Collections.shuffle(playerDeck);
            Collections.shuffle(opponentDeck);
            Card playerCard = playerDeck.get(0);
            Card opponentCard = opponentDeck.get(0);

            // Berechnet den Schaden mit den bereits implementierten Methoden in `Card`
            int playerDamage = playerCard.calculateDamageAgainst(opponentCard);
            int opponentDamage = opponentCard.calculateDamageAgainst(playerCard);

            battleLog.append(this.username).append("'s ").append(playerCard.getName()).append(" (")
                    .append(playerDamage).append(" DMG) vs. ")
                    .append(opponent.username).append("'s ").append(opponentCard.getName()).append(" (")
                    .append(opponentDamage).append(" DMG)\n");

            // Bestimmt den Gewinner der Runde
            if (playerDamage > opponentDamage) {
                battleLog.append(playerCard.getName()).append(" wins!\n");
                opponentDeck.remove(opponentCard);
            } else if (opponentDamage > playerDamage) {
                battleLog.append(opponentCard.getName()).append(" wins!\n");
                playerDeck.remove(playerCard);
            } else {
                battleLog.append("It's a draw!\n");
            }
        }


        if (round >= 100) {
            battleLog.append("Battle reached 100 rounds! It's a forced draw.\n");
            return new BattleResult(null, null, battleLog.toString(), true); // ✅ Winner & Loser sind NULL!
        }

        // Ergebnis berechnen
        if (playerDeck.isEmpty()) {
            battleLog.append(opponent.username).append(" wins the battle!\n");
            updateStatsAfterBattle(false);
            opponent.updateStatsAfterBattle(true);
            return new BattleResult(opponent, this, battleLog.toString(), false);
        } else {
            battleLog.append(this.username).append(" wins the battle!\n");
            updateStatsAfterBattle(true);
            opponent.updateStatsAfterBattle(false);
            return new BattleResult(this, opponent, battleLog.toString(), false);
        }
    }


    private void updateStatsAfterBattle(boolean won) {
        if (won) {
            this.elo += 3;
            this.wins++;
        } else {
            this.elo -= 5;
            this.losses++;
        }
    }


    // Getters for battle statistics
    public int getBattlesWon() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    // Scoreboard comparison: Users are ranked by number of battles won
    public static int compareByScore(User u1, User u2) {
        return Integer.compare(u2.wins, u1.wins); // Descending order
    }

    public String getPassword() {
        return password;
    }

    public String getToken() {
        return token;
    }

    public List<Card> getStack() {
        return stack;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String generateToken() {
        this.token = username + "-mtcgToken";
        return token;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
