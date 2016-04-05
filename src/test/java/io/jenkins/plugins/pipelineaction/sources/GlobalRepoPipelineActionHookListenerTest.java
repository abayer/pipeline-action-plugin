/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jenkins.plugins.pipelineaction.sources;

import hudson.FilePath;
import io.jenkins.plugins.pipelineaction.PipelineAction;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.net.URL;

public class GlobalRepoPipelineActionHookListenerTest extends Assert {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Before
    public void setUp() {
        j.jenkins.getInjector().injectMembers(this);
    }

    /**
     * If the user creates/removes files from the repository, it should appear/disappear from the extension list.
     */
    @Test
    public void rebuild() throws Exception {
        // initial clone
        CloneCommand clone = Git.cloneRepository();
        clone.setURI(new URL(j.getURL(), "workflowLibs.git").toExternalForm());
        File dir = tmp.newFolder();
        clone.setDirectory(dir);
        Git git = clone.call();

        FilePath src = new FilePath(new File(dir, GlobalRepoPipelineAction.ACTIONS_PREFIX
                + "/io/jenkins/plugins/pipelineaction/sources"));
        src.child("GlobalRepoDemoAction.groovy").write(IOUtils.toString(
                getClass().getResource("/io/jenkins/plugins/pipelineaction/sources/GlobalRepoDemoAction.groovy")), "UTF-8");

        // this variable to become accessible once the new definition is pushed
        git.add().addFilepattern(".").call();
        commitAndPush(git);

        assertNotNull(PipelineAction.getPipelineAction("GlobalRepoDemoAction"));
        // and if the file is removed it should disappear
        src.child("GlobalRepoDemoAction.groovy").delete();
        git.rm().addFilepattern(GlobalRepoPipelineAction.ACTIONS_PREFIX
                + "/io/jenkins/plugins/pipelineaction/sources/GlobalRepoDemoAction.groovy").call();
        commitAndPush(git);
        assertNull(PipelineAction.getPipelineAction("GlobalRepoDemoAction"));
    }

    private void commitAndPush(Git git) throws GitAPIException {
        git.commit().setMessage("changed").call();
        git.push().call();
    }

}