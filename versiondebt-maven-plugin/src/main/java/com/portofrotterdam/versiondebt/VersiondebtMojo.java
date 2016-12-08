package com.portofrotterdam.versiondebt;

import com.portofrotterdam.versiondebt.Versiondebts.VersiondebtItem;
import com.portofrotterdam.versiondebt.Versiondebts.VersiondebtItem.Version;
import org.apache.http.client.utils.DateUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataResolutionException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * This is the Maven plugin.
 *
 * It creates a versiondebt.xml file for each submodule which contains all the version information.
 *
 * Part of this plugin is heavily based on:
 * https://github.com/mojohaus/cobertura-maven-plugin/blob/master/src/main/java/org/codehaus/mojo/cobertura/CoberturaReportMojo.java
 * This is used to create an aggregate file containing all the version information (which is used by the Sonar plugin).
 *
 * Possible improvements:
 * - Add caching? Checking the latest version is a bit slow and it might be possible to cache this for a day for example?
 *
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.COMPILE, requiresProject = true, threadSafe = true)
public class VersiondebtMojo extends AbstractMojo {

    /**
     * <p>
     * The Datafile Location.
     * </p>
     */
    @Parameter(defaultValue= "${project.build.directory}/versiondebt.xml")
    private File dataFile;

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}")
    private MavenProject currentProject;

    /**
     * List of maven project of the current build
     *
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    @Parameter(defaultValue = "${reactorProjects}")
    private List<MavenProject> reactorProjects;

    private Map<MavenProject, List<MavenProject>> projectChildren;

    private String relDataFileName;

    /**
     * Misc Maven components:
     */
    @Component
    protected RepositoryMetadataManager repositoryMetadataManager;

    @Parameter(defaultValue = "${localRepository}")
    protected ArtifactRepository localRepository;

    @Override
    public void execute() throws MojoExecutionException {

        // attempt to determine where data files and output dir are
        relDataFileName = relativize( currentProject.getBasedir(), dataFile);
        if ( relDataFileName == null )
        {
            getLog().warn( "Could not determine relative data file name, defaulting to 'versiondebt.xml'" );
            relDataFileName = "versiondebt.xml";
        }

        try {
            executeDependencyReport();
            if (canGenerateAggregateReports()) {
                // If we are executing the final project, write the aggregate:
                executeAggregateReport();
            }

        } catch (IOException ioe) {
            throw new MojoExecutionException("Error creating report ", ioe);
        }

    }

    private void writeVersiondebtReport(Versiondebts versiondebts, MavenProject project) throws IOException {

        final File outputFile = new File(project.getBasedir(), relDataFileName);
        verifyDirectory(outputFile);

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(outputFile);
            VersiondebtsFactory.newInstance().toXML(versiondebts, fileWriter);
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
        }
    }

    private void executeDependencyReport() throws IOException {

        final Versiondebts versiondebts = new Versiondebts();

        final List<String> repositoryUrls = getRepositoryUrls();

        final Log log = getLog();
        log.info("");
        log.info("------------------------------------------------------------------------");
        log.info("Analyzing dependency version debt:");
        log.info("------------------------------------------------------------------------");
        log.info("");

        long totalDebtTime = 0;
        long amountOfOutdatedDependencies = 0;

        @SuppressWarnings("unchecked")
        final Set<Artifact> artifacts = currentProject.getDependencyArtifacts();
        for (final Artifact artifact : artifacts) {

            final String currentVersion = artifact.getVersion();
            final Date currentVersionDate = extractLastModified(artifact, currentVersion, repositoryUrls);

            final String latestVersion = retrieveLatestReleasedVersion(artifact);
            final Date latestVersionDate = extractLastModified(artifact, latestVersion, repositoryUrls);

            log.info("--- Dependency:" + artifact.getGroupId() + " : " + artifact.getArtifactId()+" ---");
            log.info("");
            log.info("\tCurrent version: " + currentVersion+ "\t" + currentVersionDate);
            log.info("\tLatest version: " + latestVersion+ "\t" + latestVersionDate);
            log.info("");

            if (latestVersionDate != null && currentVersionDate != null) {
                final long artifactDebtTime = latestVersionDate.getTime() - currentVersionDate.getTime();
                if (artifactDebtTime > 0) {
                    amountOfOutdatedDependencies++;
                    totalDebtTime += artifactDebtTime;
                    log.info("\tArtifact debt: " + PrettyFormatter.formatMillisToYearsDaysHours(artifactDebtTime));
                    log.info("");
                } else {
                    log.info("\tArtifact debt: Congratulations! You are up to date!");
                    log.info("");
                }
            }

            versiondebts.addVersiondebtItem(generateVersiondebtItem(artifact, currentVersion, currentVersionDate, latestVersion, latestVersionDate));
        }

        long averageDebtTime = amountOfOutdatedDependencies > 0 ? totalDebtTime / amountOfOutdatedDependencies : 0;
        log.info("------------------------------------------------------------------------");
        log.info("AVERAGE DEBT PER DEP: " + PrettyFormatter.formatMillisToYearsDaysHours(averageDebtTime));
        log.info("TOTAL DEBT:           " + PrettyFormatter.formatMillisToYearsDaysHours(totalDebtTime));
        log.info("------------------------------------------------------------------------");

        log.info("");

        writeVersiondebtReport(versiondebts, currentProject);
    }

    private VersiondebtItem generateVersiondebtItem(final Artifact artifact, final String currentVersionName, final Date currentVersionDate,
                                                    final String latestVersionName, final Date latestVersionDate) {

        final VersiondebtItem versiondebt = new VersiondebtItem(artifact.getGroupId(), artifact.getArtifactId());
        versiondebt.setUsedVersion(new Version(currentVersionName, currentVersionDate));
        versiondebt.setLatestVersion(new Version(latestVersionName, latestVersionDate));

        return versiondebt;
    }

    /**
     * Method for combining all know URLs to get the best match/latest version
     */
    private List<String> getRepositoryUrls() {
        final List<String> repositoryUrls = new ArrayList<>();
        // Combine all possible repository urls:
        @SuppressWarnings("unchecked")
        final List<ArtifactRepository> artifactRepositories = currentProject.getRemoteArtifactRepositories();
        for (final ArtifactRepository repository : artifactRepositories) {
            repositoryUrls.add(repository.getUrl());
        }
        return repositoryUrls;
    }

    private Date extractLastModified(final Artifact artifact, final String version, final List<String> repositoryUrls) throws IOException {
        for (final String repositoryUrl : repositoryUrls) {
            final URL url = new URL(generateUrl(artifact, version, repositoryUrl));
            final URLConnection connection = url.openConnection();
            String lastModified = connection.getHeaderField("Last-Modified");
            if (lastModified != null) {
                // Once found, return.
                return DateUtils.parseDate(lastModified);
            }
        }
        return null;
    }

    private String generateUrl(final Artifact artifact, final String version, final String repositoryUrl) {

        // Build the repository URL to check the Last-Modified header:
        String groupIdPart = artifact.getGroupId().replace(".", "/");
        String artifactIdPart = artifact.getArtifactId().replace(".", "/");

        return repositoryUrl + "/" + groupIdPart + "/" + artifactIdPart + "/" + version
                + "/" + artifact.getArtifactId() + "-" + version + ".pom";
    }

    private String retrieveLatestReleasedVersion(final Artifact artifact) {
        final RepositoryMetadata metadata = new ArtifactRepositoryMetadata(artifact);
        try {
            repositoryMetadataManager.resolve(metadata, currentProject.getRemoteArtifactRepositories(), localRepository);
        } catch (final RepositoryMetadataResolutionException e1) {
            e1.printStackTrace();
        }
        final Metadata repoMetadata = metadata.getMetadata();
        if (repoMetadata.getVersioning() != null) {
            final String releasedVersion = repoMetadata.getVersioning().getRelease();
            if (releasedVersion != null) {
                return releasedVersion;
            }
            final String latestVersion = repoMetadata.getVersioning().getLatest();
            if (latestVersion != null) {
                return latestVersion;
            }
        }

        return repoMetadata.getVersion();
    }

    /**
     * -------------------------------------------------------------------------------------------
     * Stuff below is heavily based on:
     * https://github.com/mojohaus/cobertura-maven-plugin/blob/master/src/main/java/org/codehaus/mojo/cobertura/CoberturaReportMojo.java
     *
     * Needed to generate an aggregate report.
     * -------------------------------------------------------------------------------------------
     */

    /**
     * Generates an aggregate report for the given project.
     */
    private void executeAggregateReport(MavenProject topProject) throws IOException {
        Versiondebts aggregateVersiondebts = new Versiondebts();

        List<MavenProject> children = getAllChildren( topProject );

        if ( children.isEmpty() )
        {
            return;
        }

        List<File> partialReportFiles = getOutputFiles(children);
        if(partialReportFiles.isEmpty()) {
            getLog().info("No reports found");
            return;
        }

        getLog().info( "Executing aggregate reports for " + topProject.getName());

        for ( File partialReportFile : partialReportFiles ) {
            try {
                // Merge all the versiondebts:
                getLog().info("Collecting report: "+partialReportFile.toString());

                Versiondebts partialDebts = VersiondebtsFactory.newInstance().fromXML(Files.newInputStream(partialReportFile.toPath()));
                aggregateVersiondebts.addAll(partialDebts.getVersiondebtItems());
            } catch (IOException ioe) {
                getLog().error(ioe);
            }
        }

        writeVersiondebtReport(aggregateVersiondebts, topProject);
    }


    /**
     * Returns a list containing all the recursive, non-pom children of the given project, never <code>null</code>.
     */
    private List<MavenProject> getAllChildren( MavenProject parentProject )
    {
        List<MavenProject> children = projectChildren.get( parentProject );
        if(children == null) {
            return Collections.emptyList();
        }

        List<MavenProject> result = new ArrayList<>();
        for(MavenProject child : children) {
            if(isMultiModule(child)) {
                result.addAll( getAllChildren( child ) );
            } else {
                result.add( child );
            }
        }
        return result;
    }

    /**
     * Generates aggregate reports for all multi-module projects.
     */
    private void executeAggregateReport() throws IOException {
        // Find the top project and create a report for that project:
        for ( MavenProject proj : reactorProjects) {
            if(!isMultiModule(proj)) {
                continue;
            }
            executeAggregateReport(proj);
        }
    }

    /**
     * Returns whether or not we can generate any aggregate reports at this time.
     */
    private boolean canGenerateAggregateReports()
    {
        // we only generate aggregate reports after the last project runs
        if(isLastProject(currentProject, reactorProjects)) {
            buildAggregateInfo();
            if (!getOutputFiles(reactorProjects).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether the element is the last element of the list
     *
     * @param project          element to check
     * @param mavenProjectList list of maven project
     * @return true if project is the last element of mavenProjectList  list
     */
    private boolean isLastProject( MavenProject project, List<MavenProject> mavenProjectList )
    {
        return project.equals( mavenProjectList.get( mavenProjectList.size() - 1 ) );
    }

    /**
     * Generates various information needed for building aggregate reports.
     */
    private void buildAggregateInfo()
    {
        if(projectChildren != null) {
            return; // already did this work
        }

        // build parent-child map
        projectChildren = new HashMap<>();
        for(MavenProject proj : reactorProjects) {
            List<MavenProject> depList = projectChildren.get(proj.getParent());
            if(depList == null) {
                depList = new ArrayList<>();
                projectChildren.put( proj.getParent(), depList);
            }
            depList.add(proj);
        }
    }

    /**
     * Returns any existing partial reports from the given list of projects.
     */
    private List<File> getOutputFiles( List<MavenProject> projects )
    {
        List<File> files = new ArrayList<>();
        for ( MavenProject proj : projects )
        {
            if ( isMultiModule( proj ) )
            {
                continue;
            }
            File outputFile = new File(proj.getBasedir(), relDataFileName);
            if ( outputFile.exists() )
            {
                files.add( outputFile );
            }
        }
        return files;
    }

    /**
     * Test if the project has pom packaging
     *
     * @param mavenProject Project to test
     * @return True if it has a pom packaging
     */
    private boolean isMultiModule( MavenProject mavenProject )
    {
        return "pom".equals( mavenProject.getPackaging() );
    }


    /**
     * Attempts to make the given childFile relative to the given parentFile.
     */
    private String relativize( File parentFile, File childFile )
    {
        try {
            URI parentURI = parentFile.getCanonicalFile().toURI().normalize();
            URI childURI = childFile.getCanonicalFile().toURI().normalize();

            URI relativeURI = parentURI.relativize( childURI );
            if ( relativeURI.isAbsolute() ) {
                // child is not relative to parent
                return null;
            }
            String relativePath = relativeURI.getPath();
            if ( File.separatorChar != '/' ) {
                relativePath = relativePath.replace( '/', File.separatorChar );
            }
            return relativePath;
        }
        catch ( Exception e ) {
            getLog().warn( "Failed relativizing " + childFile + " to " + parentFile, e );
        }
        return null;
    }

    private void verifyDirectory(final File directory) {

        File currentTarget = directory;
        // Move to top directory, not file:
        currentTarget = currentTarget.getParentFile();

        if(!currentTarget.exists()) {
            currentTarget.mkdirs();
        }
    }
}
