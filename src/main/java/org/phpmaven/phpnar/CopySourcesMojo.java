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
 * A goal to copy the sources into a working directory
 * 
 * @author mepeisen
 * @goal copy-sources
 */
public class CopySourcesMojo extends AbstractNarMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();

        for (final AolItem item : this.aolItems) {
            item.check(getLog(), project);
            
            File targetFolder = new File(this.project.getBuild().getDirectory() + "/" + item.getAol());
            
            if ("Windows".equalsIgnoreCase(item.getEffectiveOs())) {
                targetFolder = new File(targetFolder, "phpdev/vc9/" + item.getArch() + "/php-" + this.project.getVersion());
                if (!targetFolder.exists()) {
                    targetFolder.mkdirs();
                }
            }
            
            getLog().info("Copying (modified) sources to " + targetFolder);
            final File sourceFolder = new File(this.project.getCompileSourceRoots().get(0));
            try {
                FileUtils.copyDirectoryStructureIfModified(sourceFolder, targetFolder);
            } catch (IOException e) {
                throw new MojoFailureException("Error while copying sources", e);
            }
        }
    }
    
}
