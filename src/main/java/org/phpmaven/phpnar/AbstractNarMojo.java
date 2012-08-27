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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.nar.NarInfo;
import org.apache.maven.plugin.nar.NarProperties;
import org.apache.maven.plugin.nar.NarUtil;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.PropertyUtils;
import org.phpmaven.core.ExecutionUtils;

/**
 * Mojo base class for nar mojos.
 * 
 * @author mepeisen
 */
abstract class AbstractNarMojo extends AbstractMojo {
    
    /**
     * true if the nar properties are already injected
     */
    private static boolean propertiesInjected = false;
    
    /**
     * Flag to control cross compilation
     * @parameter expression="${crossCompile}"
     * @readonly
     */
    private boolean crossCompile = false;
    
    /**
     * Flag to control windows cross compilation (gcc to windows via wine and windows to gcc vis cygwin)
     * @parameter expression="${crossCompileWindows}"
     */
    private boolean crossCompileWindows = false;
    
    /**
     * The operating system to be used; empty value means: auto-detect. Some choices are: "Windows",
     * "Linux", "MacOSX", "SunOS", ... Defaults to a derived value from
     * ${os.name}
     * 
     * @parameter
     * @readonly
     */
    private String os;
    
    /**
     * The architecture to be used; empty-value means: auto-detect. Some choices are: "x86", "i386",
     * "amd64", "ppc", "sparc", ... Defaults to ${os.arch}
     * 
     * @parameter expression="${os.arch}"
     * @readonly
     */
    private String arch;
    
    /**
     * The linker to be used; empty value means: auto-detect. Some choices are: "msvc", "g++"
     * 
     * @parameter
     * @readonly
     */
    private String linker;
    
    /**
     * For windows set the location of the php-sdk (extracted). See https://wiki.php.net/internals/windows/stepbystepbuild for details.
     * @parameter expression="${phpSdkHome}"
     */
    protected File phpSdkHome;
    
    /**
     * For windows set the location of the php dependencies (extracted). See https://wiki.php.net/internals/windows/stepbystepbuild for details.
     * @parameter expression="${phpDepsHome}"
     */
    private File phpDepsHome;
    
    /**
     * The configure arguments (passed to --configure)
     * @parameter expression="${configureArgs}"
     */
    private String configureArgs;
    
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;
    
    /**
     * The aol items for the current compilation (may be multiple entries for multi compiles)
     * @parameter
     */
    protected List<AolItem> aolItems = new ArrayList<AolItem>();
    
    /**
     * Original unfiltered list of aol items
     */
    protected List<AolItem> origAolItems = new ArrayList<AolItem>();
    
    /**
     * @component role="org.codehaus.plexus.archiver.manager.ArchiverManager"
     * @required
     */
    protected ArchiverManager archiverManager;
    
    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;
    
    /**
     * @component role="org.apache.maven.repository.RepositorySystem"
     * @required
     */
    protected RepositorySystem reposSystem;
    
    /**
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;

    /**
     * The nar info
     */
    private NarInfo narInfo;

    protected boolean isCrossCompile() {
        return crossCompile;
    }

    protected void setCrossCompile(boolean crossCompile) {
        this.crossCompile = crossCompile;
    }

    protected boolean isCrossCompileWindows() {
        return crossCompileWindows;
    }

    protected void setCrossCompileWindows(boolean crossCompileWindows) {
        this.crossCompileWindows = crossCompileWindows;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!propertiesInjected) {
            getLog().debug("injecting aol properties\n" + PropertyUtils.loadProperties(AbstractNarMojo.class.getResourceAsStream("aol.properties")));
            NarProperties.inject(project, AbstractNarMojo.class.getResourceAsStream("aol.properties"));
            propertiesInjected = true;
        }
        
        if (this.aolItems.size() == 0) {
            this.aolItems.add(new AolItem(this.arch, this.os, this.linker, this.phpDepsHome, this.configureArgs));
        }
        
        // filter because of cross compilation
        this.origAolItems.addAll(this.aolItems);
        if (!this.crossCompile || !this.crossCompileWindows) {
            for (final AolItem item : this.aolItems.toArray(new AolItem[this.aolItems.size()])) {
                if (this.crossCompileWindows && !ExecutionUtils.isWindows()) {
                    this.aolItems.remove(item);
                    continue;
                }
                if (!crossCompile) {
                    // check os
                    if (!NarUtil.getOS(null).equals(item.getEffectiveOs())) {
                        this.aolItems.remove(item);
                        continue;
                    }
                }
            }
        }
    }
    
    /**
     * Returns the default configure args
     * @return configure args
     */
    protected String getConfigureArgs() {
        return configureArgs;
    }

    /**
     * Generates a nar info (source taken from maven-nar-plugin)
     * @return
     * @throws MojoExecutionException
     */
    protected final NarInfo getNarInfo() throws MojoExecutionException {
        if (narInfo == null)
        {
            String groupId = this.project.getGroupId();
            String artifactId = this.project.getArtifactId();
            
            File propertiesDir = new File( this.project.getBasedir(), "src/main/resources/META-INF/nar/" + groupId + "/" + artifactId );
            File propertiesFile = new File( propertiesDir, NarInfo.NAR_PROPERTIES );
    
            narInfo = new NarInfo( 
                groupId, artifactId,
                this.project.getVersion(), 
                getLog(),
                propertiesFile );
        }
        return narInfo;
    }
    
}
