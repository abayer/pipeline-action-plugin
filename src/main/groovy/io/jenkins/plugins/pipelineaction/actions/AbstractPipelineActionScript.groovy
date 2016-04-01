/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.jenkins.plugins.pipelineaction.actions

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.jenkins.plugins.pipelineaction.PipelineAction
import io.jenkins.plugins.pipelineaction.PipelineActionType
import org.jenkinsci.plugins.workflow.cps.CpsScript


@SuppressFBWarnings(value="SE_NO_SERIALVERSIONID")
public abstract class AbstractPipelineActionScript implements Serializable {

    CpsScript script
    Map<String,Boolean> actionFields
    Boolean actionUsesNode
    PipelineActionType actionType
    String actionStepName

    public AbstractPipelineActionScript(CpsScript script, PipelineAction actionDefinition = null) {
        this.script = script
        if (actionDefinition != null) {
            this.actionFields = actionDefinition.getFields()
            this.actionUsesNode = actionDefinition.usesNode()
            this.actionType = actionDefinition.pipelineActionType()
            this.actionStepName = actionDefinition.getName()
        }
    }

    public Map copySpecifiedArgs(Map<String,Object> origArgs) {
        return origArgs.findAll { it.key in actionFields.keySet() }
    }

    public List<String> missingRequiredArgs(Map<String,Object> origArgs) {
        return requiredArgs().findAll { a ->
            !origArgs.keySet().contains(a) || origArgs.get(a) == null
        }
    }

    public List<String> requiredArgs() {
        return actionFields.findAll { it.value }.collect { it.key }
    }

    public static final serialVersionUID = 1L
}
