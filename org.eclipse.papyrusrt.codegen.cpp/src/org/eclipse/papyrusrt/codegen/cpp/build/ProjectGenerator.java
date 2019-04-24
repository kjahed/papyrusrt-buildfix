/*******************************************************************************
 * Copyright (c) 2016 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *   Contributors:
 *   Young-Soo Roh - Initial API and implementation
 *   
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp.build;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.SortedMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.cdt.codan.core.CodanRuntime;
import org.eclipse.cdt.codan.core.model.IProblem;
import org.eclipse.cdt.codan.core.model.IProblemProfile;
import org.eclipse.cdt.codan.internal.core.model.CodanProblem;
import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.IPDOMManager;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSetting;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.make.core.IMakeTarget;
import org.eclipse.cdt.make.core.IMakeTargetManager;
import org.eclipse.cdt.make.core.MakeCorePlugin;
import org.eclipse.cdt.make.core.MakeProjectNature;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyManager;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyType;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyValue;
import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.BuildListComparator;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IProjectType;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.internal.core.Configuration;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedProject;
import org.eclipse.cdt.utils.Platform;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.papyrusrt.codegen.CodeGenPlugin;
import org.eclipse.papyrusrt.codegen.cpp.rts.UMLRTSUtil;
import org.eclipse.uml2.uml.util.UMLUtil;
import org.osgi.framework.Bundle;

/**
 * UMLRT Codegen CDT project generator.
 * 
 */
@SuppressWarnings("restriction")
public final class ProjectGenerator {

	/**
	 * Target builder id.
	 */
	private static final String TARGET_BUILDER_ID = "org.eclipse.cdt.build.MakeTargetBuilder";

	/**
	 * CDT build artifact id.
	 */
	private static final String ARTIFACT = "org.eclipse.cdt.build.core.buildArtefactType";

	/**
	 * UMLRTS_ROOT variable name.
	 */
	private static final String UMLRTS_ROOT_VAR = "UMLRTS_ROOT";

	/** Project creation error message. */
	private static final String FAILED_TO_CREATE_OUTPUT_PROJECT = "Failed to create output project";

	/**
	 * Constructor.
	 *
	 */
	private ProjectGenerator() {
	}

	/**
	 * Get or create CDT project.
	 * 
	 * @param projectName
	 *            project name
	 * @param progressMonitor
	 *            progress monitor
	 * @return created project
	 */
	public static IProject getOrCreateCPPProject(String projectName, IProgressMonitor progressMonitor) {

		IProgressMonitor monitor = progressMonitor;
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject(projectName);

		if (project.exists()) {
			if (!project.isOpen()) {
				try {
					project.open(monitor);
				} catch (CoreException e) {
					project = null;
				}
			}
		} else {
			try {
				IProjectDescription description = workspace.newProjectDescription(projectName);
				project = CCorePlugin.getDefault().createCDTProject(description, project, monitor);
						
				setupCPPProject(project, monitor);

				String rtsRoot = getUMLRTSRootEnv();
				addIncludePath(project.getName(), findIncludeDirs(rtsRoot));	
				
				addExtraSourcePath(project.getName(), "RTS", 
						new Path(rtsRoot), 						
						new Path("os/windows"),
						new Path("os/stub"));
				
				addLibraries(project.getName(), "pthread");

				addTarget(project, "all");
				addTarget(project, "clean");
				addLaunchConfiguration(project.getName());
				
				// disable indexing
				ICModel cModel = CoreModel.create(ResourcesPlugin.getWorkspace().getRoot());
				ICProject cProject = cModel.getCProject(project.getName());
				CCorePlugin.getIndexManager().setIndexerId(cProject, IPDOMManager.ID_NO_INDEXER);

				// disable code analysis
				IProblemProfile profile = CodanRuntime.getInstance().getCheckersRegistry()
						.getResourceProfileWorkingCopy(project);
				
				IProblem[] problems = profile.getProblems();
				for (int i = 0; i < problems.length; i++) {
					IProblem p = problems[i];
					((CodanProblem) p).setEnabled(false);
				}

				CodanRuntime.getInstance().getCheckersRegistry().updateProfile(project, profile);
				
			} catch (CoreException | BuildException e) {
				CodeGenPlugin.error(e);
				project = null;
			}
		}

		return project;
	}

