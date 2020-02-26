import gameObjects.BulletObject;
import gameObjects.PickupObject;
import gameObjects.enemy.EnemyObject;
import gameObjects.PlayerObject;
import gameObjects.enemy.Fighter1;
import gameObjects.enemy.PodCharged;
import gameObjects.enemy.UfoEnemy;
import settings.WeaponTypes;
import settings.GlobalConst;
import util.CMath;
import util.Vector3f;

public class GameLogic {
    private Model world;
    private int frameCount = 0;

    public GameLogic(Model world) {
        this.world = world;
    }

    // This is the heart of the game , where the model takes in all the inputs ,decides the outcomes and then changes the model accordingly.
    void doLogic() {
        // Player 1 Logic first
        if (!world.getPlayer().isDying() && !world.getPlayer().isDead()) playerLogic();
        if (world.getP2() != null) {
            // Player 2 Logic
            if (!world.getP2().isDying() && !world.getP2().isDead()) p2Logic();
            // Game Over
            if (world.getP2().isDead() && world.getPlayer().isDead()) world.setGameEnd(true);
        } // Game Over
        else if (world.getPlayer().isDead()) world.setGameEnd(true);
        if (world.isGameEnd()) return;
        // Pickup objects Logic
        pickupLogic();
        // Enemy Logic next
        enemyLogic();
        enemyBulletLogic();
        // Bullets move next
        bulletLogic();
        // interactions between objects
        gameLogic();
        // Game process
        gameProcess();
        frameCount += 1;
        // Survival to get score
        world.addScore();
    }

    private void gameLogic() {
        // this is a way to increment across the array list data structure
        //see if they hit anything
        // using enhanced for-loop style as it makes it alot easier both code wise and reading wise too
        for (EnemyObject temp : world.getEnemies()) {
            for (BulletObject bullet : world.getBullets()) {
                if (Math.abs(temp.getCentre().getX() - bullet.getCentre().getX()) < temp.getWidth() / 2f
                        && Math.abs(temp.getCentre().getY() - bullet.getCentre().getY()) < temp.getHeight() / 2f) {
                    // Damage part
                    world.setLastContact(temp);
                    temp.reduceHealth(bullet.bulletDamage);
                    world.getBullets().remove(bullet);
                }
            }
            // Player 1 Laser logic
            laserLogic(world.getPlayer(), temp);
            // Player 2 Laser
            if (world.getP2() != null) laserLogic(world.getP2(), temp);
            // Enemies are dead or not
            if (temp.isDead()) {
                world.getEnemies().remove(temp);
                world.addScore(temp);
                world.getPickupList().add(PickupObject.createPickup(temp.getNewCentre()));
            }
        }
    }
    /** This method is for game Logic*/
    private void laserLogic(PlayerObject player, EnemyObject enemy) {
        if (player.laser.isOn) {
            float height = player.laser.getCentre().getY() - enemy.getCentre().getY();
            if (Math.abs(enemy.getCentre().getX() - player.laser.getCentre().getX()) < enemy.getWidth() / 2f
                    && height < player.laser.getHeight() && height > 0) {
                // Set the contact object for laser
                world.setLastContact(enemy);
                enemy.reduceHealth(player.laser.getDamage());
            }
        }
    }

    private void pickupLogic() {
        for (PickupObject pickup : world.getPickupList()) {
            // Pickup moves
            pickup.applyVector();
            if (pickup.isContact(world.getPlayer())) world.getPickupList().remove(pickup);
            else if (world.getP2() != null && pickup.isContact(world.getP2())) world.getPickupList().remove(pickup);
        }
        // Reduce shield time
        world.getPlayer().weakenShield();
        if (world.getP2() != null) world.getP2().weakenShield();
    }

    // How enemies move
    private void enemyLogic() {
        for (EnemyObject enemy : world.getEnemies()) {
            // Move enemies
            enemy.applyVector();
            // Fire to player
            enemy.fire(world.getEBulletList(), world.getP2() != null ? CMath.binaryRandom() ? world.getPlayer() : world.getP2() : world.getPlayer());
            // See if they get to the top of the screen ( remember 0 is the top
            if (enemy.isOnBoundary())  // current boundary need to pass value to model
                world.getEnemies().remove(enemy);
        }
    }

    // Enemy bullets
    private void enemyBulletLogic() {
        for (BulletObject eb : world.getEBulletList()) {
            eb.applyVector();
            // Boundary check
            if (eb.isOnBoundary()) world.getEBulletList().remove(eb);
            // Death detection
            deathDetection(world.getPlayer(), eb);
            if (world.getP2() != null) deathDetection(world.getP2(), eb);
        }
    }
    /**This method is for enemy Bullet logic*/
    private void deathDetection(PlayerObject player, BulletObject eb) {
        if (!player.isDying() && !player.isDead()) {
            float width = Math.abs(player.getCentre().getX() - eb.getCentre().getX());
            float height = Math.abs(player.getCentre().getY() - eb.getCentre().getY());
            // Shield immune enemy's bullets
            if (player.isShield()) {
                // Remove bullets
                if (width < player.getWidth() / 1.5f && height < player.getHeight() / 1.5f)
                    world.getEBulletList().remove(eb);
            } else if (width < eb.getWidth() / 2f && height < eb.getHeight() / 2f) {
                // Then die, if there is no shield. Dying state will not remove bullets
                world.getEBulletList().remove(eb);
                player.laser.isOn = false;
                // Set death time
                player.dyingTime = 167;
            }
        }
    }

