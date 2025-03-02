package at.MTCG.Game;

import java.util.ArrayList;
import java.util.List;

public class Package {
    private static final int PACKAGE_SIZE = 5; // Each package contains 5 cards
    private List<Card> cards;

    // Constructor to initialize the package with a list of cards
    public Package(List<Card> cards) {
        if (cards == null || cards.size() != PACKAGE_SIZE) {
            throw new IllegalArgumentException("A package must contain exactly " + PACKAGE_SIZE + " cards.");
        }
        this.cards = new ArrayList<>(cards);
    }





    // Method to open the package and get all cards
    public List<Card> openPackage() {
        return new ArrayList<>(cards); // Return a copy to ensure immutability
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Package contains: \n");
        for (Card card : cards) {
            sb.append(card.getName())
                    .append(" (")
                    .append(card.getCardType())
                    .append(", ")
                    .append(card.getElementType())
                    .append(", Damage: ")
                    .append(card.getDamage())
                    .append(")\n");
        }
        return sb.toString();
    }
}
