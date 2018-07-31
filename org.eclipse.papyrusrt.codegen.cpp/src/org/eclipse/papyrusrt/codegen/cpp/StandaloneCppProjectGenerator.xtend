/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.URI
import org.eclipse.papyrusrt.xtumlrt.external.ExternalPackageManager
import org.eclipse.uml2.uml.util.UMLUtil

class StandaloneCppProjectGenerator {
	
	private static final String RTS_PLUGIN_ID = "org.eclipse.papyrusrt.rts"
	private static String UMLRTS_ROOT = ""
	
	def private boolean generate(String path, String projectName, boolean regenerate) {
		
		val project = new File(new File(path), projectName)
		if(project.exists){
			if(!project.isDirectory){
				return false;
			}
		}else{
			val result = project.mkdirs()
			if(!result){
				// failed to create directory
				return result
			}			
		}		
		
		// generate .project metadata
		val projectFile = new File(project, ".project")
		if(!projectFile.exists || regenerate){
			val writer = new BufferedWriter(new FileWriter(projectFile));
			writer.write(generateProjectMetadata(projectName).toString)
			writer.close
		}
		
		if(UMLUtil.isEmpty(UMLRTS_ROOT)){
			var rtsroot = System.getenv("UMLRTS_ROOT");
			if(UMLUtil.isEmpty(rtsroot)){
				val loc = ExternalPackageManager.getInstance().getPluginFinder().get(RTS_PLUGIN_ID)
				if(loc != null){
					val uri = URI.create(loc)
					UMLRTS_ROOT = uri.path + "/umlrts"
				}
			}else{
				UMLRTS_ROOT = "${UMLRTS_ROOT}"
			}
		}
				
		// generate .cproject metadata
		val cprojectFile = new File(project, ".cproject")
		if(!cprojectFile.exists || regenerate){
			val writer = new BufferedWriter(new FileWriter(cprojectFile));
			if(System.getProperty("os.name").toLowerCase.startsWith("win")){
				writer.write(generateCProjectMetadataForCygwin(projectName).toString)
			}else{
				writer.write(generateCProjectMetadata(projectName).toString)
			}
			writer.close
		}
		
		return true
	}
	
	// generate project
	def boolean generate(String path, String projectName) {
		return path.generate(projectName, false)
	
	}
	
	// Regenerate meta data
	def boolean regenerateMetadata(String path, String projectName){
		return path.generate(projectName, true)
	}
	
	def private generateProjectMetadata(String projectName){
		'''<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
	<name>«projectName»</name>
	<comment></comment>
	<projects>
	</projects>
	<buildSpec>
		<buildCommand>
			<name>org.eclipse.papyrusrt.codegen.umlrtgensrcbuilder</name>
			<arguments>
			</arguments>
		</buildCommand>	
		<buildCommand>
			<name>org.eclipse.cdt.managedbuilder.core.genmakebuilder</name>
			<triggers>clean,full,incremental,</triggers>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>org.eclipse.cdt.managedbuilder.core.ScannerConfigBuilder</name>
			<triggers>full,incremental,</triggers>
			<arguments>
			</arguments>
		</buildCommand>
	</buildSpec>
	<natures>
		<nature>org.eclipse.cdt.core.cnature</nature>
		<nature>org.eclipse.cdt.core.ccnature</nature>
		<nature>org.eclipse.cdt.managedbuilder.core.managedBuildNature</nature>
		<nature>org.eclipse.cdt.managedbuilder.core.ScannerConfigNature</nature>
	</natures>
</projectDescription>
'''
	}
	
