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
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted
import org.jenkinsci.plugins.workflow.cps.CpsScript

/**
 * An abstract class that all {@link PipelineAction}s will inherit from - provides convenience methods, configuration variables,
 * argument and required argument handling.
 */
@SuppressFBWarnings(value="SE_NO_SERIALVERSIONID")
public abstract class AbstractPipelineActionScript implements Serializable {

    /**
     * Used for all Pipeline step invocation.
     */
    @Whitelisted
    CpsScript script

    /**
     * Defined fields for the PipelineAction. Can be empty/null, other fields can be used that aren't defined here as
     * well.
     *
     * The map has field names as keys, and a boolean representing whether the argument/field is required.
     *
     * Can be overridden explicitly for actions coming from the global library or other sources where they don't have
     * a corresponding {@link PipelineAction}.
     */
    Map<String,Boolean> actionFields

    /**
     * True if this action needs to be run in a node context. Default is true.
     *
     * Can be overridden explicitly for actions coming from the global library or other sources where they don't have
     * a corresponding {@link PipelineAction}.
     *
     * NOTE: not currently used anywhere. Working on how that will end up looking down the road.
     */
    Boolean actionUsesNode = true

    /**
     * The {@link PipelineActionType} for this action. Default is {@code PipelineActionType.STANDARD}
     *
     * Can be overridden explicitly for actions coming from the global library or other sources where they don't have
     * a corresponding {@link PipelineAction}.
     *
     * NOTE: not currently used anywhere. Working on how that will end up looking down the road.
     */
    PipelineActionType actionType = PipelineActionType.STANDARD

    /**
     * The name of the action, as used in the "runPipelineAction(...)" step.
     *
     * Can be overridden explicitly for actions coming from the global library or other sources where they don't have
     * a corresponding {@link PipelineAction}.
     *
     * NOTE: not currently used anywhere. Working on how that will end up looking down the road.
     */
    String actionStepName

    /**
     * Creates a new {@link AbstractPipelineActionScript} instance from a {@link CpsScript} and an optional
     * {@link PipelineAction}. If the {@link PipelineAction} is given, we use its values for our fields.
     *
     * @param script A {@link CpsScript} that will be used for step invocation.
     * @param actionDefinition An optional {@link PipelineAction} defining things like required fields, etc
     */
    public AbstractPipelineActionScript(CpsScript script, PipelineAction actionDefinition = null) {
        this.script = script
        if (actionDefinition != null) {
            this.actionFields = actionDefinition.getFields()
            this.actionUsesNode = actionDefinition.usesNode()
            this.actionType = actionDefinition.pipelineActionType()
            this.actionStepName = actionDefinition.getName()
        }
    }

    /**
     * Get a new Map of the entries in the given Map with key names that are also present in the defined fields map.
     *
     * @param origArgs A map of field names to values.
     * @return A new map that's effectively a subset of the original.
     */
    @Whitelisted
    public Map copySpecifiedArgs(Map<String,Object> origArgs) {
        return origArgs.findAll { it.key in actionFields.keySet() }
    }

    /**
     * Returns a list of any required arguments defined in {@code actionFields} that are missing from the provided
     * argument map.
     *
     * @param origArgs A map of field names to values.
     * @return A list of the names of any required fields that are missing from the original map.
     */
    @Whitelisted
    public List<String> missingRequiredArgs(Map<String,Object> origArgs) {
        return requiredArgs().findAll { a ->
            !origArgs.keySet().contains(a) || origArgs.get(a) == null
        }
    }

    /**
     * Get all the required arguments for this action.
     *
     * @return a list of field names from {@code actionFields} where the value is true (meaning they're required)
     */
    @Whitelisted
    public List<String> requiredArgs() {
        return actionFields.findAll { it.value }.collect { it.key }
    }

    public static final serialVersionUID = 1L
}
