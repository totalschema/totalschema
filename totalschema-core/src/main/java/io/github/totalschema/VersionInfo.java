package io.github.totalschema;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provides access to version and git information for the TotalSchema application. This information
 * is populated during the Maven build process.
 */
public class VersionInfo {

    private static final String VERSION_PROPERTIES = "/io/github/totalschema/version.properties";
    private static final String GIT_PROPERTIES = "/io/github/totalschema/git-info.properties";

    private static final Properties versionProps = new Properties();
    private static final Properties gitProps = new Properties();
    private static final boolean loaded;

    static {
        boolean success = true;
        try {
            loadProperties(VERSION_PROPERTIES, versionProps);
            loadProperties(GIT_PROPERTIES, gitProps);
        } catch (IOException e) {
            success = false;
            System.err.println("Warning: Could not load version information: " + e.getMessage());
        }
        loaded = success;
    }

    private static void loadProperties(String resourcePath, Properties props) throws IOException {
        try (InputStream is = VersionInfo.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            props.load(is);
        }
    }

    /**
     * Gets the project version.
     *
     * @return the project version, or "unknown" if not available
     */
    public static String getVersion() {
        return versionProps.getProperty("project.version", "unknown");
    }

    /**
     * Gets the git commit ID (full SHA).
     *
     * @return the git commit ID, or "unknown" if not available
     */
    public static String getCommitId() {
        return gitProps.getProperty("git.commit.id", "unknown");
    }

    /**
     * Gets the abbreviated git commit ID (short SHA).
     *
     * @return the abbreviated commit ID, or "unknown" if not available
     */
    public static String getCommitIdAbbrev() {
        return gitProps.getProperty("git.commit.id.abbrev", "unknown");
    }

    /**
     * Gets the git branch name.
     *
     * @return the branch name, or "unknown" if not available
     */
    public static String getBranch() {
        return gitProps.getProperty("git.branch", "unknown");
    }

    /**
     * Gets the build time.
     *
     * @return the build time, or "unknown" if not available
     */
    public static String getBuildTime() {
        return gitProps.getProperty("git.build.time", "unknown");
    }

    /**
     * Checks if version information was successfully loaded.
     *
     * @return true if version information is available, false otherwise
     */
    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * Gets a formatted version string including git information.
     *
     * @return formatted version string
     */
    public static String getFullVersionInfo() {
        return String.format(
                "Version: %s%nBranch: %s%nCommit: %s%nBuild Time: %s",
                getVersion(), getBranch(), getCommitIdAbbrev(), getBuildTime());
    }

    /**
     * Gets a short version string suitable for display.
     *
     * @return short version string (e.g., "1.0-SNAPSHOT (76074a5)")
     */
    public static String getShortVersionInfo() {
        return String.format("%s (%s)", getVersion(), getCommitIdAbbrev());
    }

    private VersionInfo() {
        // Utility class, prevent instantiation
    }
}
