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

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

public enum PipelineActionType {

    @Whitelisted
    STANDARD("standard"),
    @Whitelisted
    NOTIFIER("notifier"),
    @Whitelisted
    SCM("scm"),
    @Whitelisted
    REPORTER("reporter"),
    @Whitelisted
    ANY("any"),
    ;

    private final String type;

    PipelineActionType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static PipelineActionType fromString(String t) throws IllegalArgumentException {
        if (t != null) {
            for (PipelineActionType f : PipelineActionType.values()) {
                if (t.equalsIgnoreCase(f.getType())) {
                    return f;
                }
            }
        }

        throw new IllegalArgumentException("No PipelineActionType with type '" + t + "' found.");
    }
}