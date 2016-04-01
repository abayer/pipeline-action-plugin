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

package io.jenkins.plugins.pipelineaction;

import groovy.lang.GroovyCodeSource;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.util.Iterators;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.CpsThread;

import javax.annotation.Nonnull;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace;

/**
 * {@link ExtensionPoint} for contributing Plumber scripts. Looks for relevant script in classpath and provides the
 * {@link GroovyCodeSource} for the script.
 *
 * Down the road, we'll be adding validation (probably elsewhere in the plugin, after parsing the script and having
 * something we can inspect to be sure it doesn't do things that are illegal in a Plumber context like node blocks,
 * stages, input...) and the logic to actually parse and use the scripts, but the intent is that Plumber scripts will be
 * a strict subset of valid {@link org.jenkinsci.plugins.workflow.cps.global.UserDefinedGlobalVariable}-style scripts,
 * which can be used in both forms.
 */
public abstract class PipelineAction implements ExtensionPoint {

    protected GroovyCodeSource scriptSource;

    /**
     * The name of the pipeline action. Should be unique.
     * TODO: Figure out how to enforce uniqueness?
     * 
     * @return The name of the pipeline action.
     */
    public abstract @Nonnull String getName();

    /**
     * The name of the class the piepeline action is implemented in under src/main/resources.
     *
     * @return The class name.
     */
    public abstract @Nonnull String getPipelineActionClass();

    /**
     * The {@link PipelineActionType} of this pipeline action
     *
     * @return The {@link PipelineActionType} for this pipeline action, defaulting to STANDARD.
     */
    public PipelineActionType pipelineActionType() {
        return PipelineActionType.STANDARD;
    }

    /**
     * Get the known fields for this pipeline action. Can be empty if there are no specific keys required or needed.
     *
     * @return Map of known fields, with the values being booleans marking whether the field is required, or an empty
     * map if no fields are specified.
     */
    public Map<String, Boolean> getFields() {
        return Collections.emptyMap();
    }

    /**
     * If this action needs to run in a node context, this should be true.
     *
     * @return True if this action should run in a "node { ... }" context. Defaults to true.
     */
    public Boolean usesNode() {
        return true;
    }

    /**
     * Get the {@link GroovyCodeSource} for this pipeline action. Returns the existing one if it's not null.
     * Throws an {@link IllegalStateException} if the script can't be loaded.
     * TODO: Validation that the script is a valid candidate for Plumber contribution - that may be in the parsing tho.
     *
     * @return {@link GroovyCodeSource} for the contributor.
     * @throws Exception if the script source cannot be loaded.
     */
    public GroovyCodeSource getScriptSource() throws Exception {
        if (scriptSource == null) {
            String scriptUrlString = getClass().getPackage().getName().replace('$', '/').replace('.', '/')
                    + '/' + getPipelineActionClass() + ".groovy";
            // Expect that the script will be at package/name/className/pipelineActionClass.groovy
            URL scriptUrl = getClass().getClassLoader().getResource(scriptUrlString);

            try {
                GroovyCodeSource gsc = new GroovyCodeSource(scriptUrl);
                gsc.setCachable(true);

                scriptSource = gsc;
            } catch (RuntimeException e) {
                // Probably could be a better error message...
                throw new IllegalStateException("Could not open script source - " + getFullStackTrace(e));
            }
        }

        return scriptSource;
    }

    /**
     * ONLY TO BE RUN FROM WITHIN A CPS THREAD. Parses the script source and loads it.
     * TODO: Decide if we want to cache the resulting objects or just *shrug* and re-parse them every time.
     *
     * @return The script object for this pipeline action.
     * @throws Exception if the script source cannot be loaded or we're called from outside a CpsThread.
     */
    @SuppressWarnings("unchecked")
    public Object getScript(CpsScript cpsScript) throws Exception {
        CpsThread c = CpsThread.current();
        if (c == null)
            throw new IllegalStateException("Expected to be called from CpsThread");

        return c.getExecution()
                .getShell()
                .getClassLoader()
                .parseClass(getScriptSource())
                .getConstructor(CpsScript.class, PipelineAction.class)
                .newInstance(cpsScript, this);
    }

    /**
     * Returns all the registered {@link PipelineAction}s.
     */
    public static final Iterable<PipelineAction> ALL = new Iterable<PipelineAction>() {
        @Override
        public Iterator<PipelineAction> iterator() {
            return new Iterators.FlattenIterator<PipelineAction, PipelineActionSet>(ExtensionList.lookup(PipelineActionSet.class).iterator()) {
                @Override
                protected Iterator<PipelineAction> expand(PipelineActionSet actionSet) {
                    return actionSet.iterator();
                }
            };
        }
    };

    /**
     * Returns a map of all registered {@link PipelineAction}s by name.
     *
     * @return All {@link PipelineAction}s keyed by name.
     */
    public static Map<String,PipelineAction> pipelineActionMap() {
        Map<String,PipelineAction> m = new HashMap<String, PipelineAction>();

        for (PipelineAction p : ALL) {
            m.put(p.getName(), p);
        }

        return m;
    }

    /**
     * Finds a {@link PipelineAction} with the given name.
     *
     * @return The pipeline action for the given name if it exists.
     */
    private static PipelineAction getPipelineActionFromName(String name) {
        return pipelineActionMap().get(name);
    }

    /**
     * Finds a {@link PipelineAction} of standard type with the given name.
     *
     * @param name name of the pipeline action to get
     * @return The pipeline action for the given name if it exists, null if no such pipeline action exists, and an
     *           exception if a pipeline action of a type other than standard exists for that name.
     * @throws IllegalArgumentException if a pipeline action of a type other than STANDARD exists with the given name.
     */
    public static PipelineAction getPipelineAction(String name) throws IllegalArgumentException {
        return getPipelineAction(name, PipelineActionType.STANDARD);
    }

    /**
     * Finds a {@link PipelineAction} that is of the given pipeline action type with the given name.
     *
     * @param name The name of the pipeline action to get
     * @param type The type of the pipeline action to get
     *
     * @return The pipeline action for the given name if it exists, null if no such pipeline action exists, and an
     *           exception if a pipeline action of a different type exists for that name.
     *
     * @throws IllegalArgumentException if a pipeline action of a different type exists with the given name.
     */
    public static PipelineAction getPipelineAction(String name, PipelineActionType type) throws IllegalArgumentException {
        PipelineAction p = getPipelineActionFromName(name);

        if (p != null && p.pipelineActionType() != type) {
            throw new IllegalArgumentException("PipelineAction with name " + name + " exists but is not of type '" + type.getType() + "'.");
        }

        return p;
    }

}
