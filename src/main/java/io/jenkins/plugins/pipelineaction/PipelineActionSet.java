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
package io.jenkins.plugins.pipelineaction;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.util.Iterator;

/**
 * A collection of {@link PipelineAction}s.
 */
public abstract class PipelineActionSet implements ExtensionPoint, Iterable<PipelineAction> {

    /**
     * The default case does not need to actually rebuild anything.
     */
    public synchronized void rebuild() {
        // No-op for default.
    }

    /**
     * For {@link PipelineAction}s contributed via the classpath.
     */
    @Extension
    @Restricted(NoExternalUse.class)
    public static class PluginProvidedPipelineActionSet extends PipelineActionSet {
        @Override
        public Iterator<PipelineAction> iterator() {
            return ExtensionList.lookup(PipelineAction.class).iterator();
        }
    }
}
