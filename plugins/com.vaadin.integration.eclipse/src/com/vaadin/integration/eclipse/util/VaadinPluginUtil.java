package com.vaadin.integration.eclipse.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;
import org.osgi.framework.Bundle;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.util.files.LocalFileManager;

@SuppressWarnings("restriction")
public class VaadinPluginUtil {

    private static IPackageFragmentRoot getJavaFragmentRoot(IProject project)
            throws CoreException {
        try {
            IJavaProject javaProject = JavaCore.create(project);
            for (IClasspathEntry classPathEntry : javaProject.getRawClasspath()) {
                if (classPathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    IPackageFragmentRoot fragmentRoot = javaProject
                            .findPackageFragmentRoot(classPathEntry.getPath());
                    return fragmentRoot;
                }
            }

            return null;
        } catch (JavaModelException e) {
            throw ErrorUtil.newCoreException("Unable to locate source folder",
                    e);
        }
    }

    /**
     * Create a configuration file from a template if it does not exist.
     * 
     * @param file
     *            the file to create from template
     * @param template
     * @throws CoreException
     */
    public static IFile ensureFileFromTemplate(IFile file, String template)
            throws CoreException {

        try {
            if (file.exists()) {
                return file;
            }

            String stub = VaadinPluginUtil.readTextFromTemplate(template);

            ByteArrayInputStream stubstream = new ByteArrayInputStream(
                    stub.getBytes());

            file.create(stubstream, true, null);

            return file;

        } catch (JavaModelException e) {
            throw ErrorUtil.newCoreException(
                    "Failed to create " + file.getName() + " file", e);
        } catch (IOException e) {
            throw ErrorUtil.newCoreException(
                    "Failed to create " + file.getName() + " file", e);
        }
    }

    public static void copyPluginFileToProject(IPath src, IFile dest)
            throws CoreException {
        try {
            // Bundle bundle = VaadinPlugin.getInstance().getBundle();
            // InputStream input = FileLocator.openStream(bundle, src, false);

            File file = src.toFile();
            FileInputStream input = new FileInputStream(file);
            dest.create(input, true, null);
            input.close();
        } catch (Exception e) {
            throw ErrorUtil.newCoreException("Failed to copy file to project",
                    e);
        }
    }

    public static void copySourceFileToProject(IProject project,
            Path sourceFile, String destinationPackage, String destinationFile)
            throws CoreException {
        try {
            // JavaCore.createCompilationUnitFrom();
            Bundle bundle = VaadinPlugin.getInstance().getBundle();
            InputStream input = FileLocator.openStream(bundle, sourceFile,
                    false);
            StringWriter writer = new StringWriter();
            IOUtils.copy(input, writer);
            input.close();

            String contents = writer.toString();

            IPackageFragmentRoot fragmentRoot = VaadinPluginUtil
                    .getJavaFragmentRoot(project);
            IPackageFragment packageFragment = fragmentRoot
                    .createPackageFragment(destinationPackage, true, null);
            // ICompilationUnit compilationUnit =
            packageFragment.createCompilationUnit(destinationFile, contents,
                    true, null);

            // JavaCore.create(project.getWorkspace().getRoot().getFolder(sourceFile))
        } catch (Exception e) {
            throw ErrorUtil.newCoreException(
                    "Failed to copy source file to project", e);
        }

    }

    public static String createApplicationClassSource(String packageName,
            String applicationName, String applicationClass,
            String vaadinPackagePrefix) {
        String template = "package " + packageName + ";\n\n" + "import "
                + vaadinPackagePrefix + "Application;\n" + "import "
                + vaadinPackagePrefix + "ui.*;\n\n" + "public class "
                + applicationClass + " extends Application {\n"
                + "\t@Override\n" + "\tpublic void init() {\n"
                + "\t\tWindow mainWindow = new Window(\"" + applicationName
                + "\");\n"
                + "\t\tLabel label = new Label(\"Hello Vaadin user\");\n"
                + "\t\tmainWindow.addComponent(label);\n"
                + "\t\tsetMainWindow(mainWindow);\n" + "\t}\n" + "\n" + "}\n";

        return template;

    }

