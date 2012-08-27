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
import org.phpmaven.core.ExecutionUtils;

/**
 * Mojo to validate the pom contents and prerequisites.
 * 
 * @author mepeisen
 * @goal validate
 */
public class ValidateMojo extends AbstractNarMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();

        for (final AolItem item : this.aolItems) {
            item.check(getLog(), this.project);
            if ("Windows".equalsIgnoreCase(item.getEffectiveOs())) {
                if (this.phpSdkHome == null) {
                    throw new MojoFailureException("Windows build needs the parameter phpSdkHome. See https://wiki.php.net/internals/windows/stepbystepbuild for details.");
                }
                if (item.getPhpDepsFolder() == null) {
                    throw new MojoFailureException("Windows build needs the parameter phpDepsHome for build " + item + ". See https://wiki.php.net/internals/windows/stepbystepbuild for details.");
                }
                
                if (! new File(this.phpSdkHome, "bin/bison.exe").exists() || ! new File(this.phpSdkHome, "script/conf_tools.bat").exists()) {
                    throw new MojoFailureException("Parameter phpSdkHome does not lead to php sdk. Ensure there is a file bin/bison.exe and script/conf_tools.bat.");
                }
                
                if (! new File(item.getPhpDepsFolder(), "include").exists() || ! new File(item.getPhpDepsFolder(), "lib").exists()) {
                    throw new MojoFailureException("Parameter phpDepsFolder does not lead to php dependencies. Ensure there is a directory bin and include.");
                }
                
                if (ExecutionUtils.searchExecutable(getLog(), "setenv.cmd") == null) {
                    throw new MojoFailureException("setenv.cmd could not be found. Ensure that windows sdk is installed and available via path. See https://wiki.php.net/internals/windows/stepbystepbuild for details.");
                }
            }
        }
    }
    
    
    
}
