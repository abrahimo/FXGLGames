/*
 * The MIT License (MIT)
 *
 * FXGL - JavaFX Game Library
 *
 * Copyright (c) 2015-2017 AlmasB (almaslvl@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.almasb.fxglgames.spaceinvaders;

import com.almasb.fxgl.app.ApplicationMode;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.core.math.FXGLMath;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.event.Handles;
import com.almasb.fxgl.input.*;
import com.almasb.fxgl.io.FS;
import com.almasb.fxgl.settings.GameSettings;
import com.almasb.fxgl.ui.UI;
import com.almasb.fxglgames.spaceinvaders.collision.*;
import com.almasb.fxglgames.spaceinvaders.control.EnemyControl;
import com.almasb.fxglgames.spaceinvaders.control.PlayerControl;
import com.almasb.fxglgames.spaceinvaders.event.BonusPickupEvent;
import com.almasb.fxglgames.spaceinvaders.event.GameEvent;
import com.almasb.fxglgames.spaceinvaders.level.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.almasb.fxgl.app.DSLKt.*;
import static com.almasb.fxglgames.spaceinvaders.Config.*;

/**
 * A simple clone of Space Invaders. Demonstrates basic FXGL features.
 *
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 */
public class SpaceInvadersApp extends GameApplication {

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("Space Invaders");
        settings.setVersion("1.0");
        settings.setWidth(WIDTH);
        settings.setHeight(HEIGHT);
        settings.setProfilingEnabled(true);
//        settings.setIntroEnabled(true);
//        settings.setMenuEnabled(true);
//        settings.setEnabledMenuItems(EnumSet.of(MenuItem.EXTRA));
        settings.setApplicationMode(ApplicationMode.DEVELOPER);
    }

    @Override
    protected void initInput() {
        Input input = getInput();

        input.addInputMapping(new InputMapping("Move Left", KeyCode.A));
        input.addInputMapping(new InputMapping("Move Right", KeyCode.D));
        input.addInputMapping(new InputMapping("Shoot", MouseButton.PRIMARY));
        input.addInputMapping(new InputMapping("Laser Beam", MouseButton.SECONDARY));

        // developer cheats
        if (getSettings().getApplicationMode() != ApplicationMode.RELEASE) {
            input.addAction(new UserAction("Next Level") {
                @Override
                protected void onActionBegin() {
                    nextLevel();
                }
            }, KeyCode.L);
        }
    }

    @OnUserAction(name = "Move Left")
    public void moveLeft() {
        playerControl.left();
    }

    @OnUserAction(name = "Move Right")
    public void moveRight() {
        playerControl.right();
    }

    @OnUserAction(name = "Shoot")
    public void shoot() {
        playerControl.shoot();
    }

    @OnUserAction(name = "Laser Beam", type = ActionType.ON_ACTION_BEGIN)
    public void laserBeam() {
        playerControl.shootLaser();
    }

    private Entity player;
    private PlayerControl playerControl;

    private int highScore;
    private String highScoreName;

    private GameController uiController;

    private List<SpaceLevel> levels;

    @Override
    protected void preInit() {
        getAudioPlayer().setGlobalSoundVolume(0.2);
        getAudioPlayer().setGlobalMusicVolume(0.2);

        loopBGM("bgm.mp3");
    }

    private SaveData savedData = null;

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("score", 0);
        vars.put("level", 0);
        vars.put("lives", START_LIVES);
        vars.put("enemiesKilled", 0);
        vars.put("laserMeter", 0.0);
    }

    @Override
    protected void initGame() {
        getGameWorld().addEntityFactory(new SpaceInvadersFactory());

        // we have to use file system directly, since we are running without menus
        FS.<SaveData>readDataTask(SAVE_DATA_NAME)
                .onSuccess(data -> savedData = data)
                .onFailure(ignore -> {})
                .run();

        initGame(savedData == null
                ? new SaveData("CPU", ACHIEVEMENT_MASTER_SCORER)
                : savedData);
    }

    @Override
    protected void initPhysics() {
        getPhysicsWorld().addCollisionHandler(new BonusPlayerHandler());
        getPhysicsWorld().addCollisionHandler(new BulletEnemyHandler());
        getPhysicsWorld().addCollisionHandler(new BulletPlayerHandler());
        getPhysicsWorld().addCollisionHandler(new BulletWallHandler());
        getPhysicsWorld().addCollisionHandler(new LaserBeamEnemyHandler());
    }

    private void initGame(SaveData data) {
        highScoreName = data.getName();
        highScore = data.getHighScore();

        levels = Arrays.asList(
                new Level1(),
                new Level2(),
                new Level3(),
                new Level4(),
                new Level5(),
                new Level6(),
                new Level7(),
                new BossLevel()
        );

        spawnBackground();
        spawnPlayer();

        if (!runningFirstTime)
            nextLevel();
    }

    private void spawnBackground() {
        spawn("Background");

        spawn("Stars");
        
        getMasterTimer().runAtInterval(() -> spawn("Meteor"), Duration.seconds(3));
    }

    private void spawnPlayer() {
        player = spawn("Player", getWidth() / 2 - 20, getHeight() - 40);
        playerControl = player.getComponent(PlayerControl.class);
    }

    private void spawnWall(double x, double y) {
        spawn("Wall", x, y);
    }

    private void spawnRandomBonus() {
        spawnBonus(FXGLMath.random(BonusType.values()).get());
    }

    private void spawnBonus(BonusType type) {
        double x = FXGLMath.random(getWidth() - 50);
        double y = FXGLMath.random(getHeight() / 3);

        getGameWorld().spawn("Bonus", new SpawnData(x, y).put("type", type));
    }

    private void initLevel() {
        getCurrentLevel().init();

        // TODO: move wall init to level so we can have walls in different places
        spawnWall(40, getHeight() - 100);
        spawnWall(120, getHeight() - 100);

        spawnWall(getWidth() - 160 - 40, getHeight() - 100);
        spawnWall(getWidth() - 80 - 40, getHeight() - 100);

        getInput().setProcessInput(true);
    }

    private void cleanupLevel() {
        getGameWorld().getEntitiesByType(
                SpaceInvadersType.ENEMY,
                SpaceInvadersType.BONUS,
                SpaceInvadersType.WALL,
                SpaceInvadersType.BULLET,
                SpaceInvadersType.LASER_BEAM)
                .forEach(Entity::removeFromWorld);

        getCurrentLevel().destroy();
    }

    private void nextLevel() {
        getInput().setProcessInput(false);

        if (geti("level") > 0)
            cleanupLevel();

        set("enemiesKilled", 0);
        inc("level", +1);

        if (geti("level") > levels.size()) {
            showGameOver();
            return;
        }

        spawn("LevelInfo");

        getMasterTimer().runOnceAfter(this::initLevel, Duration.seconds(LEVEL_START_DELAY));

        play(Asset.SOUND_NEW_LEVEL);
    }

    private SpaceLevel getCurrentLevel() {
        // levels are counted 1, 2, 3 ...
        // list indices are   0, 1, 2 ...
        return levels.get(geti("level") - 1);
    }

    @Override
    protected void initUI() {
        uiController = new GameController(getGameScene());

        UI ui = getAssetLoader().loadUI(Asset.FXML_MAIN_UI, uiController);

        uiController.getLabelScore().textProperty().bind(getip("score").asString("Score: %d"));
        uiController.getLabelHighScore().setText("HiScore: " + highScore + " " + highScoreName + "");
        uiController.getLaserMeter().currentValueProperty().bind(getdp("laserMeter"));

        IntStream.range(0, geti("lives"))
                .forEach(i -> uiController.addLife());

        getGameScene().addUI(ui);
    }

    private boolean runningFirstTime = true;

    @Override
    protected void onUpdate(double tpf) {
        if (runningFirstTime) {
            nextLevel();
            runningFirstTime = false;
        }
    }

    @Handles(eventType = "PLAYER_GOT_HIT")
    public void onPlayerGotHit(GameEvent event) {
        getGameScene().getViewport().shake(14, 0.35);

        inc("lives", -1);
        uiController.loseLife();

        playerControl.enableInvincibility();

        getMasterTimer().runOnceAfter(playerControl::disableInvincibility, Duration.seconds(INVINCIBILITY_TIME));

        play(Asset.SOUND_LOSE_LIFE);

        if (geti("lives") == 0)
            showGameOver();
    }

    private int scoreForKill() {
        return SCORE_ENEMY_KILL * (getGameState().getGameDifficulty().ordinal() + SCORE_DIFFICULTY_MODIFIER);
    }

    @Handles(eventType = "ENEMY_KILLED")
    public void onEnemyKilled(GameEvent event) {
        inc("enemiesKilled", +1);
        inc("score", scoreForKill());

        if (!playerControl.isLaserBeamActive() && getd("laserMeter") < LASER_METER_MAX) {
            inc("laserMeter", +LASER_METER_RECHARGE);
            if (getd("laserMeter") > LASER_METER_MAX) {
                set("laserMeter", LASER_METER_MAX);
            }
        }

        if (FXGLMath.randomBoolean(BONUS_SPAWN_CHANCE)) {
            spawnRandomBonus();
        }

        if (getCurrentLevel().isFinished())
            nextLevel();
    }

    @Handles(eventType = "ENEMY_REACHED_END")
    public void onEnemyReachedEnd(GameEvent event) {
        inc("enemiesKilled", +1);

        inc("lives", -1);
        uiController.loseLife();

        if (geti("lives") == 0)
            showGameOver();

        if (geti("enemiesKilled") == ENEMIES_PER_LEVEL)
            nextLevel();
    }

    @Handles(eventType = "ANY")
    public void onBonusPickup(BonusPickupEvent event) {
        switch (event.getType()) {
            case ATTACK_RATE:
                playerControl.increaseAttackSpeed(PLAYER_BONUS_ATTACK_SPEED);
                break;
            case LIFE:
                getGameState().increment("lives", +1);
                uiController.addLife();
                break;
            case BOMB:
                killRandomEnemy();
                break;
        }
    }

    private void killRandomEnemy() {
        List<Entity> list = getGameWorld().getEntitiesByType(SpaceInvadersType.ENEMY);
        list.get(FXGLMath.random(0, list.size() - 1))
                .getComponent(EnemyControl.class)
                .die();
    }

    private void showGameOver() {
        getDisplay().showConfirmationBox("Demo Over. Play Again?", yes -> {
            if (yes) {
                getGameWorld().getEntitiesCopy().forEach(Entity::removeFromWorld);
                startNewGame();
            } else {

                int score = getGameState().getInt("score");

                if (score > highScore) {
                    getDisplay().showInputBox("High Score! Enter your name", playerName -> {

                        // we have to use file system directly, since we are running without menus
                        FS.writeDataTask(new SaveData(playerName, score), SAVE_DATA_NAME).run();

                        exit();
                    });
                } else {
                    exit();
                }
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
