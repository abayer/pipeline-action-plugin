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

import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.SecureASTCustomizer
import org.jenkinsci.plugins.workflow.cps.CpsScript


public class StepBlacklister {
    static final List<String> blacklisted = ["stage", "parallel", "node"]

    public static GroovyShell getBlacklisterShell(GroovyShell origShell) {
        final SecureASTCustomizer astCustomizer = new SecureASTCustomizer()

        def blacklistedMethods = { expr ->
            if (expr instanceof MethodCallExpression
                && expr.getObjectExpression() instanceof VariableExpression) {
                if (((VariableExpression)expr.getObjectExpression())?.getType()?.getTypeClass()?.isAssignableFrom(CpsScript.class)
                    && expr.getMethodAsString() in blacklisted) {
                    false
                } else {
                    true
                }
            } else {
                true
            }
        } as SecureASTCustomizer.ExpressionChecker

        astCustomizer.addExpressionCheckers blacklistedMethods

        final CompilerConfiguration conf = new CompilerConfiguration()
        conf.addCompilationCustomizers(astCustomizer)

        return new GroovyShell(origShell.getClassLoader(), origShell.getContext(), conf)
    }


}
