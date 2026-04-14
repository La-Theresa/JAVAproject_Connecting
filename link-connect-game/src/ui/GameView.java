package ui;

import data.SaveManager;
import model.Constants;
import model.GameSession;
import model.Position;

import java.util.List;

/**
 * 游戏界面视图接口，由控制器驱动。
 */
public interface GameView {
    void showMenuCard();

    void showLoginCard();

    void showRegisterCard();

    void showGameCard();

    void showLeaderboardCard();

    void setGameSession(GameSession session);

    void setHintPair(Position[] pair);

    void updateHud(String userText, String difficultyText, int score, int timeLeft, int combo, String operation);

    void updateLeaderboard(List<String> lines);

    void repaintGame();

    Constants.Difficulty chooseDifficulty();

    SaveManager.SaveSlot chooseSaveSlot(List<SaveManager.SaveSlot> slots);

    void showInfoMessage(String title, String message);

    void showErrorMessage(String title, String message);
}