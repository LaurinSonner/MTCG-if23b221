package at.MTCG.Game;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CardTest {

    private Card fireMonster;
    private Card fireSpell;
    private Card normalMonster;
    private Card normalSpell;
    private Card waterMonster;
    private Card waterSpell;


    @BeforeEach
    void init() {
        fireMonster = new Card("Dragon", Card.CardType.MONSTER, Card.ElementType.FIRE, 10);
        normalSpell = new Card("NormalSpell", Card.CardType.SPELL, Card.ElementType.NORMAL, 10);
        normalMonster = new Card("Ork",  Card.CardType.MONSTER, Card.ElementType.NORMAL, 10);
        fireSpell = new Card("Fireball", Card.CardType.SPELL, Card.ElementType.FIRE, 10);
        waterMonster = new Card("Dragon", Card.CardType.MONSTER, Card.ElementType.WATER, 10);
        waterSpell = new Card("Waterball", Card.CardType.SPELL, Card.ElementType.WATER, 10);
    }



    @Test
    void testFireMonsterVSNormalMonster() {
        int calculatedDamageCard1 = fireMonster.calculateDamageAgainst(normalMonster);
        int calculatedDamageCard2 = normalMonster.calculateDamageAgainst(fireMonster);

        assertEquals(10, calculatedDamageCard1);
        assertEquals(10, calculatedDamageCard2);
    }

    @Test
    void testFireMonsterVSNormalSpell() {
        int calculatedDamageCard1 = fireMonster.calculateDamageAgainst(normalSpell);
        int calculatedDamageCard2 = normalSpell.calculateDamageAgainst(fireMonster);

        assertEquals(20, calculatedDamageCard1);
        assertEquals(5, calculatedDamageCard2);

    }

    @Test
    void testWaterMonsterVSFireSpell() {
        int calculatedDamageCard1 = waterMonster.calculateDamageAgainst(fireSpell);
        int calculatedDamageCard2 = fireSpell.calculateDamageAgainst(waterMonster);

        assertEquals(20, calculatedDamageCard1);
        assertEquals(5, calculatedDamageCard2);

    }

    @Test
    void testNormalMonsterVSWaterSpell() {
        int calculatedDamageCard1 = normalMonster.calculateDamageAgainst(waterSpell);
        int calculatedDamageCard2 = waterSpell.calculateDamageAgainst(normalMonster);

        assertEquals(20, calculatedDamageCard1);
        assertEquals(5, calculatedDamageCard2);

    }

    @Test
    void testFireSpellVSNormalSpell() {
        int calculatedDamageCard1 = fireSpell.calculateDamageAgainst(normalSpell);
        int calculatedDamageCard2 = normalSpell.calculateDamageAgainst(fireSpell);

        assertEquals(20, calculatedDamageCard1);
        assertEquals(5, calculatedDamageCard2);
    }






}