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

import hudson.Extension;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.BlanketWhitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.ProxyWhitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.StaticWhitelist;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

import java.io.IOException;

@Extension
public class RunPipelineAction extends GlobalVariable {
    @Override
    public String getName() {
        return "runPipelineAction";
    }

    @Override
    public Object getValue(CpsScript script) throws Exception {
        return script.getClass()
                .getClassLoader()
                .loadClass("io.jenkins.plugins.pipelineaction.RunPipelineActionScript")
                .getConstructor(CpsScript.class)
                .newInstance(script);
    }

    @Extension
    public static class PipelineActionWhitelist extends ProxyWhitelist {
        public PipelineActionWhitelist() throws IOException {
            super(new StaticWhitelist(
                    "method java.util.Map$Entry getKey",
                    "method java.util.Map$Entry getValue",
                    "method java.util.Map entrySet",
                    "staticField java.lang.System err",
                    "method java.io.PrintStream println java.lang.String",
                    "method java.util.Collection isEmpty",
                    "staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods toList java.util.Collection"

            ));
        }
    }

}
