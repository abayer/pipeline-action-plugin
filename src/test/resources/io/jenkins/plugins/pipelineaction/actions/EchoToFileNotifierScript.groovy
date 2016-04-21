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

import com.cloudbees.groovy.cps.NonCPS
import io.jenkins.plugins.pipelineaction.PipelineAction
import org.jenkinsci.plugins.workflow.cps.CpsScript


class EchoToFileNotifierScript extends AbstractPipelineActionScript {
    public EchoToFileNotifierScript(CpsScript script, PipelineAction actionDefinition = null) {
        super(script, actionDefinition)
    }

    def call(Map<String,Object> args) {
        def missingArgs = missingRequiredArgs(args)
        if (missingArgs.isEmpty()) {
            script.writeFile(file: args.file, text: getFileText(args))
        } else {
            script.error("Missing required field(s) for 'echoToFileNotifier' action: " + missingArgs.join(', '))
        }

    }

    @NonCPS
    def getFileText(Map<String,Object> args) {
        return args.collect { "${it.key}:${it.value}" }.join("\n")
    }

}