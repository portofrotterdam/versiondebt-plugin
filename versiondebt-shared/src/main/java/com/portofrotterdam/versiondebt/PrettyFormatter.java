package com.portofrotterdam.versiondebt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class PrettyFormatter {

    /**
     * Don't ask... there is probably a better way to do this, but this works (for now).
     *
     * @param millis
     * @return
     */
    public static String formatMillisToYearsDaysHours(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);

        final long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);

        long years = 0;
        while(days>365) {
            days -= 365;
            years++;
        }

        List<String> parts = new ArrayList<>();
        parts.add((years>0)?(years>1)?years+" years":years+" year":null);
        parts.add((days>0)?(days>1)?days+" days":days+" day":null);
        parts.add((hours>0)?(hours>1)?hours+" hours":hours+" hour":null);

        final String formatted = parts.stream().filter(s -> s!=null).collect(Collectors.joining(", "));

        if(formatted.length() == 0) {
            //Less than a day?
            if(millis > 0) {

                final long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
                millis -= TimeUnit.MINUTES.toMillis(minutes);

                final long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

                final String minutesPart = (minutes>0)?(minutes>1)?minutes+" minutes":minutes+" minute":"";
                final String secondsPart = (seconds>0)?(seconds>1)?" and "+seconds+" seconds":" and "+seconds+" second":"";
                return minutesPart + secondsPart;
            }
            return "-";
        }
        return formatted;
    }
}
