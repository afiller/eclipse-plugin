package com.vaadin.integration.eclipse.util;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.vaadin.integration.eclipse.VaadinPlugin;

public class PreferenceUtil {

    private ScopedPreferenceStore prefStore;

    private PreferenceUtil(IProject project) {
        prefStore = new ScopedPreferenceStore(new ProjectScope(project),
                VaadinPlugin.PLUGIN_ID);
    }

    // preference store keys

    public static PreferenceUtil get(IProject project) {
        return new PreferenceUtil(project);
    }

    // "true"/"false"/missing - if missing, check if >1 widgetset exists
    private static final String PREFERENCES_WIDGETSET_DIRTY = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetDirty";
    // true to suspend automatic widgetset build requests for the project
    private static final String PREFERENCES_WIDGETSET_SUSPENDED = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetBuildsSuspended";
    // "OBF"/"PRETTY"/"DETAILED" or missing (default to "OBF")
    private static final String PREFERENCES_WIDGETSET_STYLE = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetStyle";
    // a number of threads to use (-localWorkers) or missing
    private static final String PREFERENCES_WIDGETSET_PARALLELISM = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetParallelism";

    // the time last compilation lasted, used for estimation in progress monitor
    private static final String PREFERENCES_WIDGETSET_COMPILATION_ETA = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetCompilationEta";

    // to output compilation messages to console or not
    private static final String PREFERENCES_WIDGETSET_VERBOSE = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetVerbose";

    // project type flags - note that in the future, there could be multiple
    // flags set at the same time
    private static final String PREFERENCES_PROJECT_TYPE_GAE = VaadinPlugin.PLUGIN_ID
            + "." + "projectTypeGae";

    /**
     * Checks whether widgetset building for a project has been suspended
     * explicitly by the user.
     * 
     * @param project
     * @return
     */
    public boolean isWidgetsetCompilationSuspended() {
        return prefStore.getBoolean(PREFERENCES_WIDGETSET_SUSPENDED);
    }

    /**
     * Sets the suspended flag for widgetset compilation. If suspended the
     * widgetset will not be compiled automatically by the plugin. Returns true
     * if the value was changed, false if it remained the same.
     * 
     * @param parallelism
     * @return
     */
    public boolean setWidgetsetCompilationSuspended(boolean suspended) {
        boolean oldValue = isWidgetsetCompilationSuspended();
        prefStore.setValue(PREFERENCES_WIDGETSET_SUSPENDED, suspended);
        return oldValue != suspended;
    }

    public boolean isWidgetsetCompilationVerboseMode() {
        if (!prefStore.contains(PREFERENCES_WIDGETSET_VERBOSE)) {
            return false;
        } else {
            return prefStore.getBoolean(PREFERENCES_WIDGETSET_VERBOSE);
        }
    }

    /**
     * Sets the verbosity mode used in widgetset compilation. Returns true if
     * the value was changed, false if it remained the same.
     * 
     * @param parallelism
     * @return
     */
    public boolean setWidgetsetCompilationVerboseMode(boolean verbose) {
        boolean oldValue = isWidgetsetCompilationVerboseMode();
        prefStore.setValue(PREFERENCES_WIDGETSET_VERBOSE, verbose);
        return oldValue != verbose;

    }

    public String getWidgetsetCompilationStyle() {
        if (!prefStore.contains(PREFERENCES_WIDGETSET_STYLE)) {
            return "OBF";
        } else {
            return prefStore.getString(PREFERENCES_WIDGETSET_STYLE);
        }
    }

    /**
     * Sets the style parameter used in widgetset compilation. Returns true if
     * the value was changed, false if it remained the same.
     * 
     * @param style
     * @return
     */
    public boolean setWidgetsetCompilationStyle(String style) {
        String oldValue = getWidgetsetCompilationStyle();
        prefStore.setValue(PREFERENCES_WIDGETSET_STYLE, style);
        return !equals(oldValue, style);

    }

    public String getWidgetsetCompilationParallelism() {
        if (!prefStore.contains(PREFERENCES_WIDGETSET_PARALLELISM)) {
            return "";
        } else {
            return prefStore.getString(PREFERENCES_WIDGETSET_PARALLELISM);
        }
    }

    /**
     * Sets the parallelism parameter used in widgetset compilation. Returns
     * true if the value was changed, false if it remained the same.
     * 
     * @param parallelism
     * @return
     */
    public boolean setWidgetsetCompilationParallelism(String parallelism) {
        String oldValue = getWidgetsetCompilationParallelism();
        prefStore.setValue(PREFERENCES_WIDGETSET_PARALLELISM, parallelism);
        return !equals(oldValue, parallelism);
    }

    /**
     * Compares the two strings. Returns true if both are null or both contain
     * the same characters.
     * 
     * @param oldValue
     * @param newValue
     * @return
     */
    private boolean equals(String oldValue, String newValue) {
        if (oldValue == null) {
            return newValue == null;
        }
        if (newValue == null) {
            return false;
        }
        return oldValue.equals(newValue);
    }

    public void persist() throws IOException {
        prefStore.save();
    }

    public long getEstimatedCompilationTime() {
        if (prefStore.contains(PREFERENCES_WIDGETSET_COMPILATION_ETA)) {
            return prefStore.getLong(PREFERENCES_WIDGETSET_COMPILATION_ETA);
        } else {
            /**
             * Make an initial wild guess that compilation takes two minutes.
             */
            return 120 * 1000l;
        }

    }

    public void setWidgetsetCompilationTimeEstimate(long estimate) {
        prefStore.setValue(PREFERENCES_WIDGETSET_COMPILATION_ETA, estimate);

    }

    /**
     * Checks if the widgetset is marked as dirty. Returns true for dirty, false
     * for not dirty and null if no marking was found.
     * 
     * @return
     */
    public Boolean isWidgetsetDirty() {
        if (prefStore.contains(PREFERENCES_WIDGETSET_DIRTY)) {
            return prefStore.getBoolean(PREFERENCES_WIDGETSET_DIRTY);
        }

        return null;

    }

    public void setWidgetsetDirty(boolean dirty) {
        prefStore
                .setValue(PREFERENCES_WIDGETSET_DIRTY, Boolean.toString(dirty));
    }

    /**
     * Sets a flag that marks if the project is a Google App Engine project.
     * 
     * @param gaeProject
     */
    public void setProjectTypeGae(boolean gaeProject) {
        prefStore.setValue(PREFERENCES_PROJECT_TYPE_GAE, gaeProject);
    }

    public boolean isGaeProject() {
        if (prefStore.contains(PREFERENCES_PROJECT_TYPE_GAE)) {
            return prefStore.getBoolean(PREFERENCES_PROJECT_TYPE_GAE);
        }

        return false;
    }
}
