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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.nar.AOL;
import org.apache.maven.plugin.nar.Linker;
import org.apache.maven.plugin.nar.NarUtil;
import org.apache.maven.project.MavenProject;


/**
 * A single AOL item for compilation
 * 
 * @author mepeisen
 */
public class AolItem {
    
    /**
     * The architecture to be used
     */
    private String arch;
    
    /**
     * The operating system to be used
     */
    private String os;
    
    /**
     * The linker to be used
     */
    private String linker;
    
    /**
     * The aol (maybe cached)
     */
    private AOL aol;
    
    /**
     * For windows builds we need a reference to the php dependencies.
     */
    private File phpDepsHome;

    /**
     * The configure arguments
     */
    private String configureArgs;
    
    /**
     * Constructor
     */
    public AolItem() {
        // empty
    }
    
    /**
     * Constructor
     * @param arch
     * @param os
     * @param linker
     * @param configureArgs 
     * @param phpSdkHome 
     */
    public AolItem(String arch, String os, String linker, File phpDepsFolder, String configureArgs) {
        this.arch = arch;
        this.os = os;
        this.linker = linker;
        this.phpDepsHome = phpDepsFolder;
        this.configureArgs = configureArgs;
    }
    
    public String getConfigureArgs() {
        return configureArgs;
    }

    public File getPhpDepsFolder() {
        return this.phpDepsHome;
    }

    public String getArch() {
        return arch;
    }

    public String getOs() {
        return os;
    }

    public String getLinker() {
        return linker;
    }
    
    public String getEffectiveOs() {
        return NarUtil.getOS(this.os);
    }

    @Override
    public String toString() {
        if (this.aol == null) {
            return this.arch + "/" + this.os + "/" + this.linker;
        }
        return this.aol.getKey();
    }

    public String getClassifier() {
        return this.aol.getKey().replace(".", "-");
    }

    /**
     * Checks the prerequisites.
     * @param log
     * @param project
     * @throws MojoFailureException thrown if the prerequisites does not match
     */
    public void check(final Log log, final MavenProject project) throws MojoFailureException {
        if (this.aol != null) return;
        
        if (this.arch == null) {
            throw new MojoFailureException("Unable to determine os.arch for " + this.arch + "/" + this.os + "/" + this.linker);
        }
        
        try {
            final String os = NarUtil.getOS(this.os);
            final Linker linker = new Linker(this.linker);
            this.aol = NarUtil.getAOL(project, this.arch, os, linker, null);
            if (this.aol == null) {
                throw new MojoFailureException("Unable to determine aol for " + this.arch + "/" + this.os + "/" + this.linker);
            }
            
            log.info("check prerequisites for " + aol.getKey());
            
//            if ("vc9".equals(linker.getName())) {
//                // TODO check for setenv.bat
//            } else {
//                // TODO Check for unix or others
//            }
        } catch (MojoExecutionException ex) {
            throw new MojoFailureException("Unable to determine aol for " + this.arch + "/" + this.os + "/" + this.linker, ex);
        }
    }

    public AOL getAol() {
        return this.aol;
    }

    
}