	private static Collection<String> findIncludeDirs(String rtsRoot) {
		Collection<String> dirs = new HashSet<>();
		LinkedList<java.nio.file.Path> files = new LinkedList<>();
	    try {
			Files.find(Paths.get(rtsRoot), 999, (p, bfa) -> bfa.isRegularFile() && 
					(p.getFileName().toString().matches(".*\\.h")
							|| p.getFileName().toString().matches(".*\\.hh")
							|| p.getFileName().toString().matches(".*\\.hpp"))).forEach(files::add);
			for(java.nio.file.Path path : files)
	    			dirs.add(path.getParent().toFile().getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dirs;
	}
	
	/**
	 * Setup CPP project.
	 * 
	 * @param project
	 *            project to setup
	 * @param monitor
	 *            progress monitor
	 * @throws CoreException
	 */
	private static void setupCPPProject(IProject project, IProgressMonitor monitor) throws CoreException {

		if (!project.hasNature(CCProjectNature.CC_NATURE_ID)) {
			CCProjectNature.addCCNature(project, monitor);
		}
		MakeProjectNature.addNature(project, monitor);
		UMLRTCCNature.addUMLRTCCNature(project, monitor);

		java.util.Optional<IToolChain> tc;
		String os = System.getProperty("os.name").toLowerCase();
		if (os.startsWith("win")) {
			tc = getToolChains().stream().filter(t -> "cdt.managedbuild.toolchain.gnu.cygwin.exe.debug".equals(t.getId())).findFirst();
		} else if (os.startsWith("mac")) {
			tc = getToolChains().stream().filter(t -> "cdt.managedbuild.toolchain.gnu.macosx.exe.debug".equals(t.getId())).findFirst();
		} else {
			tc = getToolChains().stream().filter(t -> "cdt.managedbuild.toolchain.gnu.exe.debug".equals(t.getId())).findFirst();
		}
		if (tc.isPresent()) {
			ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
			ICProjectDescription desc = mngr.createProjectDescription(project, false);
			ManagedBuildInfo info = ManagedBuildManager.createBuildInfo(project);
			IConfiguration[] configurations = ManagedBuildManager.getExtensionConfigurations(tc.get(), ARTIFACT, "org.eclipse.cdt.build.core.buildArtefactType.exe");
			ManagedProject mProj = new ManagedProject(project, configurations[0].getProjectType());
			info.setManagedProject(mProj);
			for (IConfiguration cfg : configurations) {
				Configuration cf = (Configuration) cfg;
				String id = ManagedBuildManager.calculateChildId(cf.getId(), null);
				Configuration config = new Configuration(mProj, cf, id, false, true);
				CConfigurationData data = config.getConfigurationData();
				ICConfigurationDescription cfgDes = desc.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);
				config.setConfigurationDescription(cfgDes);
				config.exportArtifactInfo();

				IBuilder bld = config.getEditableBuilder();
				if (bld != null) {
					bld.setManagedBuildOn(true);
				}

				config.setName(cfg.getName());
				config.setArtifactName(mProj.getDefaultArtifactName());
				
				if(os.startsWith("mac")) {
					ITool[] tools = tc.get().getToolsBySuperClassId("cdt.managedbuild.tool.gnu.cpp.compiler.macosx.exe.debug");
					for(ITool tool : tools) {
						IOption option = tool.getOptionBySuperClassId("gnu.cpp.compiler.option.other.other");
						ManagedBuildManager.setOption(cfg, tool, option, "-c -fmessage-length=0 -DNEED_PTHREAD_MUTEX_TIMEDLOCK -DNEED_SEM_TIMEDWAIT");
					}
				}
				
				if(os.startsWith("win")) {
					try {
						Bundle bundle = Platform.getBundle("ca.queensu.cs.mase.papyrusrt.buildfix.cygwin");
						Path path = new Path("cygwin64");
						path.append("bin");
						URL cygwinURL = FileLocator.find(bundle, path, null);
						if(cygwinURL != null) {
							File cygwinFolder = new File(cygwinURL.toURI());
							IContributedEnvironment environment = CCorePlugin.getDefault().getBuildEnvironmentManager().getContributedEnvironment();
							environment.addVariable("PATH", cygwinFolder.getAbsolutePath(), IEnvironmentVariable.ENVVAR_REPLACE, null, cfgDes);
						}
						
					} catch(MissingResourceException | URISyntaxException  e) {
						
					}
				}
			}
			
			mngr.setProjectDescription(project, desc);

		} else {
			setupDefaultCDTProject(project, monitor);
		}
	}

	/**
	 * Create default CDT project without toolchain.
	 * 
	 * @param project
	 *            project
	 * @param monitor
	 *            monitor
	 * @throws CoreException
	 */
	private static void setupDefaultCDTProject(IProject project, IProgressMonitor monitor) throws CoreException {

		ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
		ICProjectDescription desc = mngr.createProjectDescription(project, false);
		ManagedBuildInfo info = ManagedBuildManager.createBuildInfo(project);
		ManagedProject mProj = new ManagedProject(desc);
		info.setManagedProject(mProj);
		Configuration cfg = new Configuration(mProj, null, ManagedBuildManager.calculateChildId("0", null), "Default");
		IBuilder bld = cfg.getEditableBuilder();
		if (bld != null) {
			if (bld.isInternalBuilder()) {
				IConfiguration prefCfg = ManagedBuildManager.getPreferenceConfiguration(false);
				IBuilder prefBuilder = prefCfg.getBuilder();
				cfg.changeBuilder(prefBuilder, ManagedBuildManager.calculateChildId(cfg.getId(), null), prefBuilder.getName());
				bld = cfg.getEditableBuilder();
				bld.setBuildPath(null);
			}
			bld.setManagedBuildOn(false);
		} else {
			CodeGenPlugin.error(FAILED_TO_CREATE_OUTPUT_PROJECT);
		}
		cfg.setArtifactName(mProj.getDefaultArtifactName());
		CConfigurationData data = cfg.getConfigurationData();
		desc.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);
		mngr.setProjectDescription(project, desc);
	}

