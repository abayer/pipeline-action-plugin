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

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.Job;
import hudson.model.User;
import hudson.model.queue.QueueTaskFuture;
import hudson.security.ACL;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.steps.input.InputAction;
import org.jenkinsci.plugins.workflow.support.steps.input.InputStepExecution;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InputActionTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testInputFields() throws Exception {
        JenkinsRule.WebClient webClient = j.createWebClient();
        JenkinsRule.DummySecurityRealm dummySecurityRealm = j.createDummySecurityRealm();
        GlobalMatrixAuthorizationStrategy authorizationStrategy = new GlobalMatrixAuthorizationStrategy();

        j.jenkins.setSecurityRealm(dummySecurityRealm);

        // Only give "alice" basic privs. That's normally not enough to Job.CANCEL, only for the fact that "alice"
        // is listed as the submitter.
        addUserWithPrivs("alice", authorizationStrategy);
        // Only give "bob" basic privs.  That's normally not enough to Job.CANCEL and "bob" is not the submitter,
        // so they should be rejected.
        addUserWithPrivs("bob", authorizationStrategy);
        // Give "charlie" basic privs + Job.CANCEL.  That should allow user3 cancel.
        addUserWithPrivs("charlie", authorizationStrategy);
        authorizationStrategy.add(Job.CANCEL, "charlie");

        j.jenkins.setAuthorizationStrategy(authorizationStrategy);

        final WorkflowJob foo = j.jenkins.createProject(WorkflowJob.class, "foo");
        ACL.impersonate(User.get("alice").impersonate(), new Runnable() {
            @Override
            public void run() {
                foo.setDefinition(new CpsFlowDefinition("runPipelineAction([name: 'input',\n"
                        + "id: 'InputX', message: 'OK?', ok: 'Yes', submitter: 'alice'])"));
            }
        });

        runAndAbort(webClient, foo, "alice", true);   // alice should work coz she's declared as 'submitter'
        runAndAbort(webClient, foo, "bob", false);    // bob shouldn't work coz he's not declared as 'submitter' and doesn't have Job.CANCEL privs
        runAndAbort(webClient, foo, "charlie", true); // charlie should work coz he has Job.CANCEL privs
    }

    private void runAndAbort(JenkinsRule.WebClient webClient, WorkflowJob foo, String loginAs, boolean expectAbortOk) throws Exception {
        // get the build going, and wait until workflow pauses
        QueueTaskFuture<WorkflowRun> queueTaskFuture = foo.scheduleBuild2(0);
        WorkflowRun run = queueTaskFuture.getStartCondition().get();
        CpsFlowExecution execution = (CpsFlowExecution) run.getExecutionPromise().get();

        while (run.getAction(InputAction.class) == null) {
            execution.waitForSuspension();
        }

        webClient.login(loginAs);

        InputAction inputAction = run.getAction(InputAction.class);
        InputStepExecution is = inputAction.getExecution("InputX");
        HtmlPage p = webClient.getPage(run, inputAction.getUrlName());

        try {
            j.submit(p.getFormByName(is.getId()), "abort");
            assertEquals(0, inputAction.getExecutions().size());
            queueTaskFuture.get();

            List<String> log = run.getLog(1000);
            System.out.println(log);
            assertTrue(expectAbortOk);
            assertEquals("Finished: ABORTED", log.get(log.size() - 1)); // Should be aborted
        } catch (Exception e) {
            List<String> log = run.getLog(1000);
            System.out.println(log);
            assertFalse(expectAbortOk);
            assertEquals("Yes or Abort", log.get(log.size() - 1));  // Should still be paused at input
        }
    }

    private void addUserWithPrivs(String username, GlobalMatrixAuthorizationStrategy authorizationStrategy) {
        authorizationStrategy.add(Jenkins.READ, username);
        authorizationStrategy.add(Jenkins.RUN_SCRIPTS, username);
        authorizationStrategy.add(Job.READ, username);
    }

}
