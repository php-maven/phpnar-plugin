/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.phpmaven.phpnar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.nar.NarProperties;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.phpmaven.core.ExecutionUtils;

/**
 * A goal to compile the php extension
 * 
 * @goal compile
 * @author mepeisen
 */
public class CompileMojo extends AbstractNarMojo {
    
    /**
     * The extensions that should be activated or deactivated
     * @parameter
     */
    private List<Extension> extensions = new ArrayList<Extension>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        
        for (AolItem item : this.aolItems) {
            item.check(getLog(), this.project);
            
            File targetFolder = new File(this.project.getBuild().getDirectory() + "/" + item.getAol());
            
            getLog().info("************");
            getLog().info("Starting build for " + item);
            getLog().info("");
            
            if ("Windows".equalsIgnoreCase(item.getEffectiveOs())) {
                getLog().debug("windows build");
                
                // windows build
                final File buildTargetDir = new File(targetFolder, "phpdev/vc9/" + item.getArch() + "/php-" + this.project.getVersion());
                
                final File buildScript = this.generateWindowsBuildScript(item, targetFolder, buildTargetDir);
                try {
                    executeCommand(this.getLog(), "cmd /E:ON /V:ON /c \"" + buildScript.getAbsolutePath() + "\"", targetFolder);
                } catch (CommandLineException ex) {
                    throw new MojoFailureException("Error during compile", ex);
                }
            } else {
                getLog().debug("*ix build (configure/make)");
                
                final File buildScript = this.generateIxBuildScript(item, targetFolder);
                
                try {
                    executeCommand(this.getLog(), "bash \"" + buildScript.getAbsolutePath() + "\"", targetFolder);
                } catch (CommandLineException ex) {
                    throw new MojoFailureException("Error during compile", ex);
                }
            }
        }
    }
    
    /**
     * Executes a command.
     * @param log the logger
     * @param command command line
     * @param workDir working directory
     * @throws CommandLineException throw on execution errors.
     */
    private static void executeCommand(final Log log, final String command, final File workDir) throws CommandLineException {
        final Commandline cli = new Commandline(command);
        if (log != null) {
            log.debug("Executing " + command);
        }
        
        if (workDir != null) {
            if (!workDir.exists()) {
                workDir.mkdirs();
            }
            cli.setWorkingDirectory(workDir);
        }
        final StreamConsumer systemOut = new StreamConsumer() {
                @Override
                public void consumeLine(String line) {
                    log.info(line);
                }
            };
        final StreamConsumer systemErr = new StreamConsumer() {
                @Override
                public void consumeLine(String line) {
                    log.warn(line);
                }
            };
        try {
            final int result = CommandLineUtils.executeCommandLine(
                cli,
                systemOut,
                systemErr);
            if (result != 0) {
                if (log != null) {
                    log.warn("Error invoking command. Return code " + result);
                }
                throw new CommandLineException("Error invoking command. Return code " + result);
            }
        } catch (CommandLineException ex) {
            if (log != null) {
                log.warn("Error invoking command");
            }
            throw ex;
        }
    }

    private File generateIxBuildScript(AolItem item, File targetFolder) throws MojoFailureException {
        final File buildScript = new File(targetFolder, "phpmaven.build.sh");
        if (buildScript.exists()) {
            buildScript.delete();
        }
        
        final File installFolder = new File(targetFolder, "phpmaven.install");

        String buildOs = null;

        try {
            ExecutionUtils.executeCommand(getLog(), "chmod +x " + new File(targetFolder, "config.guess").getAbsolutePath().replace("\\", "\\\\").replace(" ", "\\ "));
            ExecutionUtils.executeCommand(getLog(), "chmod +x " + new File(targetFolder, "buildconf").getAbsolutePath().replace("\\", "\\\\").replace(" ", "\\ "));
            for (final File file : new File(targetFolder, "build").listFiles()) {
                if (file.isFile()) {
                    ExecutionUtils.executeCommand(getLog(), "chmod +x " + file.getAbsolutePath().replace("\\", "\\\\").replace(" ", "\\ "));
                }
            }
            buildOs = ExecutionUtils.executeCommand(getLog(), "\"" + new File(targetFolder, "config.guess").getAbsolutePath() + "\"", targetFolder).trim();
            ExecutionUtils.executeCommand(getLog(), "\"" + new File(targetFolder, "buildconf").getAbsolutePath() + "\" --force", targetFolder);
        }
        catch (CommandLineException ex) {
            throw new MojoFailureException("Failed to find the host os (config.guess)", ex);
        }
        
        final String propertyKey = item.getAol().getKey();
        final NarProperties props = NarProperties.getInstance(this.project);
        final String hostOs = props.getProperty(propertyKey + ".HostOs");
        final String archFlags = props.getProperty(propertyKey + ".ArchFlags");
        String extraLdFlags = "";
        if (archFlags.contains("-m32")) {
            extraLdFlags = " -L/usr/lib32";
        } else if (archFlags.contains("-m64")) {
            extraLdFlags = " -L/usr/lib64";
        }
        
        final StringBuffer content = new StringBuffer();
        // TODO macosx cross compile
        content.append("./configure " +
        		"--prefix=\"" + installFolder.getAbsolutePath() + "\" " +
                this.createConfigureArgs(item, "--enable-cli --enable-embed=shared") + " " +
        		"--build=" + buildOs + " " +
        		"--host=" + hostOs + " " +
        		"CFLAGS='" + archFlags + " " + props.getProperty(propertyKey + ".c.options").replace("-Wall", "") + "' " +
        		"CXXFLAGS='" + archFlags + " " + props.getProperty(propertyKey + ".cpp.options").replace("-Wall", "") + "' " +
        		"LDFLAGS='" + archFlags + extraLdFlags + "'\n");
        content.append("make\n");
        content.append("make install\n");
        
        try {
            final FileOutputStream fos = new FileOutputStream(buildScript);
            fos.write(content.toString().getBytes());
            fos.flush();
            fos.close();
        } catch (IOException ex) {
            throw new MojoFailureException("Error writing build script", ex);
        }
        
        try {
            ExecutionUtils.executeCommand(getLog(), "chmod +x " + buildScript.getAbsolutePath().replace("\\", "\\\\").replace(" ", "\\ "));
        } catch (CommandLineException ex) {
            throw new MojoFailureException("Error while chmod build script", ex);
        }
        
        return buildScript;
    }

    private File generateWindowsBuildScript(AolItem item, File targetFolder, File buildTargetDir) throws MojoFailureException {
        final File buildScript = new File(buildTargetDir, "phpmaven.build.cmd");
        if (buildScript.exists()) {
            buildScript.delete();
        }
        
        final StringBuffer content = new StringBuffer();
        content.append("@echo off\n");
        
        final String effectiveArch = item.getArch().equals("amd64") ? "x64" : item.getArch();
        
        content.append("call setenv /" + effectiveArch + " /win7 /release\n");
        content.append("cd \"" + targetFolder.getAbsolutePath() + "\"\n");
        content.append("call bin\\phpsdk_setvars.bat\n");
        content.append("cd \"" + buildTargetDir.getAbsolutePath() + "\"\n");
        content.append("call buildconf\n");
        content.append("perl -p -i.bak -e 's/PHP_OBJECT_OUT_DIR = \\'x64..\\'/PHP_OBJECT_OUT_DIR = \\'\\'/' configure.js\n");
        content.append("call configure " + this.createConfigureArgs(item, "--enable-cli") + "\n");
        
        // fix makefile for 64 builds. nmake snap wont work because of subdirectory in BUILD_DIR
        // see https://bugs.php.net/bug.php?id=62945
        if ("amd64".equals(item.getArch()) || "ia64".equals(item.getArch())) {
            content.append("perl -p -i.bak -e 's/BUILD_DIR=(.*)/BUILD_DIR=Release_TS/' Makefile\n");
            content.append("mkdir Release_TS\n");
            content.append("mkdir Release_TS\\devel\n");
            content.append("copy x64\\Release_TS\\devel Release_TS\\devel /Y\n");
        }
        
        content.append("call nmake\n");
        content.append("call nmake snap\n");
        
        try {
            final FileOutputStream fos = new FileOutputStream(buildScript);
            fos.write(content.toString().getBytes());
            fos.flush();
            fos.close();
        } catch (IOException ex) {
            throw new MojoFailureException("Error writing build script", ex);
        }
        
        return buildScript;
    }

    /**
     * Creates the configure line
     * @param item
     * @param defaultValue
     * @return command line arguments to be used
     * @throws MojoFailureException 
     */
    private String createConfigureArgs(AolItem item, String defaultValue) throws MojoFailureException {
        String configure = item.getConfigureArgs() != null ? item.getConfigureArgs() : (this.getConfigureArgs() == null ? "" : this.getConfigureArgs());
        if (configure.length() == 0) {
            configure = defaultValue;
        }
        for (final Extension ext : this.extensions) {
            if (ext.getName() == null || ext.getName().length() == 0) {
                throw new MojoFailureException("Extension name not set");
            }
            if (ext.getEnable() == null && ext.getWith() == null) {
                throw new MojoFailureException("Either set enable or with flag for extension " + ext.getName());
            }
            
            if (configure.length() > 0) {
                configure = configure + " ";
            }
            if (ext.getEnable() != null) {
                if (ext.getEnable()) {
                    configure = configure + "--enable-" + ext.getName();
                } else {
                    configure = configure + "--disable-" + ext.getName();
                    continue; // skip shared keyword
                }
            }
            if (ext.getWith() != null) {
                if (ext.getWith()) {
                    configure = configure + "--with-" + ext.getName();
                } else {
                    configure = configure + "--without-" + ext.getName();
                    continue; // skip shared keyword
                }
            }
            
            if (ext.getShared() != null) {
                if (ext.getShared()) {
                    configure = configure + "=shared";
                } else {
                    configure = configure + "=static";
                }
            }
        }
        return configure;
    }
    
}
