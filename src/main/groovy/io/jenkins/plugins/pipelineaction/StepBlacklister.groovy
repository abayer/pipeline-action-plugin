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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.SecureASTCustomizer
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.workflow.cps.CpsThread

/**
 * Provides a static method for taking an existing {@link GroovyShell} and creating a new one with
 * the same {@link GroovyClassLoader} and {@link Binding} but a new, custom {@link SecureASTCustomizer}
 * set up to look for invocations of "script.(some-step)" where "some-step" is one of a list of
 * blacklisted steps.
 */
@SuppressFBWarnings(value="SE_NO_SERIALVERSIONID")
public class StepBlacklister {
    /**
     * The list of steps to blacklist - currently blacklisting "stage", "parallel" and "node" so as not to
     * break Plumber's model.
     */
    static final List<String> blacklisted = ["stage", "parallel", "node"]

    /**
     * Takes the original {@link GroovyShell} and returns a copy of it with a new {@link CompilerConfiguration}
     * that will reject any method invoked on a {@link CpsScript} object with one of the names in the blacklist.
     *
     * @param origShell An existing {@link GroovyShell}, probably the one we get from {@link CpsThread}.
     * @return a new {@link GroovyShell} with the tweaked {@link CompilerConfiguration}
     */
    public static GroovyShell getBlacklisterShell(GroovyShell origShell) {
        final SecureASTCustomizer astCustomizer = new SecureASTCustomizer()

        def blacklistedMethods = { expr ->
            // We only care about method calls where the method is being called on a variable - i.e., static methods
            // and the like are fine.
            if (expr instanceof MethodCallExpression
                && expr.getObjectExpression() instanceof VariableExpression) {
                // Cast the object expression to a VariableExpression.
                VariableExpression v = (VariableExpression) expr.getObjectExpression()
                // Get the type class for the type represented by the VariableExpression. Nulls are possible.
                Class clazz = v?.getType()?.getTypeClass()

                // Check if the class is assignable from CpsScript and the method string is in the blacklist.
                // Null-safety means the expression resolves to false if clazz is nullable, which is good.
                if (clazz?.isAssignableFrom(CpsScript.class)
                    && expr.getMethodAsString() in blacklisted) {
                    // If so, return false
                    false
                }
            }

            // Fallback to true, in which case compilation can continue.
            true
        } as SecureASTCustomizer.ExpressionChecker

        // Add the method check to the expression checkers on the AST customizer
        astCustomizer.addExpressionCheckers blacklistedMethods

        // Create the CompilerConfiguration and add the AST customizer
        final CompilerConfiguration conf = new CompilerConfiguration()
        conf.addCompilationCustomizers(astCustomizer)

        // Return a new GroovyShell with the original shell's classloader and binding (i.e., getContext()) but
        // adding the new CompilerConfiguration.
        return new GroovyShell(origShell.getClassLoader(), origShell.getContext(), conf)
    }


}
