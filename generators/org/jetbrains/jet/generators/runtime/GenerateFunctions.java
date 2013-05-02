/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.generators.runtime;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

public class GenerateFunctions {
    private static final int MAX_PARAM_COUNT = 22;

    private enum FunctionKind {
        FUNCTION("Functions.jet", "Function", false),
        EXTENSION_FUNCTION("ExtensionFunctions.jet", "ExtensionFunction", true);

        private final String fileName;
        private final String classNamePrefix;
        private final boolean hasReceiverParameter;

        private FunctionKind(String fileName, String classNamePrefix, boolean hasReceiverParameter) {
            this.fileName = fileName;
            this.classNamePrefix = classNamePrefix;
            this.hasReceiverParameter = hasReceiverParameter;
        }

        public String getClassName(int i) {
            return classNamePrefix + i;
        }

        public String getImplClassName(int i) {
            return classNamePrefix + "Impl" + i;
        }
    }

    private static void generateBuiltInFunctions(PrintStream out, int count, FunctionKind kind) {
        generated(out);
        for (int i = 0; i <= count; i++) {
            out.print("public trait " + kind.getClassName(i));
            out.print("<");
            if (kind.hasReceiverParameter) {
                out.print("in T, ");
            }
            for (int j = 1; j <= i; j++) {
                out.print("in P" + j + ", ");
            }
            out.print("out R> {");
            out.println();
            out.print("    public fun " + (kind.hasReceiverParameter ? "T." : "") + "invoke(");
            for (int j = 1; j <= i; j++) {
                out.print("p" + j + ": " + "P" + j);
                if (j < i) {
                    out.print(", ");
                }
            }
            out.print(") : R");
            out.println();
            out.println("}");
        }
    }

    private static void generateRuntimeFunction(PrintStream out, int i, FunctionKind kind) {
        generateRuntimeClassHeader(out);

        out.println("import org.jetbrains.jet.rt.annotation.AssertInvisibleInResolver;");
        out.println();
        out.println("@AssertInvisibleInResolver");

        out.print("public interface " + kind.getClassName(i));
        generateTypeParameters(out, i, kind);
        out.println(" {");
        out.print("    R invoke(");
        if (kind.hasReceiverParameter) {
            out.print("T receiver");
            if (i > 0) {
                out.print(", ");
            }
        }
        for (int j = 1; j <= i; j++) {
            out.print("P" + j + " p" + j);
            if (j < i) {
                out.print(", ");
            }
        }
        out.println(");");
        out.println("}");
    }

    private static void generateTypeParameters(PrintStream out, int i, FunctionKind kind) {
        out.print("<");
        if (kind.hasReceiverParameter) {
            out.print("T, ");
        }
        for (int j = 1; j <= i; j++) {
            out.print("P" + j + ", ");
        }
        out.print("R>");
    }

    private static void generateRuntimeFunctionImpl(PrintStream out, int i, FunctionKind kind) {
        generateRuntimeClassHeader(out);

        out.print("public abstract class " + kind.getImplClassName(i));
        generateTypeParameters(out, i, kind);
        out.print(" extends DefaultJetObject");
        out.print(" implements " + kind.getClassName(i));
        generateTypeParameters(out, i, kind);
        out.println(" {");
        generateToStringForFunctionImpl(out);
        out.println("}");
    }

    private static void generateToStringForFunctionImpl(PrintStream out) {
        out.println("    @Override");
        out.println("    public String toString() {");
        out.println("        return getClass().getGenericSuperclass().toString();");
        out.println("    }");
    }

    private static void generateRuntimeClassHeader(PrintStream out) {
        try {
            out.println(FileUtil.loadFile(new File("injector-generator/copyright.txt")));
        }
        catch (IOException e) {
            ExceptionUtils.rethrow(e);
        }
        out.println("package jet;");
        out.println();
    }

    private static void generated(PrintStream out) {
        out.println("// Generated by " + GenerateFunctions.class.getName());
        out.println();
        out.println("package jet");
        out.println();
    }

    private static void generateBuiltInClasses() throws FileNotFoundException {
        File baseDir = new File("compiler/frontend/src/jet/");
        assert baseDir.exists() : "Base dir does not exist: " + baseDir.getAbsolutePath();

        for (FunctionKind kind : FunctionKind.values()) {
            PrintStream functions = new PrintStream(new File(baseDir, kind.fileName));
            generateBuiltInFunctions(functions, MAX_PARAM_COUNT, kind);
            functions.close();
        }
    }

    private static void generateRuntimeClasses() throws FileNotFoundException {
        File baseDir = new File("runtime/src/jet/");
        assert baseDir.exists() : "Base dir does not exist: " + baseDir.getAbsolutePath();

        for (FunctionKind kind : FunctionKind.values()) {
            for (int i = 0; i <= MAX_PARAM_COUNT; i++) {
                PrintStream function = new PrintStream(new File(baseDir, kind.getClassName(i) + ".java"));
                generateRuntimeFunction(function, i, kind);
                function.close();

                PrintStream functionImpl = new PrintStream(new File(baseDir, kind.getImplClassName(i) + ".java"));
                generateRuntimeFunctionImpl(functionImpl, i, kind);
                functionImpl.close();
            }
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        generateBuiltInClasses();
        generateRuntimeClasses();
    }

    private GenerateFunctions() {
    }
}
