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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.FileUtils;

/**
 * A goal to package the nar files
 * 
 * @author mepeisen
 * @goal package
 */
public class PackageMojo extends AbstractNarMojo {
    
    /**
     * The maven project helper
     * @component
     * @required
     */
    private MavenProjectHelper projectHelper;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        
        final File packageFolder = new File(this.project.getBuild().getDirectory());

        for (final AolItem item : this.aolItems) {
            item.check(getLog(), project);
            
            final File targetFolder = new File(this.project.getBuild().getDirectory() + "/" + item.getAol());
            
            try {
                if ("Windows".equalsIgnoreCase(item.getEffectiveOs())) {
                    final String effectiveArch = item.getArch().equals("amd64") ? "x64" : item.getArch();
                    final File buildRootFolder = new File(targetFolder, "phpdev/vc9/" + item.getArch() + "/php-" + this.project.getVersion());
                    
                    // executables
                    final File executablePackage = new File(buildRootFolder, "Release_TS\\php-" + project.getVersion() + "-Win32-VC9-" + effectiveArch + ".zip");
                    if (!executablePackage.exists()) {
                        throw new MojoFailureException("executable package " + executablePackage.getAbsolutePath() + " not found. Possible build failure.");
                    }
                    final File executableNarFile = new File(packageFolder, this.project.getArtifactId() + "-" + this.project.getVersion() + "-" + item.getClassifier() + ".nar");
                    FileUtils.copyFileIfModified(executablePackage, executableNarFile);
                    this.projectHelper.attachArtifact(this.project, "nar", item.getClassifier(), executableNarFile);
                    
                    // developer pack
                    final File developerPackage = new File(buildRootFolder, "Release_TS\\php-devel-pack-" + project.getVersion() + "-Win32-VC9-" + effectiveArch + ".zip");
                    if (!developerPackage.exists()) {
                        throw new MojoFailureException("developer package " + developerPackage.getAbsolutePath() + " not found. Possible build failure.");
                    }
                    final File developerNarFile = new File(packageFolder, this.project.getArtifactId() + "-" + this.project.getVersion() + "-" + item.getClassifier() + "-devel.nar");
                    FileUtils.copyFileIfModified(developerPackage, developerNarFile);
                    this.projectHelper.attachArtifact(this.project, "nar", item.getClassifier() + "-devel", developerNarFile);
//                    
//                    // test pack
//                    final File testPackage = new File(buildRootFolder, "Release_TS\\php-test-pack-" + project.getVersion() + "-Win32-VC9-" + effectiveArch + ".zip");
//                    if (!testPackage.exists()) {
//                        throw new MojoFailureException("test package " + testPackage.getAbsolutePath() + " not found. Possible build failure.");
//                    }
//                    final File testNarFile = new File(packageFolder, this.project.getArtifactId() + "-" + this.project.getVersion() + "-" + item.getClassifier() + "-test.nar");
//                    FileUtils.copyFileIfModified(testPackage, testNarFile);
                    
                    // sdk files
                    final File sdkNarFile = new File(packageFolder, this.project.getArtifactId() + "-" + this.project.getVersion() + "-" + item.getClassifier() + "-sdk.nar");
                    if (!sdkNarFile.exists()) {
                        ZipOutputStream target = new ZipOutputStream(new FileOutputStream(sdkNarFile));
                        zip(target, new File(targetFolder, "bin"), "/bin");
                        zip(target, new File(targetFolder, "script"), "/script");
                        target.flush();
                        target.close();
                    }
                    this.projectHelper.attachArtifact(this.project, "nar", item.getClassifier() + "-sdk", sdkNarFile);
                    
                    // dependencies files
                    final File depsNarFile = new File(packageFolder, this.project.getArtifactId() + "-" + this.project.getVersion() + "-" + item.getClassifier() + "-deps.nar");
                    if (!depsNarFile.exists()) {
                        ZipOutputStream target = new ZipOutputStream(new FileOutputStream(depsNarFile));
                        zip(target, new File(buildRootFolder.getParentFile(), "deps"), "/deps");
                        target.flush();
                        target.close();
                    }
                    this.projectHelper.attachArtifact(this.project, "nar", item.getClassifier() + "-deps", depsNarFile);
                } else {
                    final File buildRootFolder = new File(targetFolder, "phpmaven.install");
                    
                    // executable
                    final File executableNarFile = new File(packageFolder, this.project.getArtifactId() + "-" + this.project.getVersion() + "-" + item.getClassifier() + ".nar");
                    if (executableNarFile.exists()) {
                        executableNarFile.delete();
                    }
                    if (!new File(buildRootFolder, "bin/php").exists() && !new File(buildRootFolder, "bin/php-cgi").exists()) {
                        throw new MojoFailureException("executables bin/php and bin/php-cgi not found. Possible build failure.");
                    }
                    
                    final ZipOutputStream executableTarget = new ZipOutputStream(new FileOutputStream(executableNarFile));
                    zip(executableTarget, new File(buildRootFolder, "bin/php"), "/bin/php");
                    zip(executableTarget, new File(buildRootFolder, "bin/php-cgi"), "bin/php-cgi");
                    zip(executableTarget, new File(buildRootFolder, "modules"), "/modules");
                    executableTarget.flush();
                    executableTarget.close();
                    this.projectHelper.attachArtifact(this.project, "nar", item.getClassifier(), executableNarFile);
                    
                    // developer pack
                    final File developerNarFile = new File(packageFolder, this.project.getArtifactId() + "-" + this.project.getVersion() + "-" + item.getClassifier() + "-devel.nar");
                    if (developerNarFile.exists()) {
                        developerNarFile.delete();
                    }
                    final ZipOutputStream developerTarget = new ZipOutputStream(new FileOutputStream(developerNarFile));
                    if (!new File(buildRootFolder, "lib/libphp5.so").exists() && !new File(buildRootFolder, "include").exists()) {
                        throw new MojoFailureException("library lib/libphp5.so not built or include folder not found. Ensure you used --enable-embed=shared if you overwrite the configure options.");
                    }
                    zipFilterFile(developerTarget, new File(buildRootFolder, "bin/php-config"), "/bin/php-config", buildRootFolder.getAbsolutePath(), "${MAVEN.INSTALL.ROOT}");
                    zipFilterFile(developerTarget, new File(buildRootFolder, "bin/phpize"), "/bin/phpize", buildRootFolder.getAbsolutePath(), "${MAVEN.INSTALL.ROOT}");
                    zip(developerTarget, new File(buildRootFolder, "lib/libphp5.so"), "/lib/libphp5.so");
                    zip(developerTarget, new File(buildRootFolder, "include"), "/include");
                    developerTarget.flush();
                    developerTarget.close();
                    this.projectHelper.attachArtifact(this.project, "nar", item.getClassifier() + "-devel", developerNarFile);
                }
            } catch (IOException ex) {
                throw new MojoFailureException("Error copying/creating nar files", ex);
            }
        }
    }

    private void zipFilterFile(ZipOutputStream target, File file, String pathNameInFile, String filterFrom, String filterTo) throws IOException {
        if (!file.exists()) return;
        final String contents = FileUtils.fileRead(file).replace(filterFrom, filterTo);
        final ZipEntry entry = new ZipEntry(pathNameInFile);
        entry.setTime(file.lastModified());
        target.putNextEntry(entry);
        target.write(contents.getBytes());
        target.closeEntry();
    }

    private void zip(ZipOutputStream target, File sourceFile, String pathNameInFile) throws IOException
    {
        if (sourceFile.exists()) {
            add(sourceFile.getAbsolutePath().length(), sourceFile.getAbsoluteFile(), target, pathNameInFile);
        }
    }
    
    private void add(int relLength, File source, ZipOutputStream target, String prepend) throws IOException
    {
        BufferedInputStream in = null;
        try
        {
            if (source.isDirectory())
            {
                String name = source.getPath().substring(relLength).replace("\\", "/");
                if (!name.isEmpty())
                {
                    if (!name.endsWith("/"))
                        name += "/";
                    ZipEntry entry = new ZipEntry(prepend + name);
                    entry.setTime(source.lastModified());
                    target.putNextEntry(entry);
                    target.closeEntry();
                }
                for (File nestedFile: source.listFiles())
                    add(relLength, nestedFile, target, prepend);
                return;
            }
            
            ZipEntry entry = new ZipEntry(prepend + source.getPath().substring(relLength).replace("\\", "/"));
            entry.setTime(source.lastModified());
            target.putNextEntry(entry);
            in = new BufferedInputStream(new FileInputStream(source));
            
            byte[] buffer = new byte[1024];
            while (true)
            {
                int count = in.read(buffer);
                if (count == -1)
                    break;
                target.write(buffer, 0, count);
            }
            target.closeEntry(); 
        } 
        finally 
        { 
            if (in != null) 
                in.close(); 
        } 
    }
    
}
