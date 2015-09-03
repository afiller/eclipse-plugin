package com.vaadin.integration.eclipse.background;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.preferences.PreferenceConstants;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.ProjectDependencyManager;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;
import com.vaadin.integration.eclipse.util.data.DownloadableVaadinVersion;
import com.vaadin.integration.eclipse.util.data.LocalVaadinVersion;
import com.vaadin.integration.eclipse.util.files.LocalFileManager;
import com.vaadin.integration.eclipse.util.network.DownloadManager;

/**
 * Background job that upgrades nightly builds in projects.
 */
public final class ReportVaadinUsageStatistics extends Job {

    private Map<IProject, String> vaadinProjects;

    // Use the GWT Freshness checker URL to store usage reports. 
    private static final String QUERY_URL = "https://tools.vaadin.com/version/currentversion.xml";
    private static final String FIRST_LAUNCH = "firstLaunch";

    public ReportVaadinUsageStatistics(String name, Map<IProject, String> vaadinProjects) {
        super(name);
        this.vaadinProjects = vaadinProjects;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Reporting usage statistics of Vaadin versions in use",
                (vaadinProjects.size())); // task size = number of projects

        try {
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            // report vaadin projects

            for (IProject project : vaadinProjects.keySet()) {
                IJavaProject jProject;
                IVMInstall ivmInstall = null;
                try {
                    // attempt to get ijavaproject from iproject, this fails if
                    // the project has no java nature.
                    // a vaadin project without a java nature should not exist
                    // so this should always succeed.
                    jProject = (IJavaProject) project
                            .getNature(JavaCore.NATURE_ID);
                    ivmInstall = VaadinPluginUtil
                            .getJvmInstall(jProject, false);
                } catch (CoreException e) {
                    // this should never happen as long as all vaadin projects use java
                    ErrorUtil.displayErrorFromBackgroundThread("Could not find IVM for Vaadin Project", e.getLocalizedMessage());
                }

                String vaadinVersion = vaadinProjects.get(project);
                
                String userAgent = makeUserAgent(ivmInstall);
                
                report(vaadinVersion, userAgent);
                monitor.worked(1); // completed report for one project
            }

            return Status.OK_STATUS;
        } finally {
            monitor.done();
        }
    }

    private void report(String version, String userAgent) {
        
        
        if (VaadinPlugin
                .getInstance()
                .getPreferenceStore()
                .getBoolean(
                        PreferenceConstants.DISABLE_ALL_UPDATE_NOTIFICATIONS)) {
            //if the preference is true, update queries and usage reporting is disabled
            
            return;
            
        }

        String firstLaunch = getFirstLaunch();
        String url = QUERY_URL + "?v=" + version + "&id=" + firstLaunch + "&r="
                + "unknown";

        // TODO add more meaningful revision parameter if possible
        String entryPoint = "Compiler"; // TODO add more relevant entry point if
                                        // feasible
        if (entryPoint != null) {
            url += "&e=" + entryPoint;
        }

        doHttpGet(userAgent, url);

    }

    private String getFirstLaunch() {

        Preferences prefs;
        prefs = Preferences.userNodeForPackage(ReportVaadinUsageStatistics.class);

        long currentTimeMillis = System.currentTimeMillis();
        String firstLaunch = prefs.get(FIRST_LAUNCH, null);
        if (firstLaunch == null) {
            firstLaunch = Long.toHexString(currentTimeMillis);
            prefs.put(FIRST_LAUNCH, firstLaunch);
        }
        return firstLaunch;

    }

    private void doHttpGet(String userAgent, String url) {
        Throwable caught;
        InputStream is = null;
        try {
            URL urlToGet = new URL(url);
            URLConnection conn = urlToGet.openConnection();
            conn.setRequestProperty("User-Agent", userAgent);
            is = conn.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] response = baos.toByteArray();
            return;
        } catch (MalformedURLException e) {
            caught = e;
        } catch (IOException e) {
            caught = e;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }

        ErrorUtil.displayErrorFromBackgroundThread("Caught an exception while executing HTTP query", caught.getLocalizedMessage());

        return;
    }

    private String makeUserAgent(IVMInstall ivmInstall) {
        // TODO come up with a better User Agent string that still passes the
        // whitelist
        String ua = "GWT Freshness Checker";// (

        StringBuffer extra = new StringBuffer();
        appendUserAgentProperty(extra, "java.vendor");
        appendUserAgentProperty(extra, "java.version");
        appendUserAgentProperty(extra, "os.arch");
        appendUserAgentProperty(extra, "os.name");
        appendUserAgentProperty(extra, "os.version");

        if (extra.length() > 0) {
            ua += " (" + extra.toString() + ")";
        }
        String ivmName = ivmInstall == null ? "" : ivmInstall.getName();
        ua += " Eclipse IVM: ";
        ua += ivmName;

        return ua;
    }

    private void appendUserAgentProperty(StringBuffer sb, String propName) {
        String propValue = System.getProperty(propName);
        if (propValue != null) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(propName);
            sb.append("=");
            sb.append(propValue);
        }
    }

}