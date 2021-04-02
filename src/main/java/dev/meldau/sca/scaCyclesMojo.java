package dev.meldau.sca;

/*
 * Copyright 2020-2021 Ingo Meldau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;


@Mojo(name = "sca-cycles", defaultPhase = LifecyclePhase.TEST, threadSafe = true)
public class scaCyclesMojo extends AbstractMojo {
    /**
     * output Directory
     */
    @Parameter(property = "project.build.directory", required = true, readonly = true)
    private File outputDirectory;

    /**
     * sca output Directory
     */
    @Parameter(name = "scaOutputDir", required = true, defaultValue = "${project.build.directory}/sca-output")
    private File scaOutputDir;

    /**
     * Maven Log Variable
     */
    Log mvnLog = this.getLog();

    public void execute() throws MojoExecutionException, MojoFailureException {
        mvnLog.info("Hello World");
    }
}
