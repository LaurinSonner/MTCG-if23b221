package at.MTCG.Game;

public class BattleResult {
    private final User winner;
    private final User loser;
    private final String battleLog;
    private final boolean draw;

    public BattleResult(User winner, User loser, String battleLog, boolean draw) {
        this.winner = winner;
        this.loser = loser;
        this.battleLog = battleLog;
        this.draw = draw;
    }

    public String getBattleLog() {
        return battleLog;
    }

    public boolean isDraw() {
        return draw;
    }

    public User getWinner() {
        return winner;
    }

    public User getLoser() {
        return loser;
    }
}
