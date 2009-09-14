package com.vaadin.integration.eclipse.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;
import com.vaadin.integration.eclipse.viewers.ApplicationList;

/**
 * This page lets user input a name for the theme.
 * 
 */

public class NewThemeWizardPage extends WizardPage {
    private Text themeName;
    private Combo projectCombo;
    private ApplicationList applicationList;
    private ISelection selection;
    private IProject project;
    // previous autogenerated theme name
    private String previousThemeName = "";

    public NewThemeWizardPage(ISelection selection) {
        super("wizardPage");
        setTitle("Create a new Vaadin theme");
        this.selection = selection;

        IProject project = VaadinPluginUtil.getProject(selection);
        String directory = VaadinPluginUtil.getVaadinResourceDirectory(project);
        setDescription("This wizard creates a theme folder and styles.css file to WebContent/"
                + directory + " directory.");
    }

    /**
     * @see IDialogPage#createControl(Composite)
     */
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        GridLayout layout = new GridLayout();
        container.setLayout(layout);
        layout.numColumns = 2;
        layout.verticalSpacing = 9;

        Label label = new Label(container, SWT.NULL);
        label.setText("&Project:");

        // project selection combo
        projectCombo = new Combo(container, SWT.BORDER | SWT.DROP_DOWN
                | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        projectCombo.setLayoutData(gd);
        for (IProject project : ResourcesPlugin.getWorkspace().getRoot()
                .getProjects()) {
            if (VaadinFacetUtils.isVaadinProject(project)) {
                projectCombo.add(project.getName());
            }
        }
        projectCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (!"".equals(projectCombo.getText())) {
                    setProject(ResourcesPlugin.getWorkspace().getRoot()
                            .getProject(projectCombo.getText()));
                } else {
                    setProject(null);
                }
            }
        });

        label = new Label(container, SWT.NULL);
        label.setText("&Theme name:");

        themeName = new Text(container, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        themeName.setLayoutData(gd);
        themeName.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                dialogChanged();
            }
        });

        label = new Label(container, SWT.NULL);
        label.setText("&Modify application classes to use theme:");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalAlignment = SWT.TOP;
        label.setLayoutData(gd);

        // applications can be selected from a list
        applicationList = new ApplicationList(container, SWT.BORDER | SWT.MULTI
                | SWT.V_SCROLL);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalAlignment = SWT.TOP;
        gd.heightHint = 120;
        applicationList.setLayoutData(gd);

        // initialize project based on the current selection
        setProject(VaadinPluginUtil.getProject(selection));
        dialogChanged();
        setControl(container);
    }

    private void setProject(IProject project) {
        IProject previousProject = this.project;
        this.project = project;

        // select correct project in the combo box
        int index;
        if (project != null) {
            index = projectCombo.indexOf(project.getName());
        } else {
            index = -1;
        }
        if (index == -1) {
            index = projectCombo.indexOf("");
        }
        // on Linux, Combo.select(int) always causes a ModifyEvent
        if (index != projectCombo.getSelectionIndex()) {
            projectCombo.select(index);
        }

        // can skip updating theme name and application list if project has not
        // changed
        if (project != previousProject) {
            // update theme name if and only if the user has not modified it
            if (themeName.getText().equals(previousThemeName)) {
                if (project != null) {
                    themeName.setText(project.getName().toLowerCase()
                            .replaceAll(" ", "")
                            + "theme");
                } else {
                    themeName.setText("theme");
                }
                previousThemeName = themeName.getText();
            }

            // update application list
            applicationList.update(project);
            // select all applications by default
            applicationList.selectAll();
        }
    }

    /**
     * Ensures that both text fields are set.
     */
    private void dialogChanged() {
        if (project == null) {
            updateStatus("No suitable project found");
            return;
        }

        IResource container = VaadinPluginUtil.getWebContentFolder(project);
        String themeName = getThemeName();

        if (container == null
                || (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
            updateStatus("No suitable project found");
            return;
        }
        if (!container.isAccessible()) {
            updateStatus("Project must be writable");
            return;
        }
        if (themeName.length() == 0) {
            updateStatus("File name must be specified");
            return;
        }
        if (themeName.replace('\\', '/').indexOf('/', 1) > 0) {
            updateStatus("File name must be valid");
            return;
        }
        updateStatus(null);
    }

    private void updateStatus(String message) {
        setErrorMessage(message);
        setPageComplete(message == null);
    }

    public String getThemeName() {
        return themeName.getText();
    }

    public IProject getProject() {
        return project;
    }

    public java.util.List<IType> getApplicationClassesToModify() {
        return applicationList.getSelectedApplications();
    }
}