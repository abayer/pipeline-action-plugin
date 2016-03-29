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
package io.jenkins.plugins.pipelinefunnel.funnels

import org.jenkinsci.plugins.workflow.cps.CpsScript


class InputImpl implements Serializable {

    CpsScript script
    List<String> fields

    public InputImpl(CpsScript script, List<String> fields) {
        this.script = script
        this.fields = fields
    }

    def call(Map<String,Object> args) {
        def id
        if (args.containsKey("id") && args.id != null) {
            id = args.id
        }

        if (args.containsKey("text") && args.text != null) {
            if (id != null) {
                script.input(message: args.text, id: id)
            } else {
                script.input(message: args.text)
            }
        } else {
            script.error("Non-null 'text' must be specified with 'input' action.")
        }

    }
}