    public static String createRootClassSource(String packageName,
            String applicationName, String rootClass, String vaadinPackagePrefix) {
        String template = "package " + packageName + ";\n\n" + "import "
                + vaadinPackagePrefix + "ui.*;\n" + "import "
                + vaadinPackagePrefix + "terminal.WrappedRequest;\n\n"
                + "public class " + rootClass + " extends Root {\n"
                + "\t@Override\n"
                + "\tpublic void init(WrappedRequest request) {\n"
                + "\t\tsetCaption(\"" + applicationName + "\");\n"
                + "\t\tLabel label = new Label(\"Hello Vaadin user\");\n"
                + "\t\tgetContent().addComponent(label);\n" + "\t}\n" + "\n"
                + "}\n";

        return template;

    }

    /**
     * Create a variable-based classpath entry if the given path is under the
     * target of the variable, an absolute one otherwise.
     * 
     * @param variableName
     * @param jarPath
     * @return
     */
    public static IClasspathEntry makeVariableClasspathEntry(
            String variableName, IPath jarPath) {
        IPath variablePath = JavaCore.getClasspathVariable(variableName);
        if (variablePath.isPrefixOf(jarPath)) {
            // path starting with the variable name => relative to its content
            IPath jarVariablePath = new Path(variableName).append(jarPath
                    .removeFirstSegments(variablePath.segmentCount()));
            return JavaCore.newVariableEntry(jarVariablePath, null, null);
        } else {
            return JavaCore.newLibraryEntry(jarPath, null, null);
        }
    }

    /**
     * Replace an existing class path entry (identified by last segment name)
     * with a new one or optionally append the new entry if not found.
     * 
     * The position of the replaced element on the class path is kept unchanged.
     * If a new entry is added, it is inserted at the beginning of the class
     * path.
     * 
     * @param entries
     *            list of class path entries to modify
     * @param newEntry
     *            new entry
     * @param entryNames
     *            the first entry whose last path segment matches an element on
     *            this list is replaced
     * @param addIfMissing
     *            true to add the entry if no entry matching entryNames was
     *            found
     */
    public static void replaceClassPathEntry(List<IClasspathEntry> entries,
            IClasspathEntry newEntry, String[] entryNames, boolean addIfMissing) {
        boolean found = false;
        for (int i = 0; i < entries.size(); ++i) {
            for (String entryName : entryNames) {
                if (entryName.equals(entries.get(i).getPath().lastSegment())) {
                    entries.set(i, newEntry);
                    found = true;
                    break;
                }
            }
        }
        if (addIfMissing && !found && !entries.contains(newEntry)) {
            entries.add(0, newEntry);
        }
    }

