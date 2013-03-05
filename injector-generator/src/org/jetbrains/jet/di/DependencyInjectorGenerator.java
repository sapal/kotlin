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

package org.jetbrains.jet.di;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.utils.Printer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.*;

import static org.jetbrains.jet.di.InjectorGeneratorUtil.var;

public class DependencyInjectorGenerator {

    public static final String INDENT_STEP = "    ";

    private final Set<Field> fields = Sets.newLinkedHashSet();
    private final Set<Parameter> parameters = Sets.newLinkedHashSet();
    private final Set<Field> backsParameter = Sets.newHashSet();
    private final Set<FactoryMethod> factoryMethods = Sets.newLinkedHashSet();
    private final List<Class<?>> implementsList = Lists.newArrayList();

    private final Dependencies dependencies = new Dependencies();

    private final ImportManager importManager = new ImportManager();

    public DependencyInjectorGenerator() {
    }

    public void generate(String targetSourceRoot, String injectorPackageName, String injectorClassName, Class<?> generatorClass)
            throws IOException {
        String outputFileName = targetSourceRoot + "/" + injectorPackageName.replace(".", "/") + "/" + injectorClassName + ".java";

        File file = new File(outputFileName);

        // Windows prohibits rename of open files by default.
        // http://teamcity.jetbrains.com/viewLog.html?buildId=62376&buildTypeId=bt345&tab=buildLog
        boolean useTmpfile = !SystemInfo.isWindows;

        File tmpfile = useTmpfile ? new File(outputFileName + ".tmp") : file;
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            if (parentFile.mkdirs()) {
                System.out.println("Directory created: " + parentFile.getAbsolutePath());
            }
            else {
                throw new IllegalStateException("Cannot create directory: " + parentFile);
            }
        }

        FileOutputStream fileOutputStream = new FileOutputStream(tmpfile);
        System.out.println("File opened: " + tmpfile.getAbsolutePath());


