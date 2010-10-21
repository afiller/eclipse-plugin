package com.vaadin.integration.eclipse;

import org.eclipse.ui.plugin.AbstractUIPlugin;

public class VaadinPlugin extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "com.vaadin.integration.eclipse";

    public static final String VAADIN_PACKAGE_PREFIX = "com.vaadin.";
    public static final String APPLICATION_CLASS_NAME = "Application";
    public static final String APPLICATION_CLASS_FULL_NAME = VAADIN_PACKAGE_PREFIX
            + APPLICATION_CLASS_NAME;

    public static final String ID_COMPILE_WS_APP = "compilews";

    // preference store keys

    // "true"/"false"/missing - if missing, check if >1 widgetset exists
    public static final String PREFERENCES_WIDGETSET_DIRTY = PLUGIN_ID + "."
            + "widgetsetDirty";
    // true to suspend automatic widgetset build requests for the project
    public static final String PREFERENCES_WIDGETSET_SUSPENDED = PLUGIN_ID
            + "." + "widgetsetBuildsSuspended";
    // "OBF"/"PRETTY"/"DETAILED" or missing (default to "OBF")
    public static final String PREFERENCES_WIDGETSET_STYLE = PLUGIN_ID + "."
            + "widgetsetStyle";
    // a number of threads to use (-localWorkers) or missing
    public static final String PREFERENCES_WIDGETSET_PARALLELISM = PLUGIN_ID
            + "." + "widgetsetParallelism";

    // the time last compilation lasted, used for estimation in progress monitor
    public static final String PREFERENCES_WIDGETSET_COMPILATION_ETA = PLUGIN_ID
            + "." + "widgetsetCompilationEta";

    // to output compilation messages to console or not
    public static final String PREFERENCES_WIDGETSET_VERBOSE = PLUGIN_ID + "."
            + "widgetsetVerbose";

    // project type flags - note that in the future, there could be multiple
    // flags set at the same time
    public static final String PREFERENCES_PROJECT_TYPE_GAE = PLUGIN_ID + "."
            + "projectTypeGae";
    public static final String PREFERENCES_PROJECT_TYPE_LIFERAY = PLUGIN_ID
            + "." + "projectTypeLiferay";

    public static final String PREFERENCES_LIFERAY_PATH = PLUGIN_ID + "."
            + "liferayWebInfPath";

    public static final String VAADIN_CLIENT_SIDE_CLASS_PREFIX = "V";

    public static final String VAADIN_RESOURCE_DIRECTORY = "VAADIN";

    public static final String VAADIN_DEFAULT_THEME = "reindeer";

    public static final String GWT_COMPILER_CLASS = "com.vaadin.tools.WidgetsetCompiler";

    private static VaadinPlugin instance = null;

    public VaadinPlugin() {
        instance = this;
    }

    public static VaadinPlugin getInstance() {
        return instance;
    }

}
