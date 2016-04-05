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

import groovy.lang.GroovyCodeSource;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.util.CopyOnWriteList;
import io.jenkins.plugins.pipelineaction.PipelineAction;
import io.jenkins.plugins.pipelineaction.PipelineActionSet;
import io.jenkins.plugins.pipelineaction.PipelineActionType;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.cps.global.WorkflowLibRepository;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace;

@Extension
public class GlobalRepoPipelineActionSet extends PipelineActionSet {
    private @Inject
    WorkflowLibRepository repo;

    private volatile CopyOnWriteList<PipelineAction> ours;

    /**
     * Rebuilds the list of {@link GlobalRepoPipelineAction}s and update {@link ExtensionList} accordingly.
     */
    @Override
    public synchronized void rebuild() {
        File actionsDir = new File(repo.workspace, GlobalRepoPipelineAction.ACTIONS_PREFIX);
        // first time, build the initial list
        if (ours == null)
            ours = new CopyOnWriteList<PipelineAction>();

        List<PipelineAction> list = new ArrayList<PipelineAction>();

        if (actionsDir.exists()) {
            Collection<File> children = FileUtils.listFiles(actionsDir,
                    null,
                    true // Recursive
            );

            for (File child : children) {
                if (!child.getName().endsWith(".groovy") || child.isDirectory())
                    continue;

                String className = child.getName().substring(0, child.getName().length() - 7);
                // Using className as name as well for right now.

                try {
                    GroovyCodeSource scriptSource = new GroovyCodeSource(child);
                    GlobalRepoPipelineAction action = new GlobalRepoPipelineAction(scriptSource,
                            className,
                            className,
                            PipelineActionType.STANDARD, // Only supporting standard from global repo for now.
                            Collections.<String, Boolean>emptyMap(), // No fields defined for now.
                            true // Defaulting <></>o needing a node
                    );

                    list.add(action);
                } catch (IOException e) {
                    // Probably could be a better error message...
                    throw new IllegalStateException("Could not open script source - " + getFullStackTrace(e));

                }
            }
        }
        ours.replaceBy(list);
    }

    @Override
    public Iterator<PipelineAction> iterator() {
        if (ours==null) {
            rebuild();
        }
        return ours.iterator();
    }

}
