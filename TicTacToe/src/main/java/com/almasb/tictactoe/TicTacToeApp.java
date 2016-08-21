package com.almasb.tictactoe;

import com.almasb.fxgl.app.ApplicationMode;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.settings.GameSettings;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.shape.Line;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * An example of a UI based game.
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
public class TicTacToeApp extends GameApplication {

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("TicTacToe");
        settings.setVersion("0.1");
        settings.setWidth(600);
        settings.setHeight(600);
        settings.setIntroEnabled(false);
        settings.setMenuEnabled(false);
        settings.setShowFPS(false);
        settings.setProfilingEnabled(false);
        settings.setApplicationMode(ApplicationMode.DEVELOPER);
    }

    @Override
    protected void initInput() {}

    @Override
    protected void initAssets() {}

    @Override
    protected void initGame() {}

    @Override
    protected void initPhysics() {}

    private Tile[][] board = new Tile[3][3];
    private List<TileCombo> combos = new ArrayList<>();

    @Override
    protected void initUI() {
        Line line1 = new Line(getWidth() / 3, 0, getWidth() / 3, 0);
        Line line2 = new Line(getWidth() / 3 * 2, 0, getWidth() / 3 * 2, 0);
        Line line3 = new Line(0, getHeight() / 3, 0, getHeight() / 3);
        Line line4 = new Line(0, getHeight() / 3 * 2, 0, getHeight() / 3 * 2);



        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                Tile tile = new Tile();
                tile.setTranslateX(x * getWidth() / 3);
                tile.setTranslateY(y * getHeight() / 3);

                board[x][y] = tile;
                getGameScene().addUINode(tile);
            }
        }

        getGameScene().addUINodes(line1, line2, line3, line4);

        combos.clear();

        // horizontal
        for (int y = 0; y < 3; y++) {
            combos.add(new TileCombo(board[0][y], board[1][y], board[2][y]));
        }

        // vertical
        for (int x = 0; x < 3; x++) {
            combos.add(new TileCombo(board[x][0], board[x][1], board[x][2]));
        }

        // diagonals
        combos.add(new TileCombo(board[0][0], board[1][1], board[2][2]));
        combos.add(new TileCombo(board[2][0], board[1][1], board[0][2]));

        // animation
        KeyFrame frame1 = new KeyFrame(Duration.seconds(0.5),
                new KeyValue(line1.endYProperty(), getHeight()));

        KeyFrame frame2 = new KeyFrame(Duration.seconds(1),
                new KeyValue(line2.endYProperty(), getHeight()));

        KeyFrame frame3 = new KeyFrame(Duration.seconds(0.5),
                new KeyValue(line3.endXProperty(), getWidth()));

        KeyFrame frame4 = new KeyFrame(Duration.seconds(1),
                new KeyValue(line4.endXProperty(), getWidth()));

        Timeline timeline = new Timeline(frame1, frame2, frame3, frame4);
        timeline.play();
    }

    @Override
    protected void onUpdate(double tpf) {}

    private boolean checkGameFinished() {
        for (TileCombo combo : combos) {
            if (combo.isComplete()) {
                gameOver(combo.getWinSymbol());
                return true;
            }
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                Tile tile = board[x][y];
                if (tile.getValue() == TileValue.NONE) {
                    // at least tile is empty
                    return false;
                }
            }
        }

        gameOver("DRAW");
        return true;
    }

    private void gameOver(String winner) {
        getDisplay().showConfirmationBox("Winner: " + winner + "\nContinue?", yes -> {
            if (yes)
                startNewGame();
            else
                exit();
        });
    }

    public void onUserMove(Tile tile) {
        boolean ok = tile.mark(TileValue.X);

        if (ok) {
            boolean over = checkGameFinished();

            if (!over) {
                aiMove();
                checkGameFinished();
            }
        }
    }

    /**
     * This AI move simply select first empty tile.
     * Smarter AI could do something like this in order:
     *
     * 1. Check if there is a player's half-combo and prevent that
     * 2. Check if there is a CPU's half-combo and complete it
     * 3. Check if there is a 3-tile empty combo.
     */
    private void aiMove() {
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                Tile tile = board[x][y];
                if (tile.getValue() == TileValue.NONE) {
                    tile.mark(TileValue.O);
                    return;
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}