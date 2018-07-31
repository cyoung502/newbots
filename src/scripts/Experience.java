package scripts;

import org.powerbot.script.AbstractScript;
import org.powerbot.script.rt4.ClientContext;

/**
 * <h1>Experience</h1>
 * <p>
 * The Experience class extends AbstractScript and is used to get experience values
 * that are useful for progress reports.
 *
 * @author Scout <scoutbots@tuta.io>
 * @version 1.0
 * @since 1.0
 *
 */
public class Experience extends AbstractScript<ClientContext> {

    /**
     * Array to hold all the skills based on the enum Constants.SKILLS_XXXXX, so
     * all indexes are based off those constant int values.
     */
    private int xpStart[] = new int[23];


    /**
     * Default constructor for the Experience class.
     * Only initialize this object once you are logged in.
     * Will fill the xpStart[] array.
     */
    public Experience() {

        for (int i = 0; i < xpStart.length; i++) {
            xpStart[i] = ctx.skills.experience(i);
        }
    }

    /**
     * This method will get the start experience based on the index of the
     * xpStart[] array.
     *
     * @param skill The input skill, use ie: Constants.SKILLS_ATTACK.
     * @return int This returns the start experience for the input skill.
     */
    public int getStartExperience(int skill) {

        return xpStart[skill];
    }

    /**
     * This method sets the start experience in the xpStart[] array.
     * This is useful if you need to reset it at any point during runtime.
     *
     * @param skill The input skill, use ie: Constants.SKILLS_ATTACK.
     */
    public void setStartExperience(int skill) {

        xpStart[skill] = ctx.skills.experience(skill);
    }

    /**
     * This method returns the experience gained by getting the current
     * experience and subtracting it from the xpStart[] index.
     *
     * @param skill The input skill, use ie: Constants.SKILLS_ATTACK.
     * @return int The experience gained since start for input skill.
     */
    public int getExperienceGained(int skill) {

        return ctx.skills.experience(skill) - xpStart[skill];
    }

    /**
     * This method will calculate the experience per hour by the following calculation:
     * <p>
     * xpGained * 3,600,000 / timeRunning
     *
     * @param skill The input skill, use ie: Constants.SKILLS_ATTACK.
     * @return int The experience rate per hour.
     */
    public int getExperienceHour(int skill) {

        return (int) ((getExperienceGained(skill) * 3600000D) / getRuntime());
    }

    /**
     * This method calculates the percentage to next level.
     *
     * @param skill The input skill, use ie: Constants.SKILLS_ATTACK.
     * @return int The current perecentage to the next level.
     */
    public int getExperiencePercent(int skill) {

        double numerator = ctx.skills.experience(skill) -
                ctx.skills.experienceAt(ctx.skills.realLevel(skill));
        double denominator = ctx.skills.experienceAt(ctx.skills.realLevel(skill) + 1) -
                ctx.skills.experienceAt(ctx.skills.realLevel(skill));
        return (int) ((numerator / denominator) * 100);
    }

    /**
     * This method calculates the time to level.
     *
     * @param skill The input skill, use ie: Constants.SKILLS_ATTACK.
     * @return long The time to level in milliseconds.
     */
    public long getTimeToLevel(int skill) {

        double xpToLevel = ctx.skills.experienceAt(ctx.skills.realLevel(skill) + 1) - ctx.skills.experience(skill);
        double xpHour = getExperienceHour(skill);
        if (xpHour == 0) {
            return 0;
        }
        return (long) ((xpToLevel / xpHour) * 3600000D);
    }
}