	def private generateCProjectMetadata(String projectName){
	    '''<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<?fileVersion 4.0.0?><cproject storage_type_id="org.eclipse.cdt.core.XmlProjectDescriptionStorage">
    <storageModule moduleId="org.eclipse.cdt.core.settings">
        <cconfiguration id="cdt.managedbuild.toolchain.gnu.base.4997491962">
            <storageModule moduleId="cdtBuildSystem" version="4.0.0">
                <configuration artifactName="${ProjName}" buildProperties="" id="cdt.managedbuild.toolchain.gnu.base.4997491961" name="Default" parent="org.eclipse.cdt.build.core.emptycfg">
                    <folderInfo id="cdt.managedbuild.toolchain.gnu.base.8320418219" name="/" resourcePath="">
                        <toolChain id="cdt.managedbuild.toolchain.gnu.base.9623027664" name="Linux GCC" superClass="cdt.managedbuild.toolchain.gnu.base">
                            <targetPlatform archList="all" binaryParser="org.eclipse.cdt.core.ELF" id="cdt.managedbuild.target.gnu.platform.base.1127549341" name="Debug Platform" osList="linux,hpux,aix,qnx" superClass="cdt.managedbuild.target.gnu.platform.base"/>
                            <builder id="cdt.managedbuild.target.gnu.builder.base.6925958497" managedBuildOn="false" name="Gnu Make Builder.Default" superClass="cdt.managedbuild.target.gnu.builder.base"/>
                            <tool id="cdt.managedbuild.tool.gnu.archiver.base.4527526645" name="GCC Archiver" superClass="cdt.managedbuild.tool.gnu.archiver.base"/>
                            <tool id="cdt.managedbuild.tool.gnu.cpp.compiler.base.1689764086" name="GCC C++ Compiler" superClass="cdt.managedbuild.tool.gnu.cpp.compiler.base">
                                «IF UMLRTS_ROOT.length != 0»
                                <option id="gnu.cpp.compiler.option.include.paths.1773497216" name="Include paths (-I)" superClass="gnu.cpp.compiler.option.include.paths" valueType="includePath">
                                    <listOptionValue builtIn="false" value="&quot;«UMLRTS_ROOT»/include&quot;"/>
                                </option>
                                «ENDIF»
                                <inputType id="cdt.managedbuild.tool.gnu.cpp.compiler.input.1003716063" superClass="cdt.managedbuild.tool.gnu.cpp.compiler.input"/>
                            </tool>
                            <tool id="cdt.managedbuild.tool.gnu.c.compiler.base.8030371610" name="GCC C Compiler" superClass="cdt.managedbuild.tool.gnu.c.compiler.base">
                                <inputType id="cdt.managedbuild.tool.gnu.c.compiler.input.793805426" superClass="cdt.managedbuild.tool.gnu.c.compiler.input"/>
                            </tool>
                            <tool id="cdt.managedbuild.tool.gnu.c.linker.base.2851231432" name="GCC C Linker" superClass="cdt.managedbuild.tool.gnu.c.linker.base"/>
                            <tool id="cdt.managedbuild.tool.gnu.cpp.linker.base.1578285699" name="GCC C++ Linker" superClass="cdt.managedbuild.tool.gnu.cpp.linker.base">
                                <inputType id="cdt.managedbuild.tool.gnu.cpp.linker.input.540722823" superClass="cdt.managedbuild.tool.gnu.cpp.linker.input">
                                    <additionalInput kind="additionalinputdependency" paths="$(USER_OBJS)"/>
                                    <additionalInput kind="additionalinput" paths="$(LIBS)"/>
                                </inputType>
                            </tool>
                            <tool id="cdt.managedbuild.tool.gnu.assembler.base.1716199300" name="GCC Assembler" superClass="cdt.managedbuild.tool.gnu.assembler.base">
                                <inputType id="cdt.managedbuild.tool.gnu.assembler.input.345505909" superClass="cdt.managedbuild.tool.gnu.assembler.input"/>
                            </tool>
                        </toolChain>
                    </folderInfo>
                </configuration>
            </storageModule>
            <storageModule buildSystemId="org.eclipse.cdt.managedbuilder.core.configurationDataProvider" id="cdt.managedbuild.toolchain.gnu.base.4997491961" moduleId="org.eclipse.cdt.core.settings" name="Default">
                <externalSettings/>
                <extensions>
                    <extension id="org.eclipse.cdt.core.GmakeErrorParser" point="org.eclipse.cdt.core.ErrorParser"/>
                    <extension id="org.eclipse.cdt.core.CWDLocator" point="org.eclipse.cdt.core.ErrorParser"/>
                    <extension id="org.eclipse.cdt.core.GCCErrorParser" point="org.eclipse.cdt.core.ErrorParser"/>
                    <extension id="org.eclipse.cdt.core.GASErrorParser" point="org.eclipse.cdt.core.ErrorParser"/>
                    <extension id="org.eclipse.cdt.core.GLDErrorParser" point="org.eclipse.cdt.core.ErrorParser"/>
                    <extension id="org.eclipse.cdt.core.ELF" point="org.eclipse.cdt.core.BinaryParser"/>
                </extensions>
            </storageModule>
            <storageModule moduleId="org.eclipse.cdt.core.externalSettings"/>
        </cconfiguration>
    </storageModule>
    <storageModule moduleId="cdtBuildSystem" version="4.0.0">
        <project id="«projectName».cdt.1970989858" name="«projectName»"/>
    </storageModule>
    <storageModule moduleId="scannerConfiguration">
        <autodiscovery enabled="true" problemReportingEnabled="true" selectedProfileId=""/>
    </storageModule>
    <storageModule moduleId="org.eclipse.cdt.core.LanguageSettingsProviders"/>
    <storageModule moduleId="org.eclipse.cdt.make.core.buildtargets">
        <buildTargets>
            <target name="all" path="src" targetID="org.eclipse.cdt.build.MakeTargetBuilder">
                <buildCommand>make</buildCommand>
                <buildArguments/>
                <buildTarget>all</buildTarget>
                <stopOnError>true</stopOnError>
                <useDefaultCommand>true</useDefaultCommand>
                <runAllBuilders>true</runAllBuilders>
            </target>
            <target name="clean" path="src" targetID="org.eclipse.cdt.build.MakeTargetBuilder">
                <buildCommand>make</buildCommand>
                <buildArguments/>
                <buildTarget>clean</buildTarget>
                <stopOnError>true</stopOnError>
                <useDefaultCommand>true</useDefaultCommand>
                <runAllBuilders>true</runAllBuilders>
            </target>
            <target name="info" path="src" targetID="org.eclipse.cdt.build.MakeTargetBuilder">
                <buildCommand>make</buildCommand>
                <buildArguments/>
                <buildTarget>info</buildTarget>
                <stopOnError>true</stopOnError>
                <useDefaultCommand>true</useDefaultCommand>
                <runAllBuilders>true</runAllBuilders>
            </target>            
        </buildTargets>
    </storageModule>
</cproject>
'''
	}
	
