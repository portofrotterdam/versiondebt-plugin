package com.portofrotterdam.versiondebt;

import org.sonar.api.web.AbstractRubyTemplate;
import org.sonar.api.web.Description;
import org.sonar.api.web.RubyRailsWidget;
import org.sonar.api.web.UserRole;


@UserRole(UserRole.USER)
@Description("Reports on outdated versions")
public class VersiondebtDashboardWidget extends AbstractRubyTemplate implements RubyRailsWidget {

	@Override
	public String getId() {
		return "versiondebt";
	}

	@Override
	public String getTitle() {
		return "Version Debt";
	}

	@Override
	protected String getTemplatePath() {
		return "/versiondebt_dashboard_widget.html.erb";
	}
}