	/**
	 * Get available tool chains.
	 * 
	 * @return tool chains.
	 */
	private static List<IToolChain> getToolChains() {
		IBuildPropertyManager bpm = ManagedBuildManager.getBuildPropertyManager();
		IBuildPropertyType bpt = bpm.getPropertyType(ARTIFACT);
		IBuildPropertyValue[] vs = bpt.getSupportedValues();
		Arrays.sort(vs, BuildListComparator.getInstance());

		List<IToolChain> toolchains = new ArrayList<>();
		// new style project types
		for (int i = 0; i < vs.length; i++) {
			IToolChain[] tcs = ManagedBuildManager.getExtensionsToolChains(ARTIFACT, vs[i].getId(), false);
			if (tcs == null || tcs.length == 0) {
				continue;
			}
			toolchains.addAll(Stream.of(tcs).filter(tc -> ManagedBuildManager.isPlatformOk(tc)).collect(Collectors.toList()));
		}

		// old style project types
		SortedMap<String, IProjectType> sm = ManagedBuildManager.getExtensionProjectTypeMap();
		for (String s : sm.keySet()) {
			IProjectType pt = sm.get(s);
			if (pt.isAbstract() || pt.isSystemObject()) {
				continue;
			}
			String nattr = pt.getNameAttribute();
			if (nattr == null || nattr.length() == 0) {
				continue;
			}

			IToolChain[] tcs = ManagedBuildManager.getExtensionToolChains(pt);
			toolchains.addAll(Stream.of(tcs).filter(tc -> ManagedBuildManager.isPlatformOk(tc)).collect(Collectors.toList()));
		}
		return toolchains;
	}

	/**
	 * Add make target.
	 * 
	 * @param project
	 *            project
	 * @param targetName
	 *            target name
	 * @throws CoreException
	 */
	public static void addTarget(IProject project, String targetName) throws CoreException {
		IMakeTargetManager manager = MakeCorePlugin.getDefault().getTargetManager();
		IMakeTarget newTarget = manager.createTarget(project, targetName,
				TARGET_BUILDER_ID);
		newTarget.setStopOnError(true);
		newTarget.setRunAllBuilders(false);
		newTarget.setBuildAttribute(IMakeTarget.BUILD_TARGET, targetName);
		newTarget.setUseDefaultBuildCmd(true);
		
		// get current target
		IContainer container = project.getFolder("src");
		IMakeTarget target = manager.findTarget(container, targetName);
		if (target == null) {
			manager.addTarget(container, newTarget);
		}
	}

	/**
	 * Get UMLRTS_ROOT directory.
	 * 
	 * @return UMLRTS_ROOT
	 */
	public static String getUMLRTSRootEnv() {
		String result = "";
		IEnvironmentVariable var = CCorePlugin.getDefault().getBuildEnvironmentManager().getContributedEnvironment().getVariable(UMLRTS_ROOT_VAR, null);
		if (var != null) {
			result = var.getValue();
		}
		if (UMLUtil.isEmpty(result)) {
			String rtsroot = System.getenv(UMLRTS_ROOT_VAR);
			if (UMLUtil.isEmpty(rtsroot)) {
				result = UMLRTSUtil.getRTSRootPath();
			} else {
				result = "${UMLRTS_ROOT}";
			}
		}
		return result;
	}

