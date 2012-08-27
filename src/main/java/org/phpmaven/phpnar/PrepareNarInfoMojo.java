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
import org.apache.maven.plugin.nar.Library;
import org.apache.maven.plugin.nar.NarConstants;
import org.apache.maven.plugin.nar.NarInfo;

/**
 * Prepares the nar info for given project
 * 
 * @author mepeisen
 * @goal prepare-nar-info
 */
public class PrepareNarInfoMojo extends AbstractNarMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        
        final NarInfo info = this.getNarInfo();
        info.setBinding(null, Library.EXECUTABLE);

        for (final AolItem item : this.origAolItems) {
            item.check(getLog(), project);
            info.setOutput(item.getAol(), this.project.getArtifactId() + "-" + this.project.getVersion());
            info.setBinding(item.getAol(), Library.EXECUTABLE);
            // executable
            info.setNar(item.getAol(),
                    Library.EXECUTABLE, project.getGroupId() + ":" + project.getArtifactId() + ":"
                    + NarConstants.NAR_TYPE + ":" + "${aol}");
            // developer files
            info.setNar(item.getAol(),
                    "devel", project.getGroupId() + ":" + project.getArtifactId() + ":"
                    + NarConstants.NAR_TYPE + ":" + "${aol}" + "-devel");
            if ("Windows".equalsIgnoreCase(item.getEffectiveOs())) {
                // sdk
                info.setNar(item.getAol(),
                        "sdk", project.getGroupId() + ":" + project.getArtifactId() + ":"
                        + NarConstants.NAR_TYPE + ":" + "${aol}" + "-sdk");
                // dependencies
                info.setNar(item.getAol(),
                        "deps", project.getGroupId() + ":" + project.getArtifactId() + ":"
                        + NarConstants.NAR_TYPE + ":" + "${aol}" + "-deps");
            }
//            // tests
//            info.setNar(item.getAol(),
//                    Library.EXECUTABLE, project.getGroupId() + ":" + project.getArtifactId() + ":"
//                    + NarConstants.NAR_TYPE + ":" + "${aol}" + "-test");
        }
        
        try
        {
            final File outputDir = new File(this.project.getBuild().getOutputDirectory());
            final File propertiesDir =
                new File( outputDir, "META-INF/nar/" + this.project.getGroupId() + "/"
                    + this.project.getArtifactId() );
            if ( !propertiesDir.exists() )
            {
                propertiesDir.mkdirs();
            }
            File propertiesFile = new File( propertiesDir, NarInfo.NAR_PROPERTIES );
            getNarInfo().writeToFile( propertiesFile );
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "Cannot write nar properties file", ioe );
        }
    }
    
}
