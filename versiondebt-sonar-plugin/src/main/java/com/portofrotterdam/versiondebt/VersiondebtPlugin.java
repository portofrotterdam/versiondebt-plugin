package com.portofrotterdam.versiondebt;

import org.sonar.api.CoreProperties;
import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

public class VersiondebtPlugin implements Plugin {

	public static final String VERSIONDEBT_REPORT_PATH = "versiondebt.reportPath";

	@Override
	public void define(Context context) {
		context.addExtensions(
				VersiondebtSensor.class,
				VersiondebtMetrics.class,
				VersiondebtDashboardWidget.class,
				PropertyDefinition.builder(VERSIONDEBT_REPORT_PATH)
						.category(CoreProperties.CATEGORY_JAVA)
						.subCategory("Versiondebt")
						.name("Report path")
						.description("Path (absolute or relative) to versiondebt xml file.")
						.defaultValue("target/versiondebt.xml")
						.onQualifiers(Qualifiers.PROJECT)
						.build()
		);
	}
}