	/**
	 * Get RTS library directory.
	 * 
	 * @return RTS library path
	 */
	public static String getRTSDirectory() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.startsWith("mac"))
			return "lib" + File.separator + "linux.darwin";
		else if (os.startsWith("win"))
			return "lib" + File.separator + "linux.cygwin";
		return "lib" + File.separator + "linux.x86-gcc-4.6.3";
	}

	/**
	 * Add include paths to RTS include.
	 * 
	 * @param targetProjectName
	 *            project
	 * @param paths
	 *            list of actual paths
	 * @throws CoreException
	 */
	public static void addIncludePath(String targetProjectName, Collection<String> paths) throws CoreException {
		ICModel cModel = CoreModel.create(ResourcesPlugin.getWorkspace().getRoot());
		ICProject cProject = cModel.getCProject(targetProjectName);
		if (!cProject.exists()) {
			return;
		}

		if (CoreModel.getDefault().isNewStyleProject(cProject.getProject())) {
			final IProject project = cProject.getProject();

			final ICProjectDescription projectDescription = CoreModel.getDefault().getProjectDescription(project, true);
			final ICConfigurationDescription[] configurations = projectDescription.getConfigurations();
			for (ICConfigurationDescription config : configurations) {
				final ICLanguageSetting[] languageSettings = config.getRootFolderDescription().getLanguageSettings();
				for (ICLanguageSetting langSetting : languageSettings) {
					if (Arrays.asList(langSetting.getSourceContentTypeIds()).contains("org.eclipse.cdt.core.cxxSource")) {
						final ICLanguageSettingEntry[] entries = langSetting.getSettingEntries(ICSettingEntry.INCLUDE_PATH);
						final List<ICLanguageSettingEntry> list = new ArrayList<>(
								Arrays.asList(entries));
						
						for(String path : paths)
							list.add(CDataUtil.createCIncludePathEntry(path, ICSettingEntry.INCLUDE_PATH | ICSettingEntry.READONLY));
						
						langSetting.setSettingEntries(ICSettingEntry.INCLUDE_PATH,
								list.toArray(new ICLanguageSettingEntry[list.size()]));
					}
				}
			}
			
			CoreModel.getDefault().setProjectDescription(project, projectDescription);
		}
	}
	
	/**
	 * Add extra source folder to project.
	 * 
	 * @param targetProjectName
	 *            project
	 * @param folderName
	 *            project linked folder name
	 * @param sourcePath
	 *            actual path to source folder
	 * @param excludePaths
	 * 			  paths to exclude relative to sourcePath
	 * @throws CoreException
	 */
	public static void addExtraSourcePath(String targetProjectName, String folderName, IPath sourcePath, IPath ... excludePaths) throws CoreException {
		ICModel cModel = CoreModel.create(ResourcesPlugin.getWorkspace().getRoot());
		ICProject cProject = cModel.getCProject(targetProjectName);
		if (!cProject.exists()) {
			return;
		}

		if (CoreModel.getDefault().isNewStyleProject(cProject.getProject())) {
			final IProject project = cProject.getProject();
			final ICProjectDescription projectDescription = CoreModel.getDefault().getProjectDescription(project, true);
	        ICConfigurationDescription activeConfig = projectDescription.getActiveConfiguration();
	        ICSourceEntry[] sourceEntries = activeConfig.getSourceEntries();
	        List<ICSourceEntry> sourceEntriesList = new ArrayList<>();
	        
	        // link local folder to system source folder
	        IFolder folder = project.getFolder(folderName);
	        folder.createLink(sourcePath, IResource.NONE, null);
	        
	        // exclude folderName from main source entry
	        ICSourceEntry mainEntry = sourceEntries[0];
        		List<IPath> mainEntryExclusionPatterns = new ArrayList<>(Arrays.asList(mainEntry.getExclusionPatterns()));
        		mainEntryExclusionPatterns.add(new Path(folderName));
        		sourceEntriesList.add(new CSourceEntry(mainEntry.getFullPath(), mainEntryExclusionPatterns.toArray(
        				new IPath[mainEntryExclusionPatterns.size()]), mainEntry.getFlags()));
	        
        		// add new entry
	        CSourceEntry newEntry = new CSourceEntry(folderName, excludePaths, ICSourceEntry.SOURCE_PATH|ICSourceEntry.RESOLVED);
	        sourceEntriesList.add(newEntry);
	        
	        activeConfig.setSourceEntries(sourceEntriesList.toArray(new CSourceEntry[sourceEntriesList.size()]));
	        projectDescription.setActiveConfiguration(activeConfig);
			CoreModel.getDefault().setProjectDescription(project, projectDescription);
		}
	}

	/**
	 * Add library path to RTS library.
	 * 
	 * @param targetProjectName
	 *            project
	 * @param basePath
	 *            base path
	 * @param path
	 *            actual path
	 * @throws CoreException
	 * @throws BuildException
	 */
	public static void addLibraryPath(String targetProjectName, String basePath, String path) throws CoreException, BuildException {
		ICModel cModel = CoreModel.create(ResourcesPlugin.getWorkspace().getRoot());
		ICProject cProject = cModel.getCProject(targetProjectName);
		if (!cProject.exists()) {
			return;
		}

		if (CoreModel.getDefault().isNewStyleProject(cProject.getProject())) {
			final IProject project = cProject.getProject();
			final String wsPath = new Path(basePath).makeAbsolute().append(path).toString();

			ICProjectDescription projectDescription = CoreModel.getDefault().getProjectDescription(project, true);
			ICConfigurationDescription[] configurationDescriptions = projectDescription.getConfigurations();
			IConfiguration[] configurations = new IConfiguration[configurationDescriptions.length];

			for (int i = 0; i < configurationDescriptions.length; i++) {
				ICConfigurationDescription configurationDescription = configurationDescriptions[i];
				configurations[i] = ManagedBuildManager.getConfigurationForDescription(configurationDescription);

				for (ITool tool : configurations[i].getFilteredTools()) {
					IOption[] options = tool.getOptions();
					for (IOption option : options) {
						if (option.getValueType() == IOption.LIBRARY_PATHS)
							ManagedBuildManager.setOption(configurations[i], tool, option, new String[] { '"' + wsPath + '"' });
					}
				}
			}
			
			CoreModel.getDefault().setProjectDescription(project, projectDescription);
		}
	}

	/**
	 * Add libraries as dependencies.
	 * 
	 * @param targetProjectName
	 *            project
	 * @param libraries
	 *            list of libraries
	 * @throws CoreException
	 * @throws BuildException
	 */
	public static void addLibraries(String targetProjectName, String... libraries) throws CoreException, BuildException {
		ICModel cModel = CoreModel.create(ResourcesPlugin.getWorkspace().getRoot());
		ICProject cProject = cModel.getCProject(targetProjectName);
		if (!cProject.exists()) {
			return;
		}

		if (CoreModel.getDefault().isNewStyleProject(cProject.getProject())) {
			final IProject project = cProject.getProject();

			ICProjectDescription projectDescription = CoreModel.getDefault().getProjectDescription(project, true);
			ICConfigurationDescription[] configurationDescriptions = projectDescription.getConfigurations();
			IConfiguration[] configurations = new IConfiguration[configurationDescriptions.length];

			for (int i = 0; i < configurationDescriptions.length; i++) {
				ICConfigurationDescription configurationDescription = configurationDescriptions[i];
				configurations[i] = ManagedBuildManager.getConfigurationForDescription(configurationDescription);

				for (ITool tool : configurations[i].getFilteredTools()) {
					IOption[] options = tool.getOptions();
					for (IOption option : options) {
						if (option.getValueType() == IOption.LIBRARIES)
							ManagedBuildManager.setOption(configurations[i], tool, option, libraries);
					}
				}
			}

			CoreModel.getDefault().setProjectDescription(project, projectDescription);
		}
	}

	/**
	 * Create a launch configuration
	 * 
	 * @param targetProjectName
	 *            project
	 * @throws CoreException
	 */
	public static void addLaunchConfiguration(String targetProjectName) throws CoreException {
		ICModel cModel = CoreModel.create(ResourcesPlugin.getWorkspace().getRoot());
		ICProject cProject = cModel.getCProject(targetProjectName);
		if (!cProject.exists()) {
			return;
		}

		String os = System.getProperty("os.name").toLowerCase();
		String binaryExtention = os.startsWith("win") ? ".exe" : "";

		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType("org.eclipse.cdt.launch.applicationLaunchType");
		for(ILaunchConfiguration conf : manager.getLaunchConfigurations(type)) {
			if(conf.getName().equals(targetProjectName)) {
				conf.delete();
				break;
			}
		}
		
		ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(cProject.getProject(), targetProjectName);
		workingCopy.setMappedResources(new IResource[] {cProject.getResource(), cProject.getProject()});
		workingCopy.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, targetProjectName);
		workingCopy.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, "Debug" + File.separator + targetProjectName + binaryExtention);
		workingCopy.doSave();
	}
}

