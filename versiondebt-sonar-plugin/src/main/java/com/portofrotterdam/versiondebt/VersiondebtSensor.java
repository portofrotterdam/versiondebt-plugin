package com.portofrotterdam.versiondebt;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Date;

public class VersiondebtSensor implements Sensor {

	private static final Logger LOGGER = Loggers.get(VersiondebtSensor.class);
	private Settings settings;
	private PathResolver pathResolver;
    private FileSystem fileSystem;

	public VersiondebtSensor(final Settings settings, final PathResolver pathResolver, final FileSystem fileSystem) {
		this.settings = settings;
		this.pathResolver = pathResolver;
        this.fileSystem = fileSystem;
	}

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.name("versiondebt sensor");
    }

    @Override
    public void execute(SensorContext context) {
    	String path = settings.getString(VersiondebtPlugin.VERSIONDEBT_REPORT_PATH);
		File report = pathResolver.relativeFile(fileSystem.baseDir(), path);
		if (report.exists() && report.isFile()) {
            LOGGER.info("Versiondebt report found {}", report);
            parseVersiondebtXML(report, context);
		} else {
            LOGGER.warn("Versiondebt report not found at {}", report);
        }
	}

	private void parseVersiondebtXML(final File file, final SensorContext context) {
		try {
			final Versiondebts versiondebts = VersiondebtsFactory.newInstance().fromXML(new FileInputStream(file));
            LOGGER.info("Amount of non-up to date versions '{}'", versiondebts.getVersiondebtItems().size());
            context.<Integer>newMeasure()
                    .on(context.module())
                    .forMetric(VersiondebtMetrics.DEPENDENCY_AMOUNT)
                    .withValue(getOutdatedVersionCount(versiondebts))
                    .save();

            final long duration = calculateDuration(versiondebts);
            final long days = (duration / (1000*60*60*24));
			final String durationDateString = PrettyFormatter.formatMillisToYearsDaysHours(duration);
			LOGGER.info("Duration '{}'", durationDateString);
            context.<Integer>newMeasure()
                    .on(context.module())
                    .forMetric(VersiondebtMetrics.TOTAL_DEPENDENCY_DEBT_DAYS)
                    .withValue((int) days)
                    .save();

            context.<String>newMeasure()
                    .on(context.module())
                    .forMetric(VersiondebtMetrics.TOTAL_DEPENDENCY_DEBT_STRING)
                    .withValue(durationDateString)
                    .save();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private long calculateDuration(Versiondebts versiondebts) {
		long totalDuration = 0;
		for (Versiondebts.VersiondebtItem versiondebtItem : versiondebts.getVersiondebtItems()) {
			totalDuration += getOutdatedDuration(versiondebtItem);
		}
		return totalDuration;
	}

	private int getOutdatedVersionCount(final Versiondebts versiondebts) {
		int count = 0;
		for (Versiondebts.VersiondebtItem versiondebtItem : versiondebts.getVersiondebtItems()) {
			if (getOutdatedDuration(versiondebtItem) > 0) {
				++count;
			}
		}
		return count;
	}

	private long getOutdatedDuration(final Versiondebts.VersiondebtItem versiondebtItem) {
		Date usedVersionTimestamp = versiondebtItem.getUsedVersion().getTimestamp();
		Date latestVersionTimestamp = versiondebtItem.getLatestVersion().getTimestamp();
		if (usedVersionTimestamp == null || latestVersionTimestamp == null) {
			return 0;
		}
		long difference = latestVersionTimestamp.getTime() - usedVersionTimestamp.getTime();
		return difference <= 0 ? 0 : difference;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
