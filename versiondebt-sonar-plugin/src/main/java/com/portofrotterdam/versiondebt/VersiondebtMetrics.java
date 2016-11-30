package com.portofrotterdam.versiondebt;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class VersiondebtMetrics implements Metrics {

	public static final Metric<Integer> DEPENDENCY_AMOUNT = new Metric.Builder("dependency_amount", "Amount of outdated dependencies", Metric.ValueType.INT)
			.setDescription("Amount of outdated dependencies")
			.setBestValue(0.0)
			.setDomain(CoreMetrics.DOMAIN_MAINTAINABILITY)
			.setDirection(Metric.DIRECTION_WORST)
			.create();

    public static final Metric<Integer> TOTAL_DEPENDENCY_DEBT_DAYS = new Metric.Builder("total_dependency_debt", "Total dependency debt (in days)", Metric.ValueType
            .INT)
            .setBestValue(0.0)
            .setDescription("Total dependency debt (in days)")
            .setDomain(CoreMetrics.DOMAIN_MAINTAINABILITY)
            .setDirection(Metric.DIRECTION_WORST)
            .create();

	public static final Metric<String> TOTAL_DEPENDENCY_DEBT_STRING = new Metric.Builder("total_dependency_debt_string", "Total dependency debt", Metric
            .ValueType.STRING)
            .setHidden(true)
			.setDescription("Total dependency debt")
			.setDomain(CoreMetrics.DOMAIN_MAINTAINABILITY)
			.create();

	@Override
	public List<Metric> getMetrics() {
		return new ArrayList<Metric>(asList(DEPENDENCY_AMOUNT, TOTAL_DEPENDENCY_DEBT_DAYS, TOTAL_DEPENDENCY_DEBT_STRING));
	}
}
