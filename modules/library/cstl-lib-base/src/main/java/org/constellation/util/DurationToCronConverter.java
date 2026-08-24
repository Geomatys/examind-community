
package org.constellation.util;

import java.time.Duration;

/**
 *
 * @author glegal
 */
public class DurationToCronConverter {

    public static String getCronExpression(String durationStr) {
        
        Duration interval = Duration.parse(durationStr);
        long seconds = interval.getSeconds();

        if (seconds <= 0) {
            throw new IllegalArgumentException("Polling interval must be positive");
        }

        // Less than a minute: express as a "every N seconds" cron
        if (seconds < 60) {
            return String.format("*/%d * * * * ?", seconds);
        }

        long minutes = seconds / 60;
        if (seconds % 60 != 0) {
            throw new IllegalArgumentException("Interval is not aligned on a minute boundary: " + interval);
        }

        // Less than an hour: express as a "every N minutes" cron
        if (minutes < 60) {
            return String.format("0 */%d * * * ?", minutes);
        }

        long hours = minutes / 60;
        if (minutes % 60 != 0) {
            throw new IllegalArgumentException("Interval is not aligned on an hour boundary: " + interval);
        }

        // Express as a "every N hours" cron
        return String.format("0 0 */%d * * ?", hours);
    }
}
