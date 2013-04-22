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

package org.jetbrains.jet.plugin.quickfix;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.actions.JetAddFunctionToClassAction;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManagerUtil;
import org.jetbrains.jet.plugin.codeInsight.CodeInsightUtils;

import java.util.List;

public class AddFunctionToSupertypeFix extends JetHintAction<JetNamedFunction> {
    private final List<JetAddFunctionToClassAction.FunctionToAdd> functionsToAdd;

    public AddFunctionToSupertypeFix(JetNamedFunction element) {
        super(element);
        functionsToAdd = generateFunctionsToAdd(element);
    }

    private static List<JetAddFunctionToClassAction.FunctionToAdd> generateFunctionsToAdd(JetNamedFunction functionElement) {
        BindingContext context = KotlinCacheManagerUtil.getDeclarationsFromProject(functionElement).getBindingContext();
        FunctionDescriptor functionDescriptor = context.get(BindingContext.FUNCTION, functionElement);
        List<JetAddFunctionToClassAction.FunctionToAdd> functions = Lists.newArrayList();
        if (functionDescriptor == null) return functions;
        DeclarationDescriptor containingDeclaration = functionDescriptor.getContainingDeclaration();
        if (!(containingDeclaration instanceof ClassDescriptor)) return functions;
        ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
        for (ClassDescriptor superclassDescriptor : DescriptorUtils.getSuperclassDescriptors(classDescriptor)) {
            functions.add(new JetAddFunctionToClassAction.FunctionToAdd(
                    generateFunctionSignatureForClass(functionDescriptor, superclassDescriptor),
                    superclassDescriptor));
        }
        return functions;
    }

    private static FunctionDescriptor generateFunctionSignatureForClass(FunctionDescriptor functionDescriptor, ClassDescriptor classDescriptor) {
        return functionDescriptor.copy(
                classDescriptor,
                Modality.OPEN,
                functionDescriptor.getVisibility(),
                CallableMemberDescriptor.Kind.DECLARATION,
                /* copyOverrides = */ false); // TODO
    }

    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetNamedFunction function = QuickFixUtil.getParentElementOfType(diagnostic, JetNamedFunction.class);
                return function == null ? null : new AddFunctionToSupertypeFix(function);
            }
        };
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file) && !functionsToAdd.isEmpty();
    }

    @Override
    public boolean showHint(Editor editor) {
        return false;
    }

    @NotNull
    @Override
    public String getText() {
        ClassDescriptor supertype = functionsToAdd.get(0).getClassDescriptor();
        FunctionDescriptor newFunction = functionsToAdd.get(0).getFunctionDescriptor();
        return JetBundle.message("add.function.to.supertype.action",
                                 CodeInsightUtils.createFunctionSignatureStringFromDescriptor(newFunction, /* shortTypeNames */ true),
                                 supertype.getName().toString());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("add.function.to.supertype.family");
    }

    @Override
    public void invoke(
            @NotNull final Project project, final Editor editor, PsiFile file
    ) throws IncorrectOperationException {

        CommandProcessor.getInstance().runUndoTransparentAction(new Runnable() {
            @Override
            public void run() {
                createAction(project, editor).execute();
            }
        });
    }

    @NotNull
    private JetAddFunctionToClassAction createAction(Project project, Editor editor) {
        BindingContext bindingContext = KotlinCacheManagerUtil.getDeclarationsFromProject(element).getBindingContext();
        return new JetAddFunctionToClassAction(project, editor, bindingContext, functionsToAdd);
    }
}
