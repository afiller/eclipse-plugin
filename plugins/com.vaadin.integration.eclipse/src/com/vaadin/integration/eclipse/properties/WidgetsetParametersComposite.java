package com.vaadin.integration.eclipse.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.vaadin.integration.eclipse.VaadinPlugin;

/**
 * Widgetset compilation preferences in project properties.
 */
public class WidgetsetParametersComposite extends Composite {

    private Combo styleCombo;
    private Combo parallelismCombo;
    private IProject project = null;

    public WidgetsetParametersComposite(Composite parent, int style) {
        super(parent, style);
    }

    public void setProject(IProject project) {
        this.project = project;

        ScopedPreferenceStore prefStore = new ScopedPreferenceStore(
                new ProjectScope(project), VaadinPlugin.PLUGIN_ID);

        // get values from project or defaults if none stored

        String style = prefStore
                .getString(VaadinPlugin.PREFERENCES_WIDGETSET_STYLE);
        if (style == null || "".equals(style)) {
            style = "OBF";
        }
        styleCombo.setText(style);

        String parallelism = prefStore
                .getString(VaadinPlugin.PREFERENCES_WIDGETSET_PARALLELISM);
        if (parallelism == null) {
            parallelism = "";
        }
        parallelismCombo.setText(parallelism);
    }

    public Composite createContents() {
        setLayout(new GridLayout(1, false));
        setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        createOptionsComposite(this);
        createInstructionsComposite(this);

        return this;
    }

    /**
     * Configurable options
     */
    private void createOptionsComposite(Composite parent) {
        Composite options = new Composite(parent, SWT.NULL);
        options.setLayout(new GridLayout(2, false));
        options
                .setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true,
                        false));

        // compilation style (obfuscated/pretty)
        Label label = new Label(options, SWT.NULL);
        label.setText("Javascript style:");

        styleCombo = new Combo(options, SWT.BORDER | SWT.DROP_DOWN
                | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        styleCombo.setLayoutData(gd);

        styleCombo.add("OBF");
        styleCombo.add("PRETTY");
        styleCombo.add("DETAILED");

        // compiler parallelism

        label = new Label(options, SWT.NULL);
        label.setText("Compiler threads:");

        parallelismCombo = new Combo(options, SWT.BORDER | SWT.DROP_DOWN
                | SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        parallelismCombo.setLayoutData(gd);

        parallelismCombo.add("");
        for (int i = 1; i <= 8; ++i) {
            parallelismCombo.add("" + i);
        }
    }

    private void createInstructionsComposite(Composite parent) {
        Composite instructions = new Composite(parent, SWT.NULL);
        instructions.setLayout(new FillLayout(SWT.HORIZONTAL));
        instructions.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true,
                false));

        Label label = new Label(instructions, SWT.WRAP);
        label
                .setText("To optimize widgetset compilation times, modify the \"user.agent\" parameter in the\n"
                        + "widgetset module file (.gwt.xml).");
    }

    /**
     * Gets the user-selected GWT compilation style. Default is "OBF".
     *
     * @return "OBF"/"PRETTY"/"DETAILED" - never null
     */
    public String getCompilationStyle() {
        return styleCombo.getText();
    }

    /**
     * Gets the user-selected number of GWT compiler threads. Default is no
     * selection (empty string).
     *
     * @return String containing a positive number or empty string if none
     *         specified, not null
     */
    public String getParallelism() {
        return parallelismCombo.getText();
    }

}
