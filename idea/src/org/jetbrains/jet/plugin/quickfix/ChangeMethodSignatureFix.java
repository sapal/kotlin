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

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
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
import org.jetbrains.jet.plugin.actions.JetChangeMethodSignatureAction;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManager;

import java.util.LinkedList;
import java.util.List;

public class ChangeMethodSignatureFix extends JetHintAction<JetNamedFunction> {
    private final List<JetNamedFunction> possibleSignatures;

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
            return JetBundle.message("change.method.signature.action.single", possibleSignatures.get(0).getText().trim());
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
    private JetChangeMethodSignatureAction createAction(@NotNull Project project, @NotNull Editor editor) {
        return new JetChangeMethodSignatureAction(project, editor, element, possibleSignatures);
    }

    @NotNull
    private static List<JetNamedFunction> computePossibleSignatures(JetNamedFunction functionElement) {
        Project project = functionElement.getProject();
        BindingContext context = KotlinCacheManager.getInstance(project).getDeclarationsFromProject().getBindingContext();
        SimpleFunctionDescriptor functionDescriptor = context.get(BindingContext.FUNCTION, functionElement);
        assert functionDescriptor != null;
        List<FunctionDescriptor> supermethods = getPossibleSupermethodsDescriptors(functionDescriptor);
        List<JetNamedFunction> possibleSignatures = new LinkedList<JetNamedFunction>();
        for (FunctionDescriptor supermethod : supermethods) {
            PsiElement declaration = BindingContextUtils.descriptorToDeclaration(context, supermethod);
            if (!(declaration instanceof JetNamedFunction)) continue;
            JetNamedFunction supermethodElement = (JetNamedFunction) declaration;
            possibleSignatures.add(changeSignatureToMatch(functionElement, supermethodElement));
        }
        return possibleSignatures;
    }


    private static JetNamedFunction changeSignatureToMatch(JetNamedFunction functionElement, JetNamedFunction supermethod) {
        JetNamedFunction newElement = (JetNamedFunction)supermethod.copy();
        leaveOnlySignature(newElement);

        Project project = functionElement.getProject();
        changeModifiersToOverride(project, newElement);

        JetParameterList superParameterList = newElement.getValueParameterList();
        assert superParameterList != null;
        List<JetParameter> superParameters = superParameterList.getParameters();

        JetParameterList parameterList = functionElement.getValueParameterList();
        assert parameterList != null;
        List<JetParameter> parameters = parameterList.getParameters();

        boolean[] matched = new boolean[superParameters.size()];
        boolean[] used = new boolean[parameters.size()];
        int superIdx = 0;
        for (JetParameter superParameter : superParameters) {
            int idx = 0;
            Name superName = superParameter.getNameAsSafeName();
            for (JetParameter parameter : parameters) {
                Name name = parameter.getNameAsSafeName();
                if (!used[idx] && name.equals(superName)) {
                    used[idx] = true;
                    matched[superIdx] = true;
                    break;
                }
                idx++;
            }
            superIdx++;
        }

        superIdx = 0;
        for (JetParameter superParameter : superParameters) {
            if (matched[superIdx]) continue;
            int idx = 0;
            JetTypeReference superTypeReference = superParameter.getTypeReference();
            assert superTypeReference != null;
            JetTypeElement superType = superTypeReference.getTypeElement();
            assert superType != null;
            for (JetParameter parameter : parameters) {
                JetTypeReference typeReference = parameter.getTypeReference();
                assert typeReference != null;
                JetTypeElement type = typeReference.getTypeElement();
                assert type != null;
                if (!used[idx] && type.getText().equals(superType.getText())) {
                    used[idx] = true;
                    matched[superIdx] = true;
                    superParameter.replace(parameter);
                    break;
                }
                idx++;
            }
            superIdx++;
        }
        return newElement;
    }

    private static void changeModifiersToOverride(Project project, JetNamedFunction functionElement) {
        PsiElement overrideModifier = JetPsiFactory.createModifier(project, JetTokens.OVERRIDE_KEYWORD).getFirstChild();
        JetModifierList modifierList = functionElement.getModifierList();
        assert modifierList != null;
        List<JetKeywordToken> removeModifiers = new LinkedList<JetKeywordToken>();
        removeModifiers.add(JetTokens.ABSTRACT_KEYWORD);
        removeModifiers.add(JetTokens.OPEN_KEYWORD);
        removeModifiers.add(JetTokens.OVERRIDE_KEYWORD);


        PsiElement replaceNode = null;
        for (JetKeywordToken modifier : removeModifiers) {
           ASTNode modifierNode = modifierList.getModifierNode(modifier);
           if (modifierNode != null) {
               PsiElement modifierPsi = modifierNode.getPsi();
               if (replaceNode == null) {
                   replaceNode = modifierPsi;
               }
               else {
                   modifierPsi.delete();
               }
           }
        }
        if (replaceNode == null) {
            modifierList.addAfter(overrideModifier, modifierList.getLastChild());
        }
        else {
            replaceNode.replace(overrideModifier);
        }
    }

    private static void leaveOnlySignature(JetNamedFunction functionElement) {
        JetExpression bodyExpression = functionElement.getBodyExpression();
        if (bodyExpression != null) bodyExpression.delete();

        PsiElement tail = functionElement.getLastChild();
        while(true) {
            if (tail.textMatches("=") || tail.textMatches(";") || tail instanceof PsiWhiteSpace)  tail.delete();
            else break;
            tail = functionElement.getLastChild();
        }
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
