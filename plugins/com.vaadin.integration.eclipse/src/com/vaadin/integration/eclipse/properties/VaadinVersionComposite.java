package com.vaadin.integration.eclipse.properties;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.AbstractElementListSelectionDialog;

import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.VersionUtil;
import com.vaadin.integration.eclipse.util.data.AbstractVaadinVersion;
import com.vaadin.integration.eclipse.util.data.DownloadableVaadinVersion;
import com.vaadin.integration.eclipse.util.data.LocalVaadinVersion;
import com.vaadin.integration.eclipse.util.files.LocalFileManager;
import com.vaadin.integration.eclipse.util.files.LocalFileManager.FileType;
import com.vaadin.integration.eclipse.util.network.DownloadManager;

/**
 * Project property page for selecting an Vaadin version and updating the JAR in
 * the project.
 */
public class VaadinVersionComposite extends Composite {

    private Combo versionCombo;
    private Map<String, LocalVaadinVersion> versionMap = new HashMap<String, LocalVaadinVersion>();
    private Button downloadButton;
    private IProject project = null;
    private VersionSelectionChangeListener versionSelectionListener;

    private static class DownloadVaadinDialog extends
            AbstractElementListSelectionDialog {

        /**
         * This is an ugly and inefficient hack: as sorting cannot be disabled
         * for AbstractElementListSelectionDialog, use a comparator that
         * compares the positions of the elements in a reference list.
         * 
         * Cannot do real version comparison as no information is available
         * about which builds are official releases, pre-releases or release
         * candidates, nightly builds etc.
         */
        private static class VersionStringComparator implements
                Comparator<String> {
            private Map<String, Integer> positions = new HashMap<String, Integer>();

            public VersionStringComparator(
                    List<DownloadableVaadinVersion> versionList) {
                for (int i = 0; i < versionList.size(); ++i) {
                    positions.put(versionList.get(i).getVersionNumber(), i);
                }
            }

            public int compare(String o1, String o2) {
                return positions.get(o1) - positions.get(o2);
            }
        }

        public DownloadVaadinDialog(Shell parent) {
            super(parent, new LabelProvider());

            setTitle("Select Vaadin Version to Download");
            setMessage("Select a Vaadin library version (* = any string, ? = any char):");
            setMultipleSelection(false);
        }

        /*
         * @see SelectionStatusDialog#computeResult()
         */
        @Override
        protected void computeResult() {
            setResult(Arrays.asList(getSelectedElements()));
        }

        /**
         * Creates the checkbox to show or hide development versions and nightly
         * builds.
         * 
         * @param composite
         *            the parent composite of the message area.
         */
        protected Button createDevelopmentCheckbox(Composite composite) {
            GridData data = new GridData();
            data.grabExcessVerticalSpace = false;
            data.grabExcessHorizontalSpace = true;
            data.horizontalAlignment = GridData.FILL;
            data.verticalAlignment = GridData.BEGINNING;

            final Button developmentCheckbox = new Button(composite, SWT.CHECK);
            developmentCheckbox
                    .setText("Show pre-release versions and nightly builds");
            developmentCheckbox.setSelection(false);
            developmentCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateVersionList(developmentCheckbox.getSelection());
                }
            });
            developmentCheckbox.setLayoutData(data);

            return developmentCheckbox;
        }

        // fetch the list of available versions, including or excluding
        // development versions and nightly builds
        protected void updateVersionList(boolean development) {
            try {
                DownloadableVaadinVersion selected = getSelectedVersion();

                List<DownloadableVaadinVersion> available = DownloadManager
                        .getAvailableVersions(!development);
                // Equals and hasCode implementation in
                // AbstractVaadinVersion enables us to do this
                available.removeAll(LocalFileManager
                        .getLocalVaadinJarVersions());

                DownloadableVaadinVersion[] versions = available
                        .toArray(new DownloadableVaadinVersion[0]);
                fFilteredList.setComparator(new VersionStringComparator(
                        available));
                setListElements(versions);

                // try to preserve selection
                if (selected != null) {
                    setSelection(new DownloadableVaadinVersion[] { selected });
                } else {
                    setSelection(getInitialElementSelections().toArray());
                }

            } catch (CoreException ex) {
                String displayMsg = "Failed to download list of available Vaadin versions";
                Throwable cause = ex.getCause();
                if (cause != null) {
                    displayMsg += "\n\n" + cause.getClass().getName();
                }
                String msg = cause.getMessage();
                if (msg != null && msg.length() > 0) {
                    displayMsg += ": " + msg;
                }
                ErrorUtil.displayError(displayMsg,

                ex, getShell());
                ErrorUtil
                        .handleBackgroundException(
                                IStatus.WARNING,
                                "Failed to update the list of available Vaadin versions",
                                ex);
            }
        }

        /*
         * @see Dialog#createDialogArea(Composite)
         */
        @Override
        protected Control createDialogArea(Composite parent) {
            Composite contents = (Composite) super.createDialogArea(parent);

            createMessageArea(contents);
            createFilterText(contents);
            createFilteredList(contents);

            createDevelopmentCheckbox(contents);

            updateVersionList(false);

            return contents;
        }

        /**
         * Gets the selected version string or null if none.
         * 
         * @return String version or null
         */
        public DownloadableVaadinVersion getSelectedVersion() {
            return (DownloadableVaadinVersion) getFirstResult();
        }

        @Override
        protected void handleEmptyList() {
            updateOkState();
        }
    }

    public VaadinVersionComposite(Composite parent, int style) {
        super(parent, style);

        setLayout(new GridLayout(3, false));
        GridData data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        setLayoutData(data);
    }

    private void addVersionSelectionSection() {
        Label label = new Label(this, SWT.NULL);
        label.setText("Vaadin version:");

        // Vaadin version selection combo
        versionCombo = new Combo(this, SWT.BORDER | SWT.DROP_DOWN
                | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        versionCombo.setLayoutData(gd);

        // list available versions not yet downloaded
        downloadButton = new Button(this, SWT.NULL);
        downloadButton.setText("Download...");
        gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        downloadButton.setLayoutData(gd);
        downloadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                downloadVaadin();
            }
        });
    }

    /**
     * Helper that opens the directory chooser dialog.
     * 
     * @param startingDirectory
     *            The directory the dialog will open in.
     * @return File File or <code>null</code>.
     */
    private File getDirectory(File startingDirectory) {

        DirectoryDialog fileDialog = new DirectoryDialog(getShell(), SWT.OPEN
                | SWT.SHEET);
        if (startingDirectory != null) {
            fileDialog.setFilterPath(startingDirectory.getPath());
        }
        String dir = fileDialog.open();
        if (dir != null) {
            dir = dir.trim();
            if (dir.length() > 0) {
                return new File(dir);
            }
        }

        return null;
    }

    private void updateVersionCombo() {
        versionCombo.setEnabled(true);
        downloadButton.setEnabled(true);
        try {

            versionCombo.removeAll();
            // Always allow empty selection which removes Vaadin from the
            // project
            versionCombo.add("");
            versionMap.clear();
            for (LocalVaadinVersion version : LocalFileManager
                    .getLocalVaadinJarVersions()) {
                versionMap.put(version.getVersionNumber(), version);
                versionCombo.add(version.getVersionNumber());
            }
            versionCombo.setText("");

            try {
                // select current version (if any)
                if (project == null) {
                    return;
                }

                IPath vaadinLibrary = ProjectUtil.getVaadinLibraryInProject(
                        project, true);
                if (vaadinLibrary == null) {
                    return;
                }

                String currentVaadinVersionString = VersionUtil
                        .getVaadinVersionFromJar(vaadinLibrary);
                if (currentVaadinVersionString == null) {
                    return;
                }

                // There is a version of the Vaadin jar in the project. It might
                // be in WEB-INF/lib or somewhere else on the classpath.

                // Ensure the version is listed, it might be a custom jar or it
                // might have been removed from the local store for instance
                // when Eclipse was upgraded.

                LocalVaadinVersion projectVaadinVersion = new LocalVaadinVersion(
                        FileType.VAADIN_RELEASE, currentVaadinVersionString,
                        vaadinLibrary);

                // Always show current version as "6.4.8 (vaadin-*.jar)"
                String comboboxString = currentVaadinVersionString + " ("
                        + projectVaadinVersion.getJarFilename() + ")";

                versionMap.put(comboboxString, projectVaadinVersion);
                // Add the string to the combo box as first
                // ("" becomes second)
                versionCombo.add(comboboxString, 0);
                versionCombo.setText(comboboxString);

                if (!ProjectUtil.isInProject(project, vaadinLibrary)) {
                    // If the Vaadin JAR is outside the project we just
                    // show it to the user. We really do not want to delete
                    // files outside the project anyway.
                    versionCombo.setEnabled(false);
                    downloadButton.setEnabled(false);
                }
            } catch (CoreException ce) {
                // ignore if cannot select current version
                ErrorUtil
                        .handleBackgroundException(
                                IStatus.WARNING,
                                "Failed to select the Vaadin version used in the project",
                                ce);
            }
        } catch (CoreException ex) {
            // leave the combo empty and show an error message
            ErrorUtil.displayError("Failed to list downloaded Vaadin versions",
                    ex, getShell());
        }
    }

    // list available versions not yet downloaded and let the user download one
    private void downloadVaadin() {
        try {
            // let the user choose the version to download in a dialog
            DownloadVaadinDialog dialog = new DownloadVaadinDialog(getShell());

            if (dialog.open() == Window.OK) {
                final AbstractVaadinVersion version = dialog
                        .getSelectedVersion();
                if (version != null) {
                    IRunnableWithProgress op = new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor)
                                throws InvocationTargetException {
                            try {
                                DownloadManager.downloadVaadinJar(
                                        version.getVersionNumber(), monitor);
                            } catch (CoreException e) {
                                throw new InvocationTargetException(e);
                            } finally {
                                monitor.done();
                            }
                        }
                    };
                    // ProgressService would not show the progress dialog if in
                    // a modal dialog
                    new ProgressMonitorDialog(getShell()).run(true, true, op);

                    updateVersionCombo();
                    versionCombo.setText(version.getVersionNumber());
                    fireVersionSelectionChanged();
                }
            }
        } catch (InterruptedException e) {
            return;
        } catch (InvocationTargetException e) {
            ErrorUtil.displayError(
                    "Failed to download selected Vaadin version",
                    e.getTargetException(), getShell());
        }
    }

    /**
     * This method exists only to enable automatic synchronization with a model.
     * The combo box value is the selected version string.
     * 
     * @return Combo
     */
    public Combo getVersionCombo() {
        return versionCombo;
    }

    public Composite createContents() {
        addVersionSelectionSection();
        return this;
    }

    public LocalVaadinVersion getSelectedVersion() {
        LocalVaadinVersion newVaadinVersion = versionMap.get(versionCombo
                .getText());
        if ("".equals(newVaadinVersion)) {
            newVaadinVersion = null;
        }
        return newVaadinVersion;
    }

    /**
     * Returns the text for the selected item in the version combo box.
     * 
     * @return The string shown in the combo box. Never null.
     */
    public String getSelectedVersionString() {
        return versionCombo.getText();
    }

    public void setProject(IProject project) {
        this.project = project;

        updateView();
    }

    public void setNewProject() {
        project = null;

        selectLatestLocalVersion();
    }

    protected void updateView() {
        updateVersionCombo();
    }

    protected void selectLatestLocalVersion() {
        try {
            LocalVaadinVersion newestLocalVaadinVersion = LocalFileManager
                    .getNewestLocalVaadinJarVersion();
            if (newestLocalVaadinVersion != null) {
                versionCombo.setText(newestLocalVaadinVersion
                        .getVersionNumber());
            }
        } catch (CoreException e) {
            // maybe there is no version downloaded - ignore
            ErrorUtil
                    .handleBackgroundException(
                            IStatus.WARNING,
                            "Failed to select the most recent cached Vaadin version, probably no versions in cache yet",
                            e);
        }
    }

    public interface VersionSelectionChangeListener {
        public void versionChanged();
    }

    public void setVersionSelectionListener(
            final VersionSelectionChangeListener listener) {
        versionSelectionListener = listener;
        versionCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fireVersionSelectionChanged();
            }
        });
    }

    protected void fireVersionSelectionChanged() {
        if (versionSelectionListener != null) {
            versionSelectionListener.versionChanged();
        }
    }

}