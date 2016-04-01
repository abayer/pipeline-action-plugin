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
import io.jenkins.plugins.pipelineaction.PipelineAction;
import io.jenkins.plugins.pipelineaction.PipelineActionType;

import javax.annotation.Nonnull;
import java.util.Map;

public class GlobalRepoPipelineAction extends PipelineAction {
    public static final String ACTIONS_PREFIX = "actions";

    private final String name;
    private final String pipelineActionClass;
    private final PipelineActionType pipelineActionType;
    private final Map<String,Boolean> fields;
    private final Boolean usesNode;


    public GlobalRepoPipelineAction(GroovyCodeSource scriptSource,
                                    String name,
                                    String pipelineActionClass,
                                    PipelineActionType pipelineActionType,
                                    Map<String,Boolean> fields,
                                    Boolean usesNode) {
        this.scriptSource = scriptSource;
        this.name = name;
        this.pipelineActionClass = pipelineActionClass;
        this.pipelineActionType = pipelineActionType;
        this.fields = fields;
        this.usesNode = usesNode;
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    @Nonnull
    @Override
    public String getPipelineActionClass() {
        return pipelineActionClass;
    }

    @Override
    public PipelineActionType pipelineActionType() {
        return pipelineActionType;
    }

    @Override
    public Map<String, Boolean> getFields() {
        return fields;
    }

    @Override
    public Boolean usesNode() {
        return usesNode;
    }

    /**
     * Gets the {@link GroovyCodeSource} defined at initialization time.
     *
     * @return The code provided at creation.
     * @throws Exception Inherited, never actually throws an exception.
     */
    @Override
    public GroovyCodeSource getScriptSource() throws Exception {
        return scriptSource;
    }

}
