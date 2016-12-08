package com.portofrotterdam.versiondebt;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the following XML fragment:
 * <p/>
 * <pre>
 * 		<versiondebtItems>
 * 			<versiondebtItem groupId="org.hibernate" artifactId="hibernate-core">
 * 			<usedVersion>
 * 				<name>3.5.4-Final</name>
 * 				<timestamp>21-Jul-2010 22:44</timestamp>
 * 			</usedVersion>
 * 			<latestVersion>
 * 				<name>4.3.5-Final</name>
 * 		 		<timestamp>02-Apr-2014 14:52</timestamp>
 * 			</latestVersion>
 * 		</versiondebtItem>
 * </pre>
 */
@XStreamAlias("versiondebtItems")
public class Versiondebts implements Serializable {

	@XStreamImplicit
	private final Set<VersiondebtItem> versiondebtItems = new HashSet<>();

	public void addVersiondebtItem(final VersiondebtItem item) {
	    versiondebtItems.add(item);
	}

	public void addAll(List<VersiondebtItem> items) {
		versiondebtItems.addAll(items);
	}

	public List<VersiondebtItem> getVersiondebtItems() {
		if (versiondebtItems != null) {
			return Collections.unmodifiableList(new ArrayList<>(versiondebtItems));
		} else {
			return Collections.emptyList();
		}
	}

	@XStreamAlias("versiondebtItem")
	public static class VersiondebtItem {

		@XStreamAsAttribute
		private String groupId;

		@XStreamAsAttribute
		private String artifactId;

		public VersiondebtItem() {
		}

		public VersiondebtItem(final String groupId, final String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

		private Version usedVersion;

		private Version latestVersion;

		public String getGroupId() {
			return groupId;
		}

		public String getArtifactId() {
			return artifactId;
		}

        public void setUsedVersion(final Version usedVersion) {
            this.usedVersion = usedVersion;
        }

		public Version getUsedVersion() {
			return usedVersion;
		}

        public void setLatestVersion(final Version latestVersion) {
            this.latestVersion = latestVersion;
        }

        public Version getLatestVersion() {
			return latestVersion;
		}

		@XStreamAlias("version")
		public static class Version {

			private String name;

			private Date timestamp;

			public Version() {
			}

			public Version(final String name, final Date timestamp) {
                this.name = name;
                this.timestamp = timestamp;
            }

			public String getName() {
				return name;
			}

			public Date getTimestamp() {
				return timestamp;
			}
		}
	}
}