        PrintStream out = new PrintStream(fileOutputStream);
        try {
            fields.addAll(dependencies.satisfyDependencies());

            String copyright = "injector-generator/copyright.txt";
            out.println(FileUtil.loadFile(new File(copyright)));

            out.println("package " + injectorPackageName + ";");
            out.println();

            generateImports(out, injectorPackageName);
            out.println();

            out.println("/* This file is generated by " + generatorClass.getName() + ". DO NOT EDIT! */");
            out.print("public class " + injectorClassName);
            generateImplementsList(out);
            out.println(" {");
            out.println();
            generateFields(out);
            out.println();
            generateConstructor(injectorClassName, out);
            out.println();
            generateDestroy(injectorClassName, out);
            out.println();
            generateGetters(out);
            if (!factoryMethods.isEmpty()) {
                out.println();
                generateFactoryMethods(out);
            }
            out.println("}"); // class

            fileOutputStream.close();

            if (useTmpfile) {
                if (!tmpfile.renameTo(file)) {
                    throw new RuntimeException("failed to rename " + tmpfile + " to " + file);
                }
                System.out.println("Renamed " + tmpfile + " to " + file);
            }
        }
        finally {
            fileOutputStream.close();
            System.out.println("File closed");
        }
    }

    private void generateImplementsList(PrintStream out) {
        if (!implementsList.isEmpty()) {
            out.print(" implements ");
            for (Iterator<Class<?>> iterator = implementsList.iterator(); iterator.hasNext(); ) {
                Class<?> superInterface = iterator.next();
                if (!superInterface.isInterface()) {
                    throw new IllegalArgumentException("Only interfaces are supported as supertypes");
                }
                out.print(type(superInterface));
                if (iterator.hasNext()) {
                    out.print(", ");
                }
            }
        }
    }

    public void implementInterface(Class<?> superInterface) {
        implementsList.add(superInterface);
    }

    public void addPublicParameter(Class<?> type) {
        addPublicParameter(new DiType(type));
    }

    public void addPublicParameter(DiType type) {
        addPublicParameter(type, true);
    }

    public void addPublicParameter(DiType type, boolean required) {
        addParameter(true, type, var(type), required);
    }


    public void addParameter(Class<?> type) {
        addParameter(DiType.fromReflectionType(type));
    }

    public void addParameter(DiType type) {
        addParameter(type, true);
    }

    public void addParameter(Class<?> type, boolean required) {
        addParameter(new DiType(type), required);
    }

    public void addParameter(DiType type, boolean required) {
        addParameter(false, type, var(type), required);
    }



    public void addParameter(boolean reexport, @NotNull DiType type, @Nullable String name, boolean required) {
        Field field = addField(reexport, type, name, null);
        Parameter parameter = new Parameter(type, name, field, required);
        parameters.add(parameter);
        field.setInitialization(new ParameterExpression(parameter));
        backsParameter.add(field);
        dependencies.addSatisfiedField(field);
    }

    public Field addPublicField(Class<?> type) {
        return addPublicField(new DiType(type));
    }

    public Field addPublicField(DiType type) {
        return addField(true, type, null, null);
    }

    public Field addField(Class<?> type) {
        return addField(new DiType(type));
    }

    public Field addField(DiType type) {
        return addField(false, type, null, null);
    }

    public Field addField(Enum<?> enu) {
        Class<? extends Enum> clazz = enu.getClass();
        return addField(false, clazz, null, new GivenExpression(clazz.getSimpleName() + "." + enu.name()));
    }

    public Field addField(boolean isPublic, Class<?> type, @Nullable String name, @Nullable Expression init) {
        return addField(isPublic, new DiType(type), name, init);
    }

    public Field addField(boolean isPublic, DiType type, @Nullable String name, @Nullable Expression init) {
        Field field = Field.create(isPublic, type, name == null ? var(type) : name, init);
        fields.add(field);
        dependencies.addField(field);
        return field;
    }

    public void addFactoryMethod(@NotNull Class<?> returnType, Class<?>... parameterTypes) {
        List<DiType> types = Lists.newArrayList();
        for (Class<?> type : parameterTypes) {
            types.add(new DiType(type));
        }
        addFactoryMethod(new DiType(returnType), types);
    }

    public void addFactoryMethod(@NotNull DiType returnType, DiType... parameterTypes) {
        addFactoryMethod(returnType, Arrays.asList(parameterTypes));
    }

    public void addFactoryMethod(@NotNull DiType returnType, @NotNull Collection<DiType> parameterTypes) {
        List<Parameter> parameters = Lists.newArrayList();
        for (DiType type : parameterTypes) {
            parameters.add(new Parameter(type, var(type), null, false));
        }
        factoryMethods.add(new FactoryMethod("create" + type(returnType), returnType, parameters));
    }

    private void generateImports(PrintStream out, String injectorPackageName) {
        for (Class<?> importedClass : importManager.getImportedClasses()) {
            if (importedClass.isPrimitive()) continue;
            String importedPackageName = importedClass.getPackage().getName();
            if ("java.lang".equals(importedPackageName)
                || injectorPackageName.equals(importedPackageName)) {
                continue;
            }
            out.println("import " + importedClass.getCanonicalName() + ";");
        }
    }

    private void generateFields(PrintStream out) {
        for (Field field : fields) {
            String _final = backsParameter.contains(field) ? "final " : "";
            out.println("    private " + _final + type(InjectorGeneratorUtil.getEffectiveFieldType(field)) + " " + field.getName() + ";");
        }
    }

    private void generateConstructor(String injectorClassName, PrintStream out) {
        Printer p = new Printer(out);
        p.pushIndent();

        // Constructor parameters
        if (parameters.isEmpty()) {
            p.println("public ", injectorClassName, "() {");
        }
        else {
            p.println("public ", injectorClassName, "(");
            p.pushIndent();
            for (Iterator<Parameter> iterator = parameters.iterator(); iterator.hasNext(); ) {
                Parameter parameter = iterator.next();
                p.print(); // indent
                if (parameter.isRequired()) {
                    p.printWithNoIndent("@NotNull ");
                }
                p.printWithNoIndent(type(parameter.getType()), " ", parameter.getName());
                if (iterator.hasNext()) {
                    p.printlnWithNoIndent(",");
                }
            }
            p.printlnWithNoIndent();
            p.popIndent();
            p.println(") {");
        }

        p.pushIndent();

        InjectionLogicGenerator.generateForFields(p, fields);

        p.popIndent();
        p.println("}");
    }

    private void generateDestroy(@NotNull String injectorClassName, @NotNull PrintStream out) {
        out.println("    @PreDestroy");
        out.println("    public void destroy() {");
        for (Field field : fields) {
            // TODO: type of field may be different from type of object
            List<Method> preDestroyMethods = InjectorGeneratorUtil
                    .getPreDestroyMethods(InjectorGeneratorUtil.getEffectiveFieldType(field).getClazz());
            for (Method preDestroy : preDestroyMethods) {
                out.println("        " + field.getName() + "." + preDestroy.getName() + "();");
            }
            if (preDestroyMethods.size() > 0) {
                out.println();
            }
        }
        out.println("    }");
    }

    private void generateGetters(PrintStream out) {
        String indent0 = "    ";
        String indent1 = indent0 + INDENT_STEP;
        for (Field field : fields) {
            if (!field.isPublic()) continue;
            String visibility = field.isPublic() ? "public" : "private";
            out.println(indent0 + visibility + " " + type(field.getType()) + " " + field.getGetterName() + "() {");

            out.println(indent1 + "return this." + field.getName() + ";");
            out.println(indent0 + "}");
            out.println();
        }
    }

    private void generateFactoryMethods(PrintStream out) {
        Printer p = new Printer(out);
        p.pushIndent();
        for (FactoryMethod method : factoryMethods) {
            generateFactoryMethod(p, method);
        }
    }

    private void generateFactoryMethod(Printer p, FactoryMethod method) {
        Dependencies localDependencies = new Dependencies();

        Map<Parameter, Field> parameterToField = Maps.newHashMap();
        for (Parameter parameter : method.getParameters()) {
            Field field = new Field(true, parameter.getType(), parameter.getName());
            localDependencies.addSatisfiedField(field);
            parameterToField.put(parameter, field);
        }

        for (Field storedField : fields) {
            localDependencies.addSatisfiedField(storedField);
        }

        Field resultField = new Field(true, method.getReturnType(), "_result");
        localDependencies.addField(resultField);

        Collection<Field> fields = Lists.newArrayList(localDependencies.satisfyDependencies());
        fields.add(resultField);

        p.println("public ", type(method.getReturnType()), " ", method.getName(), "(");
        p.pushIndent();
        for (Iterator<Parameter> iterator = method.getParameters().iterator(); iterator.hasNext(); ) {
            Parameter parameter = iterator.next();
            p.print(type(parameter.getType()), " ", parameter.getName());
            if (iterator.hasNext()) {
                p.printlnWithNoIndent(", ");
            }
        }
        p.println();
        p.popIndent();
        p.println(") {");
        p.pushIndent();

        InjectionLogicGenerator.generateForLocalVariables(importManager, p, fields);

        p.println("return ", resultField.getName(), ";");

        p.popIndent();
        p.println("}");
    }

    private CharSequence type(DiType type) {
        return importManager.render(type);
    }

    private CharSequence type(Class<?> type) {
        return type(DiType.fromReflectionType(type));
    }
}
