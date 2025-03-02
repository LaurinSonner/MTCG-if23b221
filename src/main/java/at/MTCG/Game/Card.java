package at.MTCG.Game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)

public class Card {


    public Card(String id, String name, int damage, ElementType elementType, CardType cardType) {
        this.id = id;
        this.name = name;
        this.damage = damage;
        this.cardType = cardType;
        this.elementType = elementType;
    }

    public enum CardType {
        MONSTER, SPELL
    }

    public enum ElementType {
        FIRE, WATER, NORMAL
    }

    @JsonProperty("Id")
    private String id;
    @JsonProperty("Name")
    private String name;
    private CardType cardType;
    private ElementType elementType;
    @JsonProperty("Damage")
    private int damage;

    public Card(String name, CardType cardType, ElementType elementType, int damage) {
        this.name = name;
        this.damage = damage;
        this.cardType = cardType;
        this.elementType = elementType;
    }

    public Card() {
    }

    // Method to calculate damage in a battle
    public int calculateDamageAgainst(Card opponent) {

        // Check if Special Rule applys
        if (applySpecialRules(opponent)) {
            return 0;
        }

        if (this.cardType == CardType.MONSTER && opponent.cardType == CardType.MONSTER) {
            // Pure monster fight, element does not matter
            return this.damage;
        } else if (this.cardType == CardType.SPELL || opponent.cardType == CardType.SPELL) {
            // Element type affects damage in case of spell interaction
            return applyElementalModifiers(opponent);
        }
        return this.damage;
    }

    // Method to apply elemental effectiveness
    public int applyElementalModifiers(Card opponent) {
        if (this.elementType == ElementType.WATER && opponent.elementType == ElementType.FIRE) {
            return this.damage * 2;  // Water is effective against Fire
        } else if (this.elementType == ElementType.FIRE && opponent.elementType == ElementType.NORMAL) {
            return this.damage * 2;  // Fire is effective against Normal
        } else if (this.elementType == ElementType.NORMAL && opponent.elementType == ElementType.WATER) {
            return this.damage * 2;  // Normal is effective against Water
        } else if (opponent.elementType == ElementType.WATER && this.elementType == ElementType.FIRE) {
            return this.damage / 2;  // Fire is not effective against Water
        } else if (opponent.elementType == ElementType.FIRE && this.elementType == ElementType.NORMAL) {
            return this.damage / 2;  // Normal is not effective against Fire
        } else if (opponent.elementType == ElementType.NORMAL && this.elementType == ElementType.WATER) {
            return this.damage / 2; // Water is not effective against Normal
        }
        return this.damage; // No modification otherwise
    }

    private boolean applySpecialRules(Card opponent) {
        // Goblins greifen keine Drachen an
        if (this.name.toLowerCase().contains("goblin") && opponent.name.toLowerCase().contains("dragon")) {
            return true;
        }

        // Zauberer kontrollieren Orks
        if (this.name.toLowerCase().contains("wizard") && opponent.name.toLowerCase().contains("ork")) {
            return true;
        }

        // Ritter ertrinken sofort durch Wasserzauber
        if (this.name.toLowerCase().contains("knight") && opponent.cardType == CardType.SPELL && opponent.elementType == ElementType.WATER) {
            return true;
        }

        // Kraken sind immun gegen Zauber
        if (this.name.toLowerCase().contains("kraken") && opponent.cardType == CardType.SPELL) {
            return true;
        }

        // Feuerelfen können Drachenangriffe ausweichen
        if (this.name.toLowerCase().contains("fireelf") && opponent.name.toLowerCase().contains("dragon")) {
            return true;
        }

        return false; // Keine Spezialregel greift
    }

    // Getters


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CardType getCardType() {
        return cardType;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public int getDamage() {
        return damage;
    }

    // Setters
    public void setDamage(int damage) {
        this.damage = damage;
    }
}


