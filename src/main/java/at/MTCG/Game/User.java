package at.MTCG.Game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
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
    private int battlesWon;
    private int battlesLost;
    private String token;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.coins = 20; // Every user starts with 20 coins
        this.stack = new ArrayList<>();
        this.deck = new ArrayList<>();
        this.battlesWon = 0;
        this.battlesLost = 0;
    }

    public User()
    {

    }

    public boolean authenticate(String username, String password) {
        return this.username.equals(username) && this.password.equals(password);
    }

    // Get the username (needed for some comparisons)
    public String getUsername() {
        return this.username;
    }

    // Game.User can acquire card packages by spending coins
    public boolean acquirePackage(List<Card> newCards) {
        if (coins < 5) {
            return false;  // Not enough coins
        }
        if (newCards.size() != 5) {
            throw new IllegalArgumentException("A package must consist of 5 cards.");
        }
        coins -= 5;
        stack.addAll(newCards);
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
    public boolean battle(User opponent) {
        List<Card> opponentDeck = opponent.getDeck();
        if (this.deck.size() != 4 || opponentDeck.size() != 4) {
            throw new IllegalArgumentException("Both players must have a full deck of 4 cards.");
        }

        int myWins = 0;
        int opponentWins = 0;

        // Simulate 4 rounds of battle
        for (int i = 0; i < 4; i++) {
            Card myCard = this.deck.get(i);
            Card opponentCard = opponentDeck.get(i);

            // Compare damage (you could expand this by using a battle logic class or method)
            int myDamage = myCard.calculateDamageAgainst(opponentCard);
            int opponentDamage = opponentCard.calculateDamageAgainst(myCard);

            if (myDamage > opponentDamage) {
                myWins++;
            } else if (opponentDamage > myDamage) {
                opponentWins++;
            }
        }

        if (myWins > opponentWins) {
            this.battlesWon++;
            opponent.battlesLost++;
            return true; // This user wins the battle
        } else if (opponentWins > myWins) {
            this.battlesLost++;
            opponent.battlesWon++;
            return false; // Opponent wins the battle
        }
        // If it's a draw, neither win/lose stats change
        return false;
    }

    // Getters for battle statistics
    public int getBattlesWon() {
        return battlesWon;
    }

    public int getBattlesLost() {
        return battlesLost;
    }

    // Scoreboard comparison: Users are ranked by number of battles won
    public static int compareByScore(User u1, User u2) {
        return Integer.compare(u2.battlesWon, u1.battlesWon); // Descending order
    }

    public String getPassword() {
        return password;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String generateToken() {
        this.token = username + "-mtcgToken";
        return token;
    }
}
