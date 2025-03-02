package at.MTCG.Game;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TradingDeal {
    @JsonProperty("Id")
    private String id;

    @JsonProperty("CardToTrade")
    private String cardToTrade;

    @JsonProperty("Type")
    private String type; // "monster" oder "spell"

    @JsonProperty("MinimumDamage")
    private int minimumDamage;

    private String offeringUser; // Der Benutzer, der das Angebot erstellt

    public TradingDeal() {
        // Leerer Konstruktor für Jackson
    }

    public TradingDeal(String id, String cardToTrade, String type, int minimumDamage, String offeringUser) {
        this.id = id;
        this.cardToTrade = cardToTrade;
        this.type = type;
        this.minimumDamage = minimumDamage;
        this.offeringUser = offeringUser;
    }

    // GETTER
    public String getId() {
        return id;
    }

    public String getCardToTrade() {
        return cardToTrade;
    }

    public String getType() {
        return type;
    }

    public int getMinimumDamage() {
        return minimumDamage;
    }

    public String getOfferingUser() {
        return offeringUser;
    }

    // SETTER
    public void setId(String id) {
        this.id = id;
    }

    public void setCardToTrade(String cardToTrade) {
        this.cardToTrade = cardToTrade;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setMinimumDamage(int minimumDamage) {
        this.minimumDamage = minimumDamage;
    }

    public void setOfferingUser(String offeringUser) {
        this.offeringUser = offeringUser;
    }

    @Override
    public String toString() {
        return "TradingDeal{" +
                "id='" + id + '\'' +
                ", cardToTrade='" + cardToTrade + '\'' +
                ", type='" + type + '\'' +
                ", minimumDamage=" + minimumDamage +
                ", offeringUser='" + offeringUser + '\'' +
                '}';
    }
}
