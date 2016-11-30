package com.portofrotterdam.versiondebt;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class PrettyFormatterTest {

    @Test
    public void testMoreThanADay() {

        long millis = TimeUnit.DAYS.toMillis(365);
        Assert.assertEquals("365 days", PrettyFormatter.formatMillisToYearsDaysHours(millis));

        millis = TimeUnit.DAYS.toMillis(366);
        Assert.assertEquals("1 year, 1 day", PrettyFormatter.formatMillisToYearsDaysHours(millis));

        millis = TimeUnit.DAYS.toMillis(366) + TimeUnit.HOURS.toMillis(5);
        Assert.assertEquals("1 year, 1 day, 5 hours", PrettyFormatter.formatMillisToYearsDaysHours(millis));
    }

    @Test
    public void testLessThanAnHour() {

        long millis = TimeUnit.MINUTES.toMillis(1);
        Assert.assertEquals("1 minute", PrettyFormatter.formatMillisToYearsDaysHours(millis));

        millis += TimeUnit.MINUTES.toMillis(1);
        Assert.assertEquals("2 minutes", PrettyFormatter.formatMillisToYearsDaysHours(millis));

        millis += TimeUnit.SECONDS.toMillis(1);
        Assert.assertEquals("2 minutes and 1 second", PrettyFormatter.formatMillisToYearsDaysHours(millis));

        millis += TimeUnit.SECONDS.toMillis(1);
        Assert.assertEquals("2 minutes and 2 seconds", PrettyFormatter.formatMillisToYearsDaysHours(millis));
    }
}
