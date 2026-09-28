package ru.rooyzee.elytrixitem.util;

public final class ExperienceUtil {

    private ExperienceUtil() {
    }

    public static int getTotalExperienceForLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        }

        if (level <= 31) {
            return (int) Math.floor(2.5D * level * level - 40.5D * level + 360.0D);
        }

        return (int) Math.floor(4.5D * level * level - 162.5D * level + 2220.0D);
    }

    public static int getLevelFromTotalExperience(int totalExperience) {
        int level = 0;

        while (getTotalExperienceForLevel(level + 1) <= totalExperience) {
            level++;
        }

        return level;
    }
}