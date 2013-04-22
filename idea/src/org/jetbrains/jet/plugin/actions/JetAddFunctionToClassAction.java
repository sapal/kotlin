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

package org.jetbrains.jet.plugin.actions;

import com.intellij.codeInsight.hint.QuestionAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetClassBody;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.codeInsight.CodeInsightUtils;
import org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil;
import org.jetbrains.jet.plugin.codeInsight.ReferenceToClassesShortening;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JetAddFunctionToClassAction implements QuestionAction {
    private final List<FunctionToAdd> functionsToAdd;
    private final Project project;
    private final Editor editor;
    private final BindingContext bindingContext;

    public static class FunctionToAdd {
        private final ClassDescriptor classDescriptor;
        private final FunctionDescriptor functionDescriptor;

        public ClassDescriptor getClassDescriptor() {
            return classDescriptor;
        }

        public FunctionDescriptor getFunctionDescriptor() {
            return functionDescriptor;
        }

        public FunctionToAdd(FunctionDescriptor functionDescriptor, ClassDescriptor classDescriptor) {
            this.functionDescriptor = functionDescriptor;
            this.classDescriptor = classDescriptor;
        }
    }

    public JetAddFunctionToClassAction(
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull BindingContext bindingContext,
            @NotNull List<FunctionToAdd> functionsToAdd) {
        this.project = project;
        this.editor = editor;
        this.bindingContext = bindingContext;
        this.functionsToAdd = new ArrayList<FunctionToAdd>(functionsToAdd);
    }

    @Override
    public boolean execute() {
        if (functionsToAdd.isEmpty()) {
            return false;
        }

        if (functionsToAdd.size() == 1 || !editor.getComponent().isShowing()) {
            FunctionToAdd function = functionsToAdd.get(0);
            addFunction(project, function.getClassDescriptor(), function.getFunctionDescriptor(), bindingContext);
        }
        else {
            // TODO
        }

        return true;
    }

    private static void addFunction(
            final Project project,
            ClassDescriptor classDescriptor,
            FunctionDescriptor functionDescriptor,
            BindingContext bindingContext
    ) {
        final String signatureString = CodeInsightUtils.createFunctionSignatureStringFromDescriptor(
                functionDescriptor,
                /* shortTypeNames = */ false);

        PsiDocumentManager.getInstance(project).commitAllDocuments();

        final JetClass classDeclaration = (JetClass) DescriptorToDeclarationUtil.getDeclaration(project, classDescriptor, bindingContext);
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
            @Override
            public void run() {

                JetClassBody body = classDeclaration.getBody();
                if (body == null) {
                    classDeclaration.add(JetPsiFactory.createWhiteSpace(project));
                    body = (JetClassBody) classDeclaration.add(JetPsiFactory.createEmptyClassBody(project));
                }

                // TODO: merge with OverrideImplementMethodsHandler
                PsiElement anchor = body.getLBrace();
                JetNamedFunction functionElement = JetPsiFactory.createFunction(project, signatureString + "{}");
                body.addAfter(functionElement, anchor);

                ReferenceToClassesShortening.compactReferenceToClasses(Collections.singletonList(functionElement));
         }
        }, JetBundle.message("add.function.to.class.action"), null);
    }


}