    /**
     * Update the class path for program execution launch configurations
     * referring to any of the the given JAR file names in their arguments (not
     * the class path of the launch itself!) or in the class path of a Java
     * launch. This is called when a JAR is replaced by a different version
     * which may have a different name or location.
     * 
     * The old JAR is identified by its file name without path. For external
     * launches, the JAR path is extracted by back-tracking from the JAR file
     * name to the previous path separator and that full path is replaced with
     * the given new path to a JAR file.
     * 
     * This is primarily meant for updating the generated widgetset compilation
     * and hosted mode launches, but will also modify certain other kinds of
     * launches.
     * 
     * @throws CoreException
     */
    @SuppressWarnings("deprecation")
    public static void updateLaunchClassPath(IProject project,
            String[] jarNames, IPath jarPath) throws CoreException {
        // list all launches
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType typeExternal = manager
                .getLaunchConfigurationType(IExternalToolConstants.ID_PROGRAM_LAUNCH_CONFIGURATION_TYPE);
        ILaunchConfigurationType typeJava = manager
                .getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
        // it seems it is not possible to get the project for an external launch
        // with the official APIs
        // ILaunchConfiguration[] launchConfigurations = manager
        // .getLaunchConfigurations(type);

        // limit to launches that are top-level resources in the project of
        // interest
        for (IResource resource : project.members()) {
            // identify the external launches referring to the JAR
            if (resource instanceof IFile
                    && "launch".equals(resource.getFileExtension())) {
                ILaunchConfiguration launchConfiguration = manager
                        .getLaunchConfiguration((IFile) resource);
                if (launchConfiguration != null && launchConfiguration.exists()) {
                    if (typeExternal.equals(launchConfiguration.getType())) {
                        String arguments = launchConfiguration.getAttribute(
                                IExternalToolConstants.ATTR_TOOL_ARGUMENTS, "");
                        for (String jarName : jarNames) {
                            if (arguments.contains(jarName)) {
                                // update the classpath of a single launch
                                updateLaunchClassPath(launchConfiguration,
                                        jarName, jarPath);
                            }
                        }
                    } else if (typeJava.equals(launchConfiguration.getType())) {
                        IJavaProject jproject = JavaCore.create(project);
                        updateJavaLaunchClassPath(jproject,
                                launchConfiguration, jarNames, jarPath);
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static void updateLaunchClassPath(
            ILaunchConfiguration launchConfiguration, String jarName,
            IPath jarPath) throws CoreException {
        // update a launch - careful about separators etc.
        ILaunchConfigurationWorkingCopy workingCopy = launchConfiguration
                .getWorkingCopy();

        String arguments = launchConfiguration.getAttribute(
                IExternalToolConstants.ATTR_TOOL_ARGUMENTS, "");

        // find the JAR reference (from previous separator to the next) and
        // replace it with the new path
        // on all platforms, also need to handle &quot;
        String separators;
        if ("windows".equals(PlatformUtil.getPlatform())) {
            separators = ";&\"";
        } else {
            separators = ":&;\"";
        }

        // look for the JAR name potentially preceded with a path etc.
        Pattern pattern = Pattern.compile("[" + separators + "]([^"
                + separators + "]*" + jarName + ")[" + separators + "]");
        Matcher matcher = pattern.matcher(arguments);

        String newPath = JavaRuntime.newArchiveRuntimeClasspathEntry(jarPath)
                .getLocation();

        // replace path from previous separator to the next
        String result = arguments;
        matcher.find();
        for (int group = 1; group <= matcher.groupCount(); ++group) {
            result = result.replace(matcher.group(group), newPath);
            matcher.find();
        }

        workingCopy.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS,
                result);

        // save the launch
        workingCopy.doSave();
    }

    @SuppressWarnings("unchecked")
    private static void updateJavaLaunchClassPath(IJavaProject jproject,
            ILaunchConfiguration launchConfiguration, String[] jarNames,
            IPath jarPath) throws CoreException {
        // update a launch
        ILaunchConfigurationWorkingCopy workingCopy = launchConfiguration
                .getWorkingCopy();

        boolean modified = false;

        List<String> classPath = workingCopy.getAttribute(
                IJavaLaunchConfigurationConstants.ATTR_CLASSPATH,
                new ArrayList<String>());
        List<String> newClassPath = new ArrayList<String>();

        // any of the JAR file names
        StringBuilder jarNamesRegexp = new StringBuilder();
        for (String jarName : jarNames) {
            if (jarNamesRegexp.length() > 0) {
                jarNamesRegexp.append("|");
            }
            jarNamesRegexp.append(jarName);
        }
        Pattern pattern = Pattern.compile("externalArchive=\".*[/\\\\]("
                + jarNamesRegexp.toString() + ")\".*");
        Matcher matcher = pattern.matcher("");
        for (String cpMemento : classPath) {
            matcher.reset(cpMemento);
            if (matcher.find()) {
                // new memento from path
                String newMemento = JavaRuntime
                        .newArchiveRuntimeClasspathEntry(jarPath).getMemento();
                newClassPath.add(newMemento);
                modified = true;
            } else {
                newClassPath.add(cpMemento);
            }
        }

        if (modified) {
            workingCopy.setAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_CLASSPATH,
                    newClassPath);
        }

        // save the launch
        workingCopy.doSave();
    }

    /**
     * Returns the first gwt user jar defined in projects classpath.
     * 
     * If not set, a gwt jar file provided by plugin is returned.
     */
    public static IPath getGWTUserJarPath(IJavaProject jproject)
            throws CoreException {
        // check first for explicitly set gwt-user jar file
        IClasspathEntry[] rawClasspath = jproject.getRawClasspath();
        for (IClasspathEntry cp : rawClasspath) {
            if (cp.toString().contains("gwt-user")) {
                // User has explicitly defined GWT version to use directly on
                // the classpath, or classpath entry created by the plugin
                IClasspathEntry resolvedClasspathEntry = JavaCore
                        .getResolvedClasspathEntry(cp);
                return resolvedClasspathEntry.getPath();
            } else if (cp.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                // primarily WEB-INF/lib
                IClasspathContainer container = JavaCore.getClasspathContainer(
                        cp.getPath(), jproject);
                if (container == null) {
                    // Container is null at least for an invalid JRE reference
                    continue;
                }
                IClasspathEntry[] containerEntries = container
                        .getClasspathEntries();
                for (IClasspathEntry ccp : containerEntries) {
                    if (ccp.toString().contains("gwt-user")) {
                        // User has explicitly defined GWT version to use
                        IClasspathEntry resolvedClasspathEntry = JavaCore
                                .getResolvedClasspathEntry(ccp);
                        return resolvedClasspathEntry.getPath();
                    }
                }
            }
        }

        String gwtVersion = ProjectUtil
                .getRequiredGWTVersionForProject(jproject);
        return LocalFileManager.getLocalGwtUserJar(gwtVersion);
    }

    public static Path getPathToTemplateFile(String path) throws IOException {
        Bundle bundle = VaadinPlugin.getInstance().getBundle();
        URL fileURL = FileLocator.toFileURL(bundle.getResource("template/"
                + path));
        return new Path(fileURL.getPath());
    }

    public static String readTextFromStream(InputStream resourceAsStream) {
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(resourceAsStream, writer);
            resourceAsStream.close();
        } catch (IOException e) {
            // TODO this error message might not be ideal
            ErrorUtil.handleBackgroundException(IStatus.ERROR,
                    "Failed to read template file from the Vaadin plugin", e);
        }

        return writer.toString();

    }

    public static IPath getConfigurationPath() throws CoreException {
        URL userLocation = Platform.getUserLocation().getURL();
        URL configurationLocation = Platform.getConfigurationLocation()
                .getURL();

        if (configurationLocation != null) {
            try {
                return new Path(FileLocator.toFileURL(configurationLocation)
                        .getPath()).append(IPath.SEPARATOR
                        + VaadinPlugin.PLUGIN_ID);
            } catch (IOException e) {
                throw ErrorUtil.newCoreException("getConfigurationPath failed",
                        e);
            }
        }

        if (userLocation != null) {
            try {
                return new Path(FileLocator.toFileURL(userLocation).getPath())
                        .append(IPath.SEPARATOR + VaadinPlugin.PLUGIN_ID);
            } catch (IOException e) {
                throw ErrorUtil.newCoreException("getConfigurationPath failed",
                        e);
            }
        }

        IPath stateLocation = VaadinPlugin.getInstance().getStateLocation();
        if (stateLocation != null) {
            return stateLocation;
        }

        throw ErrorUtil.newCoreException(
                "getConfigurationPath found nowhere to store files", null);
    }

    public static IPath getDownloadDirectory() throws CoreException {
        IPath path = getConfigurationPath()
                .append(IPath.SEPARATOR + "download");

        // Create the directory if it does not exist
        if (!path.toFile().exists()) {
            path.toFile().mkdirs();
        }

        return path;
    }

    public static IPath getDownloadDirectory(String identifier)
            throws CoreException {
        IPath path = getDownloadDirectory()
                .append(IPath.SEPARATOR + identifier);

        // Create the directory if it does not exist
        if (!path.toFile().exists()) {
            path.toFile().mkdirs();
        }

        return path;
    }

    public static IPath getVersionedDownloadDirectory(String identifier,
            String version) throws CoreException {
        IPath path = getDownloadDirectory(identifier).append(
                IPath.SEPARATOR + version);

        return path;
    }

    /**
     * Create the folder if it does not exist. If the parent folder does not
     * exist, it is created first.
     * 
     * @param folder
     * @param monitor
     * @throws CoreException
     */
    public static void createFolders(IFolder folder, IProgressMonitor monitor)
            throws CoreException {
        boolean local = true;

        if (folder.exists()) {
            return;
        }

        if (!folder.getParent().exists()) {
            createFolders((IFolder) folder.getParent(), monitor);
        }

        folder.create(true, local, monitor);

    }

    public static boolean typeExtendsClass(IType type, String className)
            throws JavaModelException {
        if (type.exists() && type.isStructureKnown() && type.isClass()) {
            ITypeHierarchy h = type.newSupertypeHierarchy(null);
            IType spr = h.getSuperclass(type);
            while (spr != null) {
                if (!spr.isClass()) {
                    break;
                }
                if (spr.getFullyQualifiedName().equals(className)) {

                    return true;
                }
                spr = h.getSuperclass(spr);
            }
        }
        return false;
    }

    private static InputStream openPluginFileAsStream(String templateName)
            throws IOException {
        Bundle bundle = VaadinPlugin.getInstance().getBundle();
        InputStream input = FileLocator.openStream(bundle, new Path(
                templateName), false);
        return input;
    }

    public static String readTextFromTemplate(String templateName)
            throws IOException {
        return readTextFromStream(openPluginFileAsStream("template/"
                + templateName));
    }

    public static IType[] getSubClasses(IProject project, String superClass,
            boolean allSubTypes, IProgressMonitor monitor)
            throws JavaModelException {
        // find all non-binary subclasses of a named class in the project
        IJavaProject javaProject = JavaCore.create(project);
        if (javaProject == null) {
            return new IType[0];
        }
        IType superType = javaProject.findType(superClass);
        ITypeHierarchy newTypeHierarchy = superType.newTypeHierarchy(monitor);

        IType[] subTypes;
        if (allSubTypes) {
            subTypes = newTypeHierarchy.getAllSubtypes(superType);
        } else {
            subTypes = newTypeHierarchy.getSubclasses(superType);
        }
        List<IType> types = new ArrayList<IType>();
        for (IType subclass : subTypes) {
            // #3441 for some reason, getAllSubtypes fetches types that are in
            // the project AND those open in editors => filter for project
            if (subclass.isResolved() && !subclass.isBinary()
                    && javaProject.equals(subclass.getJavaProject())) {
                types.add(subclass);
            }
        }
        return types.toArray(new IType[0]);
    }

    public static IType[] getApplicationClasses(IProject project,
            IProgressMonitor monitor) throws JavaModelException {
        // find all non-binary subclasses of Application in the project
        return getSubClasses(project, VaadinPlugin.APPLICATION_CLASS_FULL_NAME,
                true, monitor);
    }

    public static boolean isVaadinJar(IPath path) {
        if ("jar".equals(path.getFileExtension())
                && path.lastSegment().contains("vaadin")) {
            JarFile jarFile = null;
            try {
                URL url = path.toFile().toURL();
                url = new URL("jar:" + url.toExternalForm() + "!/");
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                jarFile = conn.getJarFile();
                Manifest manifest = jarFile.getManifest();
                if (manifest == null) {
                    return false;
                }
                Attributes mainAttributes = manifest.getMainAttributes();
                if ("Vaadin".equals(mainAttributes.getValue("Bundle-Name"))
                        && "com.vaadin".equals(mainAttributes
                                .getValue("Bundle-SymbolicName"))) {
                    return true;
                }
            } catch (MalformedURLException e) {
                String message = (jarFile == null) ? "Could not access JAR"
                        : "Could not access JAR " + jarFile.getName();
                ErrorUtil
                        .handleBackgroundException(IStatus.WARNING, message, e);
            } catch (IOException e) {
                String message = (jarFile == null) ? "Could not access JAR"
                        : "Could not access JAR " + jarFile.getName();
                ErrorUtil
                        .handleBackgroundException(IStatus.WARNING, message, e);
            } finally {
                closeJarFile(jarFile);
            }
        }
        return false;
    }

    public static void closeJarFile(JarFile jarFile) {
        // TODO make better jar handling. Windows locks files without
        // this, mac fails to rebuild widgetset with
        if (PlatformUtil.getPlatform().equals("windows")) {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    ErrorUtil.handleBackgroundException(IStatus.WARNING,
                            "Closing JAR file failed", e);
                }
            }
        }
    }