    // Player bullets
    private void bulletLogic() {
        // move bullets
        for (BulletObject temp : world.getBullets()) {
            //check to move them
            temp.applyVector();
            //see if they hit anything
            //see if they get to the top of the screen ( remember 0 is the top
            if (temp.isOnBoundary()) {
                world.getBullets().remove(temp);
            }
        }
        // Reduce cool down time
        if (world.getPlayer().bulletClodDown > 0) world.getPlayer().bulletClodDown--;
        if (world.getP2() != null && world.getP2().bulletClodDown > 0) world.getP2().bulletClodDown--;
    }

    private void playerLogic() {
        PlayerObject player = world.getPlayer();
        // Weapon switch logic
        if (Controller.getInstance().isKeyRPressed()) {
            if (player.currentWeapon == WeaponTypes.Laser)
                player.currentWeapon = WeaponTypes.Bullet;
            else player.currentWeapon = WeaponTypes.Laser;
            Controller.getInstance().setKeyRPressed(false);
            player.laser.isOn = false;
        }
        // smoother animation is possible if we make a target position  // done but may try to change things for students
        // check for movement and if you fired a bullet
        if (Controller.getInstance().isKeyAPressed()) {
            player.getCentre().ApplyVector(GlobalConst.sFighterAMov);
        }
        if (Controller.getInstance().isKeyDPressed()) {
            player.getCentre().ApplyVector(GlobalConst.sFighterDMov);
        }
        if (Controller.getInstance().isKeyWPressed()) {
            player.getCentre().ApplyVector(GlobalConst.sFighterWMov);
        }
        if (Controller.getInstance().isKeySPressed()) {
            player.getCentre().ApplyVector(GlobalConst.sFighterSMov);
        }
        // Whether laser is on
        if (Controller.getInstance().isKeySpacePressed()) {
            // If current weapon is laser
            if (player.currentWeapon == WeaponTypes.Laser) player.laser.isOn = true;
                // Bullet part
            else {
                player.createBullet(world.getBullets(), world.bulletResource);
//                Controller.getInstance().setKeySpacePressed(false);
            }
        } else {
            if (player.currentWeapon == WeaponTypes.Laser) player.laser.isOn = false;
        }
    }
    private void p2Logic() {
        PlayerObject player = world.getP2();
        MouseControl control = MouseControl.getInstance();
        control.poll();
        // Movement
        double width = control.getPosition().getX() - player.getCentre().getX();
        double height = control.getPosition().getY() - player.getCentre().getY();
        double x = width > 0 ? width < GlobalConst.sFighterDMov.getX() ? width : GlobalConst.sFighterDMov.getX()
                : width > GlobalConst.sFighterAMov.getX() ? width : GlobalConst.sFighterAMov.getX();
        double y = height > 0 ? height < GlobalConst.sFighterWMov.getY() ? height : GlobalConst.sFighterWMov.getY()
                : height > GlobalConst.sFighterSMov.getY() ? height : GlobalConst.sFighterSMov.getY();
        // Make movement normal
        if (Math.abs(height) > Math.abs(width) && Math.abs(height) != 0) {
            x = x * Math.abs(width / height);
        }
        if (Math.abs(height) <= Math.abs(width) && Math.abs(width) != 0) {
            y = y * Math.abs(height / width);
        }
        player.getCentre().ApplyVector(new Vector3f((float) x, (float) -y, 0));
        // Change weapon
        if (control.buttonDownOnce(3)) {
            if (player.currentWeapon == WeaponTypes.Laser)
                player.currentWeapon = WeaponTypes.Bullet;
            else player.currentWeapon = WeaponTypes.Laser;
            player.laser.isOn = false;
        }
        // Fire
        if(control.buttonDown(1)) {
            // If current weapon is laser
            if (player.currentWeapon == WeaponTypes.Laser) player.laser.isOn = true;
                // Bullet part
            else player.createBullet(world.getBullets(), world.bulletResource);
        } else {
            if (player.currentWeapon == WeaponTypes.Laser) player.laser.isOn = false;
        }
    }

    private void gameProcess() {
        int intensity = frameCount / CMath.getFrames(300) + 1;
        int cuFrame = frameCount % CMath.getFrames(300);
        if (CMath.timeTrigger(cuFrame, 0, 10, 1)) {
            world.getEnemies().add(new UfoEnemy(intensity));
        }
        if (CMath.timeTrigger(cuFrame, 12)) {
            world.getEnemies().add(new PodCharged(intensity));
        }
        if (CMath.timeTrigger(cuFrame, 20, 30, 1)) {
            world.getEnemies().add(new UfoEnemy(intensity));
        }
        if (CMath.timeTrigger(cuFrame, 30)) {
            world.getEnemies().add(new PodCharged(intensity));
        }
        if (CMath.timeTrigger(cuFrame, 40)) {
            world.getEnemies().add(new PodCharged(intensity));
        }
        if (CMath.timeTrigger(cuFrame, 35, 50, 1)) {
            world.getEnemies().add(new UfoEnemy(intensity));
        }
        if (CMath.timeTrigger(cuFrame, 55)) {
            world.getEnemies().add(new PodCharged(intensity));
        }
        if (CMath.timeTrigger(cuFrame, 60, 80, 2)) {
            world.getEnemies().add(new Fighter1(intensity));
        }

    }
}
