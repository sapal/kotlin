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

import com.intellij.codeInsight.daemon.impl.ShowAutoImportPass;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.actions.JetChangeFunctionSignatureAction;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManager;

import java.util.*;

public class ChangeMethodSignatureFix extends JetHintAction<JetNamedFunction> {
    private final List<String> possibleSignatures;

    public ChangeMethodSignatureFix(@NotNull JetNamedFunction element) {
        super(element);
        this.possibleSignatures = computePossibleSignatures(element);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file) && !possibleSignatures.isEmpty();
    }

    @NotNull
    @Override
    public String getText() {
        if (possibleSignatures.size() == 1)
            return JetBundle.message("change.method.signature.action.single", possibleSignatures.get(0));
        else
            return JetBundle.message("change.method.signature.action.multiple");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.method.signature.family");
    }

    @Override
    public void invoke(@NotNull final Project project, @NotNull final Editor editor, final PsiFile file)
            throws IncorrectOperationException {
        CommandProcessor.getInstance().runUndoTransparentAction(new Runnable() {
            @Override
            public void run() {
                createAction(project, editor).execute();
            }
        });
    }

    @NotNull
    private JetChangeFunctionSignatureAction createAction(@NotNull Project project, @NotNull Editor editor) {
        return new JetChangeFunctionSignatureAction(project, editor, element, possibleSignatures);
    }

    @NotNull
    private static List<String> computePossibleSignatures(JetNamedFunction functionElement) {
        Project project = functionElement.getProject();
        BindingContext context = KotlinCacheManager.getInstance(project).getDeclarationsFromProject().getBindingContext();
        SimpleFunctionDescriptor functionDescriptor = context.get(BindingContext.FUNCTION, functionElement);
        assert functionDescriptor != null;
        List<FunctionDescriptor> supermethods = getPossibleSupermethodsDescriptors(functionDescriptor);
        List<String> possibleSignatures = new LinkedList<String>();
        for (FunctionDescriptor supermethod : supermethods) {
            PsiElement declaration = BindingContextUtils.descriptorToDeclaration(context, supermethod);
            if (!(declaration instanceof JetNamedFunction)) continue;
            JetNamedFunction supermethodElement = (JetNamedFunction) declaration;
            possibleSignatures.add(getSignature(supermethodElement, functionElement));
        }
        return possibleSignatures;
    }

    private static String getSignature(JetNamedFunction supermethod, JetNamedFunction functionElement) {
        JetNamedFunction newElement = (JetNamedFunction)supermethod.copy();
        JetExpression bodyExpression = newElement.getBodyExpression();
        if (bodyExpression != null) bodyExpression.delete();

        Project project = functionElement.getProject();
        PsiElement overrideModifier = JetPsiFactory.createModifier(project, JetTokens.OVERRIDE_KEYWORD).getFirstChild();
        JetModifierList modifierList = newElement.getModifierList();
        assert modifierList != null;
        ASTNode openModifierNode = modifierList.getModifierNode(JetTokens.OPEN_KEYWORD);
        assert openModifierNode != null;
        PsiElement openModifier = openModifierNode.getPsi();
        assert openModifier != null;
        openModifier.replace(overrideModifier);

        return newElement.getText();
    }

    private static List<FunctionDescriptor> getPossibleSupermethodsDescriptors(SimpleFunctionDescriptor functionDescriptor) {
        DeclarationDescriptor containingDeclaration = functionDescriptor.getContainingDeclaration();
        List<FunctionDescriptor> supermethods = new LinkedList<FunctionDescriptor>();
        if (!(containingDeclaration instanceof ClassDescriptor)) return supermethods;
        ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;

        Name name = functionDescriptor.getName();
        for (ClassDescriptor superclass : DescriptorUtils.getSuperclassDescriptors(classDescriptor)) {
            JetType type = superclass.getDefaultType();
            JetScope scope = type.getMemberScope();
            for (FunctionDescriptor function : scope.getFunctions(name)) {
                if (function.getModality().isOverridable()) supermethods.add(function);
            }
        }
        return supermethods;
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

    @Override
    public boolean showHint(Editor editor) {
        if (possibleSignatures.isEmpty()) {
            return false;
        }

        final Project project = editor.getProject();
        if (project == null) {
            return false;
        }

        if (HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(true)) {
            return false;
        }

        return true;
    }
}
