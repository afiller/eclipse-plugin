package com.vaadin.integration.eclipse.handlers;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.builder.ThemeCompiler;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class CompileThemeHandler extends AbstractVaadinCompileHandler {

    public CompileThemeHandler() {
    }

    @Override
    public void startCompileJob(ISelection currentSelection,
            IEditorPart activeEditor) {
        startCompileThemeJob(currentSelection, activeEditor, null);
    }

    public static void startCompileThemeJob(final ISelection currentSelection,
            final IEditorPart activeEditor, ISchedulingRule schedulingRule) {
        Job job = new Job("Compiling theme...") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    monitor.beginTask("Compiling theme", 1);

                    // find and compile theme(s)
                    boolean compiled = false;
                    if (currentSelection instanceof IStructuredSelection
                            && ((IStructuredSelection) currentSelection).size() == 1) {
                        IStructuredSelection ssel = (IStructuredSelection) currentSelection;
                        Object obj = ssel.getFirstElement();
                        if (obj instanceof IFile) {
                            IFile file = (IFile) obj;
                            IProject project = file.getProject();
                            if (null != project) {
                                compiled = compileFile(monitor, file);
                            }
                        }
                        if (!compiled) {
                            IProject project = ProjectUtil
                                    .getProject(currentSelection);
                            if (project == null) {
                                IFile file = getFileForEditor(activeEditor);
                                if (file != null && file.exists()
                                        && null != file.getProject()) {
                                    compiled = compileFile(monitor, file);
                                }
                            } else if (null != project) {
                                compileAllThemes(monitor, project);
                                compiled = true;
                            }
                        }
                    } else {
                        IFile file = getFileForEditor(activeEditor);
                        if (file != null && null != file.getProject()) {
                            compiled = compileFile(monitor, file);
                        }
                    }

                    if (!compiled) {
                        ErrorUtil
                                .displayErrorFromBackgroundThread(
                                        "Select theme",
                                        "Select a theme file (.scss) or a Vaadin project to compile.");
                    }
                } catch (OperationCanceledException e) {
                    // Do nothing if user cancels compilation
                } catch (Exception e) {
                    showException(e);
                    // Also log the exception
                    ErrorUtil.handleBackgroundException(IStatus.ERROR,
                            "Theme compilation failed", e);
                } finally {
                    monitor.done();
                }
                return Status.OK_STATUS;
            }

        };

        if (schedulingRule != null) {
            job.setRule(schedulingRule);
        }
        job.setUser(false);
        job.schedule();
    }

    protected static void compileAllThemes(IProgressMonitor monitor,
            IProject project) throws CoreException {
        IFolder themes = ProjectUtil.getThemesFolder(project);
        if (themes.exists()) {
            for (IResource theme : themes.members()) {
                IFolder themeFolder = (IFolder) theme;
                try {
                    ThemeCompiler.run(project, monitor, themeFolder);
                    themeFolder.refreshLocal(IResource.DEPTH_ONE,
                            new SubProgressMonitor(monitor, 1));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // try to compile a file as an SCSS theme, or if not one, try to
    // compile themes in the containing project
    protected static boolean compileFile(IProgressMonitor monitor, IFile file)
            throws CoreException, IOException, InterruptedException {
        if (null == file || null == file.getProject()) {
            return false;
        }

        IProject project = file.getProject();
        IFolder themes = ProjectUtil.getThemesFolder(project);

        if (!themes.exists()) {
            return false;
        }

        boolean compiled = false;

        // TODO could check here if the file is within a theme and only compile
        // that theme

        if (!compiled) {
            if (VaadinFacetUtils.isVaadinProject(project)) {
                compileAllThemes(monitor, project);
                compiled = true;
            }
        }

        return compiled;
    }

    protected static void showException(final Exception e) {
        ErrorUtil.displayErrorFromBackgroundThread(
                "Error compiling theme",
                "Error compiling theme:\n" + e.getClass().getName() + " - "
                        + e.getMessage() + "\n\nSee error log for details.");
    }
}
