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
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

/**
 * A goal to copy the php sdk for windows
 * 
 * @author mepeisen
 * @goal copy-sdk
 */
public class CopySdkMojo extends AbstractNarMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();

        for (final AolItem item : this.aolItems) {
            item.check(getLog(), project);
            
            final File targetFolder = new File(this.project.getBuild().getDirectory() + "/" + item.getAol());
            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }
            
            if ("Windows".equalsIgnoreCase(item.getEffectiveOs())) {
                // for windows prepare the directory structure
                final File sdkTestFile = new File(targetFolder, "bin/bison.exe");
                if (!sdkTestFile.exists()) {
                    getLog().info("Copy windows php-sdk to " + targetFolder);
                    try {
                        FileUtils.copyDirectoryStructure(this.phpSdkHome, targetFolder);
                    } catch (IOException e) {
                        throw new MojoFailureException("Error while copying", e);
                    }
                }
                final File buildTargetDir = new File(targetFolder, "phpdev/vc9/" + item.getArch());
                if (!buildTargetDir.exists()) {
                    buildTargetDir.mkdirs();
                }
                
                // copy deps
                final File depsFolder = new File(buildTargetDir, "deps");
                if (!depsFolder.exists()) {
                    depsFolder.mkdirs();
                    getLog().info("Copy dependencies to " + depsFolder);
                    try {
                        FileUtils.copyDirectoryStructure(item.getPhpDepsFolder(), depsFolder);
                    } catch (IOException e) {
                        throw new MojoFailureException("Error while copying", e);
                    }
                }
            }
        }
    }
    
}
