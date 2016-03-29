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
package io.jenkins.plugins.pipelineaction

import com.cloudbees.groovy.cps.NonCPS
import org.jenkinsci.plugins.workflow.cps.CpsScript

// TODO: May want to move this to an actual class extending Step to avoid some weirdness.
class RunPipelineActionScript implements Serializable {
    CpsScript script

    RunPipelineActionScript(CpsScript script) {
        this.script = script
    }

    def call(Map args) {
        return call(PipelineActionType.STANDARD, args)
    }

    def call(String type, Map args) {
        return call(PipelineActionType.fromString(type), args)
    }

    def call(PipelineActionType type, Map args) {
        String name = args?.name

        if (name == null) {
            name = "script"
        }

        return getPipelineAction(name, type)?.call(args)
    }

    @NonCPS
    def getPipelineAction(String name, PipelineActionType type) {
        return PipelineAction.getPipelineAction(name, type)?.getScript(script)
    }
}