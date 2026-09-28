package ru.rooyzee.elytrixitem.util;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import ru.rooyzee.elytrixitem.Main;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Ускорение игрока на элитре "фейерверком".
 *
 * Поведение похоже на ванильную ракету: рывок в сторону взгляда при запуске,
 * затем непрерывная тяга вдоль направления полёта ракеты со искристым следом.
 * Отличия от ванили: тяга слабее и короче, плюс её можно слегка подруливать взглядом.
 */
public final class ElytraBoostUtil {

    /** Сколько тиков ракета тянет игрока (у ванили ~30 тиков). */
    private static final int BOOST_TICKS = 24;

    /** Сила стартового рывка при запуске. */
    private static final double START_KICK = 0.5;

    /** Тяга в первый тик (у ванили заметно выше). */
    private static final double START_THRUST = 0.13;

    /** Тяга в последний тик (линейно затухает от начальной). */
    private static final double END_THRUST = 0.04;

    /** Насколько тяга следует за взглядом игрока (0 — строго по направлению запуска, как в ваниле). */
    private static final double STEER = 0.35;

    private ElytraBoostUtil() {
    }

    public static void boost(Main plugin, Player player) {
        // Ракета "запускается" в направлении взгляда и дальше летит по своей траектории
        Vector launchDirection = player.getLocation().getDirection().normalize();

        Vector kick = launchDirection.clone().multiply(START_KICK).add(player.getVelocity().multiply(0.3D));
        player.setVelocity(kick);

        new BukkitRunnable() {
            private int ticksLeft = BOOST_TICKS;

            @Override
            public void run() {
                if (ticksLeft-- <= 0 || !player.isOnline() || !player.isGliding()) {
                    cancel();
                    return;
                }

                double progress = 1.0D - (ticksLeft / (double) BOOST_TICKS);
                double thrust = START_THRUST + (END_THRUST - START_THRUST) * progress;

                // В основном тянем вдоль траектории ракеты, чуть-чуть подруливаем взглядом
                Vector direction = launchDirection.clone()
                        .multiply(1.0D - STEER)
                        .add(player.getLocation().getDirection().normalize().multiply(STEER))
                        .normalize();

                ThreadLocalRandom random = ThreadLocalRandom.current();
                Vector wobble = new Vector(
                        random.nextDouble(-0.02D, 0.02D),
                        random.nextDouble(-0.02D, 0.02D),
                        random.nextDouble(-0.02D, 0.02D)
                );

                Vector velocity = player.getVelocity().multiply(0.99D).add(direction.multiply(thrust)).add(wobble);
                player.setVelocity(velocity);

                // Искристый след позади игрока, как у летящей ракеты
                org.bukkit.Location trail = player.getLocation().clone()
                        .subtract(direction.clone().multiply(0.8D))
                        .add(new Vector(0.0D, 0.2D, 0.0D));

                player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, trail, 2, 0.05D, 0.05D, 0.05D, 0.01D);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