    public static IPath makePathAbsolute(IPath path) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource workspaceResource = root.findMember(path);
        if (workspaceResource != null) {
            path = workspaceResource.getRawLocation();
        }
        return path;
    }

    /**
     * Returns the project classpath as a string, in a format that can be used
     * when launching external programs on the same platform where Eclipse is
     * running.
     * 
     * For a Vaadin 6.2+ project, output locations should be on the classpath of
     * the widgetset compiler (but after all source directories) to enable
     * accessing the server side annotations.
     * 
     * @param jproject
     * @param includeOutputDirectories
     *            true to also include output (class file) locations on the
     *            classpath
     * @return
     * @throws CoreException
     * @throws JavaModelException
     */
    public static String getProjectBaseClasspath(IJavaProject jproject,
            boolean includeOutputDirectories) throws CoreException,
            JavaModelException {
        String classpathSeparator = PlatformUtil.getClasspathSeparator();
        IProject project = jproject.getProject();

        // use LinkedHashSet that preserves order but eliminates duplicates
        Set<IPath> sourceLocations = new LinkedHashSet<IPath>();
        Set<IPath> outputLocations = new LinkedHashSet<IPath>();
        Set<IPath> otherLocations = new LinkedHashSet<IPath>();

        // ensure the default output location is on the classpath
        outputLocations.add(getRawLocation(project,
                jproject.getOutputLocation()));

        // key libraries
        IRuntimeClasspathEntry gwtdev = JavaRuntime
                .newArchiveRuntimeClasspathEntry(ProjectDependencyManager
                        .getGWTDevJarPath(jproject));
        otherLocations.add(getRawLocation(project, gwtdev.getPath()));

        IRuntimeClasspathEntry gwtuser = JavaRuntime
                .newArchiveRuntimeClasspathEntry(getGWTUserJarPath(jproject));
        otherLocations.add(getRawLocation(project, gwtuser.getPath()));

        IPath vaadinJarPath = ProjectUtil.findProjectVaadinJarPath(jproject);
        if (vaadinJarPath == null) {
            throw ErrorUtil.newCoreException("Vaadin JAR could not be found");
        }
        IRuntimeClasspathEntry vaadinJar = JavaRuntime
                .newArchiveRuntimeClasspathEntry(vaadinJarPath);
        otherLocations.add(getRawLocation(project, vaadinJar.getPath()));

        // iterate over build path and classify its components
        // only source locations and their output directories (if any) are used
        for (IClasspathEntry classPathEntry : jproject
                .getResolvedClasspath(true)) {
            if (classPathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                // gwt compiler also needs javafiles for classpath
                IPath path = classPathEntry.getPath();
                sourceLocations.add(getRawLocation(project, path));

                // source entry has custom output location?
                IPath outputLocation = classPathEntry.getOutputLocation();
                if (outputLocation != null) {
                    outputLocations
                            .add(getRawLocation(project, outputLocation));
                }
                // } else {
                // IPath path = classPathEntry.getPath();
                // IPath rawLocation = getRawLocation(project, path);
                // otherLocations.add(rawLocation);
                // } else if (classPathEntry) {
            } else if (classPathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                // Include gwt dependencies aswell
                if (ProjectUtil.isGWTDependency(jproject,
                        classPathEntry.getPath())) {
                    otherLocations.add(getRawLocation(project,
                            classPathEntry.getPath()));
                }

            }
        }

        // source directories must come before output locations
        Set<IPath> locations = new LinkedHashSet<IPath>();

        locations.addAll(sourceLocations);
        if (includeOutputDirectories) {
            locations.addAll(outputLocations);
        }
        locations.addAll(otherLocations);

        // safeguard
        locations.remove(null);

        // construct classpath string
        // TODO deprecated variable, broken - can give wrong JRE
        IRuntimeClasspathEntry systemLibsEntry = JavaRuntime
                .newVariableRuntimeClasspathEntry(new Path(
                        JavaRuntime.JRELIB_VARIABLE));

        String classPath = systemLibsEntry.getLocation();
        for (IPath path : locations) {
            if ("".equals(classPath)) {
                classPath = path.toPortableString();
            } else {
                classPath = classPath + classpathSeparator
                        + path.toPortableString();
            }
        }

        return classPath;
    }

    /**
     * Returns the full path to the Java executable. The project JVM is used if
     * available, the workspace default VM if none is specified for the project.
     * 
     * @param jproject
     * @return JVM executable path in platform specific format
     * @throws CoreException
     */
    public static String getJvmExecutablePath(IJavaProject jproject)
            throws CoreException {
        String vmName;
        IVMInstall vmInstall = JavaRuntime.getVMInstall(jproject);
        // this might be unnecessary
        if (vmInstall == null) {
            vmInstall = JavaRuntime.getDefaultVMInstall();
        }
        File vmBinDir = new File(vmInstall.getInstallLocation(), "bin");
        // windows hack, as Eclipse can run the JVM but does not give its
        // executable name through public APIs
        if ("windows".equals(PlatformUtil.getPlatform())) {
            vmName = new File(vmBinDir, "java.exe").getAbsolutePath();
        } else {
            vmName = new File(vmBinDir, "java").getAbsolutePath();
        }
        return vmName;
    }

    /**
     * Convert a path to a raw filesystem location - also works when the project
     * is outside the workspace
     * 
     * @param project
     * @param path
     * @return
     */
    public static IPath getRawLocation(IProject project, IPath path) {
        // constructing the handles is inexpensive
        IFolder folder = project.getWorkspace().getRoot().getFolder(path);
        IFile file = project.getWorkspace().getRoot().getFile(path);
        if (folder.exists()) {
            return folder.getRawLocation();
        } else if (file.exists()) {
            return file.getRawLocation();
        } else {
            // assumed to be complete path if not in the workspace
            return path;
        }
    }

    /**
     * Find Java launch configuration for GWT hosted mode, create it if missing.
     * 
     * @param project
     * @return the {@link ILaunchConfiguration} created/found launch
     *         configuration or null if none
     */
    public static ILaunchConfiguration createHostedModeLaunch(IProject project) {
        if (project == null) {
            return null;
        }

        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

        ILaunchConfigurationType type = manager
                .getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);

        try {
            IJavaProject jproject = JavaCore.create(project);

            // check GWT version
            boolean isGwt20 = ProjectUtil.isGwt20(project);

            String launchName = "GWT " + (isGwt20 ? "development" : "hosted")
                    + " mode for " + project.getName();

            // find and return existing launch, if any
            ILaunchConfiguration[] launchConfigurations = manager
                    .getLaunchConfigurations();
            for (ILaunchConfiguration launchConfiguration : launchConfigurations) {
                if (launchName.equals(launchConfiguration.getName())) {
                    // is the launch in the same project?
                    String launchProject = launchConfiguration
                            .getAttribute(
                                    IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                                    "");
                    if (project.getName().equals(launchProject)) {
                        ErrorUtil
                                .logInfo("GWT development mode launch already exists for the project");
                        return launchConfiguration;
                    }
                }
            }

            // create a new launch configuration

            ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(
                    project, launchName);

            String mainClass = "com.google.gwt.dev.GWTShell";
            if (isGwt20) {
                mainClass = "com.google.gwt.dev.DevMode";
            }
            workingCopy.setAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
                    mainClass);

            workingCopy.setAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                    project.getName());

            IPath location = project.getLocation();
            workingCopy.setAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY,
                    location.toOSString());

            IFolder wsDir = ProjectUtil.getWebContentFolder(project)
                    .getFolder(VaadinPlugin.VAADIN_RESOURCE_DIRECTORY)
                    .getFolder("widgetsets");
            String wsDirString = wsDir.getLocation().toPortableString();
            if (wsDirString.startsWith(location.toPortableString())) {
                wsDirString = wsDirString.replaceFirst(
                        location.toPortableString() + "/", "");
            }
            String arguments;
            if (isGwt20) {
                arguments = "-noserver -war " + wsDirString + " "
                        + WidgetsetUtil.getConfiguredWidgetSet(jproject);

                // Could maybe make a more educated guess but this will at least
                // allow the user to launch a browser session and show what the
                // url parameters should be
                arguments += " -startupUrl http://localhost:8080/"
                        + project.getName();
                // Same as GWT default. Added to show users how to change the
                // bind address
                arguments += " -bindAddress 127.0.0.1";
            } else {
                arguments = "-noserver -out " + wsDirString;
            }

            workingCopy.setAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS,
                    arguments);

            String vmargs = "-Xmx512M -XX:MaxPermSize=256M";
            if (PlatformUtil.getPlatform().equals("mac") && !isGwt20) {
                vmargs = vmargs + " -XstartOnFirstThread";
            }
            workingCopy
                    .setAttribute(
                            IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
                            vmargs);

            // construct the launch classpath

            List<String> classPath = new ArrayList<String>();

            // GWT libraries should come first
            classPath.add(JavaRuntime.newArchiveRuntimeClasspathEntry(
                    ProjectDependencyManager.getGWTDevJarPath(jproject))
                    .getMemento());
            classPath.add(JavaRuntime.newArchiveRuntimeClasspathEntry(
                    getGWTUserJarPath(jproject)).getMemento());

            // default classpath reference, instead of "exploding"
            // JavaRuntime.computeUnresolvedRuntimeClasspath()
            classPath.add(JavaRuntime.newDefaultProjectClasspathEntry(jproject)
                    .getMemento());

            // add source paths on the classpath
            for (IClasspathEntry entry : jproject.getRawClasspath()) {
                if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    IRuntimeClasspathEntry source = JavaRuntime
                            .newArchiveRuntimeClasspathEntry(entry.getPath());
                    classPath.add(source.getMemento());
                }
            }

            workingCopy
                    .setAttribute(
                            IJavaLaunchConfigurationConstants.ATTR_CLASSPATH,
                            classPath);
            workingCopy.setAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH,
                    false);

            return workingCopy.doSave();

        } catch (CoreException e) {
            ErrorUtil.handleBackgroundException(
                    "Failed to find or create development mode launch for project "
                            + project.getName(), e);
            return null;
        }
    }

}
