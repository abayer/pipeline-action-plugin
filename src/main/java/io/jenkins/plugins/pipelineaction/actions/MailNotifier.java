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

import com.google.common.collect.ImmutableMap;
import hudson.Extension;
import io.jenkins.plugins.pipelineaction.PipelineAction;
import io.jenkins.plugins.pipelineaction.PipelineActionType;

import java.util.Map;

@Extension
public class MailNotifier extends PipelineAction {

    @Override
    public String getName() {
        return "email";
    }

    @Override
    public Map<String, Boolean> getFields() {
        return ImmutableMap.<String,Boolean>builder()
                .put("to", false)
                .put("from", false)
                .put("charset", false)
                .put("subject", true)
                .put("body", true)
                .put("cc", false)
                .put("bcc", false)
                .put("replyTo", false)
                .put("mimeType", false)
                .build();
    }

    @Override
    public String getPipelineActionClass() {
        return "MailNotifierScript";
    }

    @Override
    public PipelineActionType pipelineActionType() {
        return PipelineActionType.NOTIFIER;
    }
}
