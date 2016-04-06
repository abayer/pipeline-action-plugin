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

import hudson.ExtensionFinder;
import hudson.ExtensionList;
import hudson.model.Result;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.cps.global.WorkflowLibRepository;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import javax.inject.Inject;
import java.io.File;

public class GlobalRepoPipelineActionTest {
    @Rule
    public RestartableJenkinsRule story = new RestartableJenkinsRule();

    @Inject
    Jenkins jenkins;

    @Inject
    WorkflowLibRepository repo;

    @Inject
    GlobalRepoPipelineActionSet globalRepoPipelineActionSet;

    /**
     * Test
     */
    @Test
    public void actionFromGlobalRepo() throws Exception {
        story.addStep(new Statement() {
            @Override public void evaluate() throws Throwable {
                File dir = new File(repo.workspace,"actions/io/jenkins/plugins/pipelineaction/sources");
                dir.mkdirs();

                File outFile = new File(dir, "GlobalRepoDemoAction.groovy");
                FileUtils.copyURLToFile(
                        getClass().getResource("/io/jenkins/plugins/pipelineaction/sources/GlobalRepoDemoAction.groovy"),
                        outFile);

                // Hack to deal with the lack of an actual commit.
                globalRepoPipelineActionSet.rebuild();
                WorkflowJob p = jenkins.createProject(WorkflowJob.class, "p");

                p.setDefinition(new CpsFlowDefinition(
                        "runPipelineAction(['name':'GlobalRepoDemoAction',\n"
                                + "pants:'trousers',\n"
                                + "shirts:'polos'])\n"
                ));

                // get the build going
                WorkflowRun b = p.scheduleBuild2(0).getStartCondition().get();
                story.j.assertLogContains("echoing pants == trousers",
                        story.j.assertBuildStatusSuccess(story.j.waitForCompletion(b)));
                story.j.assertLogContains("echoing shirts == polos", b);
            }
        });
    }

    @Test
    public void invalidStepsInAction() {
        story.addStep(new Statement() {
            @Override public void evaluate() throws Throwable {

                File dir = new File(repo.workspace,"actions/io/jenkins/plugins/pipelineaction/sources");
                dir.mkdirs();

                File outFile = new File(dir, "InvalidStepsAction.groovy");
                FileUtils.copyURLToFile(
                        getClass().getResource("/io/jenkins/plugins/pipelineaction/sources/InvalidStepsAction.groovy"),
                        outFile);

                // Hack to deal with the lack of an actual commit.
                globalRepoPipelineActionSet.rebuild();
                WorkflowJob p = jenkins.createProject(WorkflowJob.class, "p");

                p.setDefinition(new CpsFlowDefinition(
                        "runPipelineAction(['name':'InvalidStepsAction',\n"
                                + "pants:'trousers',\n"
                                + "shirts:'polos'])\n"
                ));

                // get the build going
                WorkflowRun b = p.scheduleBuild2(0).getStartCondition().get();
                story.j.assertBuildStatus(Result.FAILURE, story.j.waitForCompletion(b));
                story.j.assertLogContains("Blacklisted steps used in action", b);
                story.j.assertLogContains("Expression [MethodCallExpression] is not allowed: script.node", b);
            }
        });

    }

}