	def private generateCProjectMetadataForCygwin(String projectName){
	    '''<?xml version="1.0" encoding="UTF-8" standalone="no"?>
	    <?fileVersion 4.0.0?><cproject storage_type_id="org.eclipse.cdt.core.XmlProjectDescriptionStorage">
	    	<storageModule moduleId="org.eclipse.cdt.core.settings">
	    		<cconfiguration id="cdt.managedbuild.config.gnu.cygwin.exe.debug.980520581">
	    			<storageModule buildSystemId="org.eclipse.cdt.managedbuilder.core.configurationDataProvider" id="cdt.managedbuild.config.gnu.cygwin.exe.debug.980520581" moduleId="org.eclipse.cdt.core.settings" name="Debug">
	    				<externalSettings/>
	    				<extensions>
	    					<extension id="org.eclipse.cdt.core.Cygwin_PE" point="org.eclipse.cdt.core.BinaryParser"/>
	    					<extension id="org.eclipse.cdt.core.GASErrorParser" point="org.eclipse.cdt.core.ErrorParser"/>
	    					<extension id="org.eclipse.cdt.core.GmakeErrorParser" point="org.eclipse.cdt.core.ErrorParser"/>
	    					<extension id="org.eclipse.cdt.core.GLDErrorParser" point="org.eclipse.cdt.core.ErrorParser"/>
	    					<extension id="org.eclipse.cdt.core.CWDLocator" point="org.eclipse.cdt.core.ErrorParser"/>
	    					<extension id="org.eclipse.cdt.core.GCCErrorParser" point="org.eclipse.cdt.core.ErrorParser"/>
	    				</extensions>
	    			</storageModule>
	    			<storageModule moduleId="cdtBuildSystem" version="4.0.0">
	    				<configuration artifactName="${ProjName}" buildArtefactType="org.eclipse.cdt.build.core.buildArtefactType.exe" buildProperties="org.eclipse.cdt.build.core.buildArtefactType=org.eclipse.cdt.build.core.buildArtefactType.exe,org.eclipse.cdt.build.core.buildType=org.eclipse.cdt.build.core.buildType.debug" cleanCommand="rm -rf" description="" id="cdt.managedbuild.config.gnu.cygwin.exe.debug.980520581" name="Debug" parent="cdt.managedbuild.config.gnu.cygwin.exe.debug">
	    					<folderInfo id="cdt.managedbuild.config.gnu.cygwin.exe.debug.980520581." name="/" resourcePath="">
	    						<toolChain id="cdt.managedbuild.toolchain.gnu.cygwin.exe.debug.245506467" name="Cygwin GCC" superClass="cdt.managedbuild.toolchain.gnu.cygwin.exe.debug">
	    							<targetPlatform id="cdt.managedbuild.target.gnu.platform.cygwin.exe.debug.680331407" name="Debug Platform" superClass="cdt.managedbuild.target.gnu.platform.cygwin.exe.debug"/>
	    							<builder buildPath="${workspace_loc:/c}/Debug" id="cdt.managedbuild.target.gnu.builder.cygwin.exe.debug.1123165732" keepEnvironmentInBuildfile="false" managedBuildOn="true" name="Gnu Make Builder" superClass="cdt.managedbuild.target.gnu.builder.cygwin.exe.debug"/>
	    							<tool id="cdt.managedbuild.tool.gnu.assembler.cygwin.exe.debug.355408502" name="GCC Assembler" superClass="cdt.managedbuild.tool.gnu.assembler.cygwin.exe.debug">
									«IF UMLRTS_ROOT.length != 0»
									<option id="gnu.both.asm.option.include.paths.1307501777" superClass="gnu.both.asm.option.include.paths" valueType="includePath">
										<listOptionValue builtIn="false" value="&quot;«UMLRTS_ROOT»\include&quot;"/>
									</option>
									«ENDIF»
	    								<inputType id="cdt.managedbuild.tool.gnu.assembler.input.1899783247" superClass="cdt.managedbuild.tool.gnu.assembler.input"/>
	    							</tool>
	    							<tool id="cdt.managedbuild.tool.gnu.archiver.cygwin.base.1540805693" name="GCC Archiver" superClass="cdt.managedbuild.tool.gnu.archiver.cygwin.base"/>
	    							<tool id="cdt.managedbuild.tool.gnu.cpp.compiler.cygwin.exe.debug.679549339" name="Cygwin C++ Compiler" superClass="cdt.managedbuild.tool.gnu.cpp.compiler.cygwin.exe.debug">
	    								<option id="gnu.cpp.compiler.cygwin.exe.debug.option.optimization.level.655345258" name="Optimization Level" superClass="gnu.cpp.compiler.cygwin.exe.debug.option.optimization.level" useByScannerDiscovery="false" value="gnu.cpp.compiler.optimization.level.none" valueType="enumerated"/>
	    								<option id="gnu.cpp.compiler.cygwin.exe.debug.option.debugging.level.1250786344" name="Debug Level" superClass="gnu.cpp.compiler.cygwin.exe.debug.option.debugging.level" useByScannerDiscovery="false" value="gnu.cpp.compiler.debugging.level.max" valueType="enumerated"/>
										«IF UMLRTS_ROOT.length != 0»
										<option id="gnu.cpp.compiler.option.include.paths.1678157794" superClass="gnu.cpp.compiler.option.include.paths" valueType="includePath">
											<listOptionValue builtIn="false" value="&quot;«UMLRTS_ROOT»\include&quot;"/>
										</option>
										«ENDIF»
	    								<inputType id="cdt.managedbuild.tool.gnu.cpp.compiler.input.cygwin.112150320" superClass="cdt.managedbuild.tool.gnu.cpp.compiler.input.cygwin"/>
	    							</tool>
	    							<tool id="cdt.managedbuild.tool.gnu.c.compiler.cygwin.exe.debug.1425344114" name="Cygwin C Compiler" superClass="cdt.managedbuild.tool.gnu.c.compiler.cygwin.exe.debug">
	    								<option defaultValue="gnu.c.optimization.level.none" id="gnu.c.compiler.cygwin.exe.debug.option.optimization.level.1196154684" name="Optimization Level" superClass="gnu.c.compiler.cygwin.exe.debug.option.optimization.level" useByScannerDiscovery="false" valueType="enumerated"/>
	    								<option id="gnu.c.compiler.cygwin.exe.debug.option.debugging.level.731657235" name="Debug Level" superClass="gnu.c.compiler.cygwin.exe.debug.option.debugging.level" useByScannerDiscovery="false" value="gnu.c.debugging.level.max" valueType="enumerated"/>
										«IF UMLRTS_ROOT.length != 0»	    								
										<option id="gnu.c.compiler.option.include.paths.1712091319" superClass="gnu.c.compiler.option.include.paths" valueType="includePath">
											<listOptionValue builtIn="false" value="&quot;«UMLRTS_ROOT»\include&quot;"/>
										</option>
										«ENDIF»
	    								<inputType id="cdt.managedbuild.tool.gnu.c.compiler.input.cygwin.203106561" superClass="cdt.managedbuild.tool.gnu.c.compiler.input.cygwin"/>
	    							</tool>
	    							<tool id="cdt.managedbuild.tool.gnu.c.linker.cygwin.exe.debug.1613601859" name="Cygwin C Linker" superClass="cdt.managedbuild.tool.gnu.c.linker.cygwin.exe.debug"/>
	    							<tool id="cdt.managedbuild.tool.gnu.cpp.linker.cygwin.exe.debug.150791258" name="Cygwin C++ Linker" superClass="cdt.managedbuild.tool.gnu.cpp.linker.cygwin.exe.debug">
	    								<inputType id="cdt.managedbuild.tool.gnu.cpp.linker.input.2053989711" superClass="cdt.managedbuild.tool.gnu.cpp.linker.input">
	    									<additionalInput kind="additionalinputdependency" paths="$(USER_OBJS)"/>
	    									<additionalInput kind="additionalinput" paths="$(LIBS)"/>
	    								</inputType>
	    							</tool>
	    						</toolChain>
	    					</folderInfo>
	    				</configuration>
	    			</storageModule>
	    			<storageModule moduleId="org.eclipse.cdt.core.externalSettings"/>
	    		</cconfiguration>
	    		<cconfiguration id="cdt.managedbuild.config.gnu.cygwin.exe.release.830625860">
	    			<storageModule buildSystemId="org.eclipse.cdt.managedbuilder.core.configurationDataProvider" id="cdt.managedbuild.config.gnu.cygwin.exe.release.830625860" moduleId="org.eclipse.cdt.core.settings" name="Release">
	    				<externalSettings/>
	    				<extensions>
	    					<extension id="org.eclipse.cdt.core.Cygwin_PE" point="org.eclipse.cdt.core.BinaryParser"/>
	    					<extension id="org.eclipse.cdt.core.GASErrorParser" point="org.eclipse.cdt.core.ErrorParser"/>
	    					<extension id="org.eclipse.cdt.core.GmakeErrorParser" point="org.eclipse.cdt.core.ErrorParser"/>
	    					<extension id="org.eclipse.cdt.core.GLDErrorParser" point="org.eclipse.cdt.core.ErrorParser"/>
	    					<extension id="org.eclipse.cdt.core.CWDLocator" point="org.eclipse.cdt.core.ErrorParser"/>
	    					<extension id="org.eclipse.cdt.core.GCCErrorParser" point="org.eclipse.cdt.core.ErrorParser"/>
	    				</extensions>
	    			</storageModule>
	    			<storageModule moduleId="cdtBuildSystem" version="4.0.0">
	    				<configuration artifactName="${ProjName}" buildArtefactType="org.eclipse.cdt.build.core.buildArtefactType.exe" buildProperties="org.eclipse.cdt.build.core.buildArtefactType=org.eclipse.cdt.build.core.buildArtefactType.exe,org.eclipse.cdt.build.core.buildType=org.eclipse.cdt.build.core.buildType.release" cleanCommand="rm -rf" description="" id="cdt.managedbuild.config.gnu.cygwin.exe.release.830625860" name="Release" parent="cdt.managedbuild.config.gnu.cygwin.exe.release">
	    					<folderInfo id="cdt.managedbuild.config.gnu.cygwin.exe.release.830625860." name="/" resourcePath="">
	    						<toolChain id="cdt.managedbuild.toolchain.gnu.cygwin.exe.release.587807483" name="Cygwin GCC" superClass="cdt.managedbuild.toolchain.gnu.cygwin.exe.release">
	    							<targetPlatform id="cdt.managedbuild.target.gnu.platform.cygwin.exe.release.386480403" name="Debug Platform" superClass="cdt.managedbuild.target.gnu.platform.cygwin.exe.release"/>
	    							<builder buildPath="${workspace_loc:/c}/Release" id="cdt.managedbuild.target.gnu.builder.cygwin.exe.release.1650634844" keepEnvironmentInBuildfile="false" managedBuildOn="true" name="Gnu Make Builder" superClass="cdt.managedbuild.target.gnu.builder.cygwin.exe.release"/>
	    							<tool id="cdt.managedbuild.tool.gnu.assembler.cygwin.exe.release.733379197" name="GCC Assembler" superClass="cdt.managedbuild.tool.gnu.assembler.cygwin.exe.release">
										«IF UMLRTS_ROOT.length != 0»
										<option id="gnu.both.asm.option.include.paths.1632898777" superClass="gnu.both.asm.option.include.paths" valueType="includePath">
											<listOptionValue builtIn="false" value="&quot;«UMLRTS_ROOT»\include&quot;"/>
										</option>
										«ENDIF»
	    								<inputType id="cdt.managedbuild.tool.gnu.assembler.input.129880286" superClass="cdt.managedbuild.tool.gnu.assembler.input"/>
	    							</tool>
	    							<tool id="cdt.managedbuild.tool.gnu.archiver.cygwin.base.1151813552" name="GCC Archiver" superClass="cdt.managedbuild.tool.gnu.archiver.cygwin.base"/>
	    							<tool id="cdt.managedbuild.tool.gnu.cpp.compiler.cygwin.exe.release.728311195" name="Cygwin C++ Compiler" superClass="cdt.managedbuild.tool.gnu.cpp.compiler.cygwin.exe.release">
	    								<option id="gnu.cpp.compiler.cygwin.exe.release.option.optimization.level.883110365" name="Optimization Level" superClass="gnu.cpp.compiler.cygwin.exe.release.option.optimization.level" useByScannerDiscovery="false" value="gnu.cpp.compiler.optimization.level.most" valueType="enumerated"/>
	    								<option id="gnu.cpp.compiler.cygwin.exe.release.option.debugging.level.964253167" name="Debug Level" superClass="gnu.cpp.compiler.cygwin.exe.release.option.debugging.level" useByScannerDiscovery="false" value="gnu.cpp.compiler.debugging.level.none" valueType="enumerated"/>
										«IF UMLRTS_ROOT.length != 0»
										<option id="gnu.cpp.compiler.option.include.paths.983362243" superClass="gnu.cpp.compiler.option.include.paths" valueType="includePath">
											<listOptionValue builtIn="false" value="&quot;«UMLRTS_ROOT»\include&quot;"/>
										</option>
	    								«ENDIF»
	    								<inputType id="cdt.managedbuild.tool.gnu.cpp.compiler.input.cygwin.330030312" superClass="cdt.managedbuild.tool.gnu.cpp.compiler.input.cygwin"/>
	    							</tool>
	    							<tool id="cdt.managedbuild.tool.gnu.c.compiler.cygwin.exe.release.1838180696" name="Cygwin C Compiler" superClass="cdt.managedbuild.tool.gnu.c.compiler.cygwin.exe.release">
	    								<option defaultValue="gnu.c.optimization.level.most" id="gnu.c.compiler.cygwin.exe.release.option.optimization.level.1171465442" name="Optimization Level" superClass="gnu.c.compiler.cygwin.exe.release.option.optimization.level" useByScannerDiscovery="false" valueType="enumerated"/>
	    								<option id="gnu.c.compiler.cygwin.exe.release.option.debugging.level.1099610109" name="Debug Level" superClass="gnu.c.compiler.cygwin.exe.release.option.debugging.level" useByScannerDiscovery="false" value="gnu.c.debugging.level.none" valueType="enumerated"/>
	    								«IF UMLRTS_ROOT.length != 0»
	    								<option id="gnu.c.compiler.option.include.paths.1234491149" superClass="gnu.c.compiler.option.include.paths" valueType="includePath">
	    									<listOptionValue builtIn="false" value="&quot;«UMLRTS_ROOT»\include&quot;"/>
	    								</option>
	    								«ENDIF»
	    								<inputType id="cdt.managedbuild.tool.gnu.c.compiler.input.cygwin.492790812" superClass="cdt.managedbuild.tool.gnu.c.compiler.input.cygwin"/>
	    							</tool>
	    							<tool id="cdt.managedbuild.tool.gnu.c.linker.cygwin.exe.release.639933432" name="Cygwin C Linker" superClass="cdt.managedbuild.tool.gnu.c.linker.cygwin.exe.release"/>
	    							<tool id="cdt.managedbuild.tool.gnu.cpp.linker.cygwin.exe.release.943105356" name="Cygwin C++ Linker" superClass="cdt.managedbuild.tool.gnu.cpp.linker.cygwin.exe.release">
	    								<inputType id="cdt.managedbuild.tool.gnu.cpp.linker.input.1337288436" superClass="cdt.managedbuild.tool.gnu.cpp.linker.input">
	    									<additionalInput kind="additionalinputdependency" paths="$(USER_OBJS)"/>
	    									<additionalInput kind="additionalinput" paths="$(LIBS)"/>
	    								</inputType>
	    							</tool>
	    						</toolChain>
	    					</folderInfo>
	    				</configuration>
	    			</storageModule>
	    			<storageModule moduleId="org.eclipse.cdt.core.externalSettings"/>
	    		</cconfiguration>
	    	</storageModule>
	    	<storageModule moduleId="cdtBuildSystem" version="4.0.0">
	    		<project id="«projectName».cdt.managedbuild.target.gnu.cygwin.exe.1255429509" name="Executable" projectType="cdt.managedbuild.target.gnu.cygwin.exe"/>
	    	</storageModule>
	    	<storageModule moduleId="scannerConfiguration">
	    		<autodiscovery enabled="true" problemReportingEnabled="true" selectedProfileId=""/>
	    		<scannerConfigBuildInfo instanceId="cdt.managedbuild.config.gnu.cygwin.exe.debug.980520581;cdt.managedbuild.config.gnu.cygwin.exe.debug.980520581.;cdt.managedbuild.tool.gnu.cpp.compiler.cygwin.exe.debug.679549339;cdt.managedbuild.tool.gnu.cpp.compiler.input.cygwin.112150320">
	    			<autodiscovery enabled="true" problemReportingEnabled="true" selectedProfileId=""/>
	    		</scannerConfigBuildInfo>
	    		<scannerConfigBuildInfo instanceId="cdt.managedbuild.config.gnu.cygwin.exe.debug.980520581;cdt.managedbuild.config.gnu.cygwin.exe.debug.980520581.;cdt.managedbuild.tool.gnu.c.compiler.cygwin.exe.debug.1425344114;cdt.managedbuild.tool.gnu.c.compiler.input.cygwin.203106561">
	    			<autodiscovery enabled="true" problemReportingEnabled="true" selectedProfileId=""/>
	    		</scannerConfigBuildInfo>
	    		<scannerConfigBuildInfo instanceId="cdt.managedbuild.config.gnu.cygwin.exe.release.830625860;cdt.managedbuild.config.gnu.cygwin.exe.release.830625860.;cdt.managedbuild.tool.gnu.c.compiler.cygwin.exe.release.1838180696;cdt.managedbuild.tool.gnu.c.compiler.input.cygwin.492790812">
	    			<autodiscovery enabled="true" problemReportingEnabled="true" selectedProfileId=""/>
	    		</scannerConfigBuildInfo>
	    		<scannerConfigBuildInfo instanceId="cdt.managedbuild.config.gnu.cygwin.exe.release.830625860;cdt.managedbuild.config.gnu.cygwin.exe.release.830625860.;cdt.managedbuild.tool.gnu.cpp.compiler.cygwin.exe.release.728311195;cdt.managedbuild.tool.gnu.cpp.compiler.input.cygwin.330030312">
	    			<autodiscovery enabled="true" problemReportingEnabled="true" selectedProfileId=""/>
	    		</scannerConfigBuildInfo>
	    	</storageModule>
	    	<storageModule moduleId="org.eclipse.cdt.core.LanguageSettingsProviders"/>
	    </cproject>
'''
	}	
}