package at.MTCG.Game;

public abstract class Card {

    public enum CardType {
        MONSTER, SPELL
    }

    public enum ElementType {
        FIRE, WATER, NORMAL
    }

    private String name;
    private CardType cardType;
    private ElementType elementType;
    private int damage;

    public Card(String name, CardType cardType, ElementType elementType, int damage) {
        this.name = name;
        this.cardType = cardType;
        this.elementType = elementType;
        this.damage = damage;
    }

    // Abstract method to handle custom battle logic
    public abstract boolean canAttack(Card opponent);

    // Method to calculate damage in a battle
    public int calculateDamageAgainst(Card opponent) {
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
    private int applyElementalModifiers(Card opponent) {
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
        }
        return this.damage; // No modification otherwise
    }

    // Getters
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


