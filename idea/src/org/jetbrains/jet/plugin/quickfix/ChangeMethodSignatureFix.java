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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetParameterList;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManager;

import java.util.HashSet;
import java.util.Set;

public class ChangeMethodSignatureFix extends JetIntentionAction<JetNamedFunction>{

    private Set<FunctionDescriptor> supermethodsDescriptors;
    private String methodSignature;

    public ChangeMethodSignatureFix(@NotNull JetNamedFunction element) {
        super(element);
    }


    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file)) {
            return false;
        }

        BindingContext context = KotlinCacheManager.getInstance(project).getDeclarationsFromProject().getBindingContext();
        SimpleFunctionDescriptor functionDescriptor = context.get(BindingContext.FUNCTION, element);
        assert functionDescriptor != null;
        DeclarationDescriptor containingDeclaration = functionDescriptor.getContainingDeclaration();
        if (!(containingDeclaration instanceof ClassDescriptor)) return false;
        ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
        prepareSupermethodDescriptors(classDescriptor);
        if (!supermethodsDescriptors.isEmpty()) {
            JetNamedFunction newElement = getNewFunctionElement(element, project, getFunctionDescriptor());

            JetParameterList parameterList = newElement.getValueParameterList();
            if (parameterList == null) return false;
            methodSignature = newElement.getName() + parameterList.getText();
        }
        return !supermethodsDescriptors.isEmpty();
    }

    private void prepareSupermethodDescriptors(@NotNull ClassDescriptor descriptor) {
        supermethodsDescriptors = new HashSet<FunctionDescriptor>();
        Name name = element.getNameAsName();
        assert name != null;
        for (ClassDescriptor superclass : DescriptorUtils.getSuperclassDescriptors(descriptor)) {
            JetType type = superclass.getDefaultType();
            JetScope scope = type.getMemberScope();
            supermethodsDescriptors.addAll(scope.getFunctions(name)); // TODO: only open methods
        }
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("change.method.signature.action", newMethodSignature());
    }

    @NotNull
    private String newMethodSignature() {
        return methodSignature;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.method.signature.family");
    }

    @NotNull
    private static JetNamedFunction getNewFunctionElement(@NotNull JetNamedFunction element, @NotNull Project project,
            @NotNull FunctionDescriptor function) {
        JetNamedFunction newElement = (JetNamedFunction) element.copy();
        JetParameterList newParameters = newElement.getValueParameterList();
        assert newParameters != null;
        newParameters.deleteChildRange(newParameters.getFirstChild(), newParameters.getLastChild());
        for (ValueParameterDescriptor parameter : function.getValueParameters()) {
            newParameters.add(JetPsiFactory.createParameter(project, parameter.getName().getName(), parameter.getType().toString()));
        }
        newParameters.addBefore(JetPsiFactory.createCallArguments(project, "()").getFirstChild(), newParameters.getFirstChild());
        newParameters.addAfter(JetPsiFactory.createCallArguments(project, "()").getLastChild(), newParameters.getLastChild());
        return newElement;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        FunctionDescriptor function = getFunctionDescriptor();
        element.replace(getNewFunctionElement(element, project, function));
    }

    private FunctionDescriptor getFunctionDescriptor() {
        return supermethodsDescriptors.iterator().next();
    }

    @NotNull
    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetNamedFunction function = QuickFixUtil.getParentElementOfType(diagnostic, JetNamedFunction.class);
                return function == null ? null : new ChangeMethodSignatureFix(function);
            }
        };
    }
}
