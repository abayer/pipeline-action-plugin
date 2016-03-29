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
package io.jenkins.plugins.pipelineaction.actions;

import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.scm.GitSampleRepoRule;
import org.jenkinsci.plugins.workflow.steps.scm.GitStep;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class ScriptActionTest {
    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();
    @Rule
    public RestartableJenkinsRule story = new RestartableJenkinsRule();
    @Rule public GitSampleRepoRule sampleRepo = new GitSampleRepoRule();

    @Test
    public void testScriptExplicitName() throws Exception {
        sampleRepo.init();
        sampleRepo.write("Jenkinsfile",
                "node {\n"
                        + "runPipelineAction([name:'script',\n"
                        + "script:'echo trousers'])\n"
                        + "}");

        sampleRepo.git("add", "Jenkinsfile");
        sampleRepo.git("commit", "--message=files");

        runTestForLog();
    }

    @Test
    public void testScriptAsDefault() throws Exception {
        sampleRepo.init();
        sampleRepo.write("Jenkinsfile",
                "node {\n"
                        + "runPipelineAction([\n"
                        + "script:'echo trousers'])\n"
                        + "}");

        sampleRepo.git("add", "Jenkinsfile");
        sampleRepo.git("commit", "--message=files");

        runTestForLog();
    }

    private void runTestForLog() {
        story.addStep(new Statement() {
            @Override public void evaluate() throws Throwable {
                WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class, "p");
                p.setDefinition(new CpsScmFlowDefinition(new GitStep(sampleRepo.toString()).createSCM(), "Jenkinsfile"));
                WorkflowRun b = p.scheduleBuild2(0).waitForStart();
                story.j.assertLogContains("trousers",
                        story.j.assertBuildStatusSuccess(story.j.waitForCompletion(b)));
            }
        });
    }
}
