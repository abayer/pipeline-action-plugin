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

package io.jenkins.plugins.pipelinefunnel;

import groovy.lang.GroovyCodeSource;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.CpsThread;

import javax.annotation.Nonnull;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
public abstract class Funnel implements ExtensionPoint {

    private GroovyCodeSource scriptSource;

    /**
     * The name of the funnel. Should be unique.
     * TODO: Figure out how to enforce uniqueness?
     * 
     * @return The name of the funnel.
     */
    public abstract @Nonnull String getName();

    /**
     * The name of the class the funnel is implemented in under src/main/resources.
     *
     * @return The class name.
     */
    public abstract @Nonnull String getFunnelClass();

    /**
     * Whether this funnel is a notifier.
     *
     * @return True if the funnel is a notifier, false otherwise.
     */
    public FunnelType funnelType() {
        return FunnelType.STANDARD;
    }

    /**
     * Get the known fields for this funnel. Can be empty if there are no specific keys required or needed.
     *
     * @return List of Map keys for this funnel's argument, or an empty list.
     */
    public List<String> getFields() {
        return Collections.emptyList();
    }

    /**
     * Get the {@link GroovyCodeSource} for this contributor. Returns the existing one if it's not null.
     * Throws an {@link IllegalStateException} if the script can't be loaded.
     * TODO: Validation that the script is a valid candidate for Plumber contribution - that may be in the parsing tho.
     *
     * @return {@link GroovyCodeSource} for the contributor.
     * @throws Exception if the script source cannot be loaded.
     */
    public GroovyCodeSource getScriptSource() throws Exception {
        if (scriptSource == null) {
            String scriptUrlString = getClass().getPackage().getName().replace('$', '/').replace('.', '/')
                    + '/' + getFunnelClass() + ".groovy";
            // Expect that the script will be at package/name/className/funnelClass.groovy
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
     * @return The script object for this Plunger.
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
                .getConstructor(CpsScript.class, List.class)
                .newInstance(cpsScript, getFields());
    }

    /**
     * Returns all the registered {@link Funnel}s.
     *
     * @return All {@link Funnel}s.
     */
    public static ExtensionList<Funnel> all() {
        return ExtensionList.lookup(Funnel.class);
    }


    /**
     * Returns a map of all registered {@link Funnel}s by name.
     *
     * @return All {@link Funnel}s keyed by name.
     */
    public static Map<String,Funnel> funnelMap() {
        Map<String,Funnel> m = new HashMap<String, Funnel>();

        for (Funnel p : all()) {
            m.put(p.getName(), p);
        }

        return m;
    }

    /**
     * Finds a {@link Funnel} with the given name.
     *
     * @return The funnel for the given name if it exists.
     */
    private static Funnel getFunnelFromAll(String name) {
        return funnelMap().get(name);
    }

    /**
     * Finds a {@link Funnel} of standard type with the given name.
     *
     * @param name name of the funnel to get
     * @return The funnel for the given name if it exists, null if no such funnel exists, and an exception if a funnel
     *           of a type other than standard exists for that name.
     * @throws IllegalArgumentException
     */
    public static Funnel getFunnel(String name) throws IllegalArgumentException {
        return getFunnel(name, FunnelType.STANDARD);
    }

    /**
     * Finds a {@link Funnel} that is of the given funnel type with the given name.
     *
     * @param name The name of the funnel to get
     * @param type The type of the funnel to get
     *
     * @return The funnel for the given name if it exists, null if no such funnel exists, and an exception if
     *           a funnel of a different type exists for that name.
     *
     * @throws IllegalArgumentException if a funnel of a different type exists with the given name.
     */
    public static Funnel getFunnel(String name, FunnelType type) throws IllegalArgumentException {
        Funnel p = getFunnelFromAll(name);

        if (p != null && p.funnelType() != type) {
            throw new IllegalArgumentException("Funnel with name " + name + " exists but is not of type '" + type.getType() + "'.");
        }

        return p;
    }

}
