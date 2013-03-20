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

import com.google.common.collect.Maps;
import com.intellij.codeInsight.hint.HintManager;
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
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.actions.JetChangeFunctionSignatureAction;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManager;
import org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Fix that changes member function's signature to match one of super functions' signatures.
 */
public class ChangeMemberFunctionSignatureFix extends JetHintAction<JetNamedFunction> {
    private final List<SimpleFunctionDescriptor> possibleSignatures;

    public ChangeMemberFunctionSignatureFix(@NotNull JetNamedFunction element) {
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
            return JetBundle.message("change.function.signature.action.single",
                                     getFunctionSignatureString(possibleSignatures.get(0)));
        else
            return JetBundle.message("change.function.signature.action.multiple");
    }

    @NotNull
    private String getFunctionSignatureString(@NotNull SimpleFunctionDescriptor functionSignature) {
        return DescriptorToDeclarationUtil.createOverridedFunctionSignatureStringFromDescriptor(element.getProject(), functionSignature);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.function.signature.family");
    }

    @Override
    public void invoke(@NotNull final Project project, @NotNull final Editor editor, PsiFile file)
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

    /**
     * Computes all the signatures a 'functionElement' could be changed to in order to remove NOTHING_TO_OVERRIDE error.
     */
    @NotNull
    private List<SimpleFunctionDescriptor> computePossibleSignatures(JetNamedFunction functionElement) {
        Project project = functionElement.getProject();
        BindingContext context = KotlinCacheManager.getInstance(project).getDeclarationsFromProject().getBindingContext();
        SimpleFunctionDescriptor functionDescriptor = context.get(BindingContext.FUNCTION, functionElement);
        assert functionDescriptor != null;
        List<SimpleFunctionDescriptor> superFunctions = getPossibleSuperFunctionsDescriptors(functionDescriptor);
        Map<String, SimpleFunctionDescriptor> possibleSignatures = Maps.newHashMap();
        for (SimpleFunctionDescriptor superFunction : superFunctions) {
            if (!superFunction.getKind().isReal()) continue;
            SimpleFunctionDescriptor signature = changeSignatureToMatch(functionDescriptor, superFunction);
            possibleSignatures.put(getFunctionSignatureString(signature), signature);
        }
        return new ArrayList<SimpleFunctionDescriptor>(possibleSignatures.values());
    }

    /**
     *  Changes function's signature to match superFunction's signature. Returns new descriptor.
     */
    private static SimpleFunctionDescriptor changeSignatureToMatch(SimpleFunctionDescriptor function, SimpleFunctionDescriptor superFunction) {
        List<ValueParameterDescriptor> superParameters = superFunction.getValueParameters();
        List<ValueParameterDescriptor> parameters = function.getValueParameters();
        List<ValueParameterDescriptor> newParameters = new ArrayList<ValueParameterDescriptor>(superParameters);

        // Parameters in superFunction, which are matched in new method signature:
        boolean[] matched = new boolean[superParameters.size()];
        // Parameters in this method, which are used in new method signature:
        boolean[] used = new boolean[parameters.size()];

        // Match parameters with the same name (but possibly different types):
        int superIdx = 0;
        for (ValueParameterDescriptor superParameter : superParameters) {
            int idx = 0;
            Name superName = superParameter.getName();
            for (ValueParameterDescriptor parameter : parameters) {
                Name name = parameter.getName();
                if (!used[idx] && name.equals(superName)) {
                    used[idx] = true;
                    matched[superIdx] = true;
                    break;
                }
                idx++;
            }
            superIdx++;
        }

        // Match parameters with the same type (but possibly different names). Preserve ordering:
        superIdx = 0;
        for (ValueParameterDescriptor superParameter : superParameters) {
            if (matched[superIdx]) continue;
            int idx = 0;
            JetType superParameterType = superParameter.getType();
            for (ValueParameterDescriptor parameter : parameters) {
                JetType parameterType = parameter.getType();
                if (!used[idx] && JetTypeChecker.INSTANCE.equalTypes(superParameterType, parameterType)) {
                    used[idx] = true;
                    matched[superIdx] = true;
                    newParameters.set(superIdx, parameter);
                    break;
                }
                idx++;
            }
            superIdx++;
        }

        Visibility superVisibility = superFunction.getVisibility();
        Visibility visibility = function.getVisibility();
        Visibility newVisibility = superVisibility;
        // If function has greater visibility than super function, keep function's visibility:
        Integer compareVisibilities = Visibilities.compare(visibility, superVisibility);
        if (compareVisibilities != null && compareVisibilities > 0) {
            newVisibility = visibility;
        }
        return DescriptorUtils.replaceValueParameters(
                superFunction.copy(
                        function.getContainingDeclaration(),
                        Modality.OPEN,
                        newVisibility,
                        CallableMemberDescriptor.Kind.DELEGATION,
                        /* copyOverrides = */ true),
                DescriptorUtils.fixParametersIndexes(newParameters));
    }

    /**
     * Returns all open functions in superclasses which have the same name as 'functionDescriptor' (but possibly
     * different parameters/return type).
     */
    @NotNull
    private static List<SimpleFunctionDescriptor> getPossibleSuperFunctionsDescriptors(@NotNull SimpleFunctionDescriptor functionDescriptor) {
        DeclarationDescriptor containingDeclaration = functionDescriptor.getContainingDeclaration();
        List<SimpleFunctionDescriptor> superFunctions = new LinkedList<SimpleFunctionDescriptor>();
        if (!(containingDeclaration instanceof ClassDescriptor)) return superFunctions;
        ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;

        Name name = functionDescriptor.getName();
        for (ClassDescriptor superclass : DescriptorUtils.getSuperclassDescriptors(classDescriptor)) {
            JetType type = superclass.getDefaultType();
            JetScope scope = type.getMemberScope();
            for (FunctionDescriptor function : scope.getFunctions(name)) {
                if (!function.getKind().isReal()) continue;
                assert function instanceof SimpleFunctionDescriptor;
                SimpleFunctionDescriptor simpleFunctionDescriptor = (SimpleFunctionDescriptor) function;
                if (simpleFunctionDescriptor.getModality().isOverridable()) 
                    superFunctions.add(simpleFunctionDescriptor);
            }
        }
        return superFunctions;
    }

    @NotNull
    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetNamedFunction function = QuickFixUtil.getParentElementOfType(diagnostic, JetNamedFunction.class);
                return function == null ? null : new ChangeMemberFunctionSignatureFix(function);
            }
        };
    }

    @Override
    public boolean showHint(@NotNull Editor editor) {
        if (possibleSignatures.isEmpty()) {
            return false;
        }

        Project project = editor.getProject();
        if (project == null) {
            return false;
        }

        if (HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(true)) {
            return false;
        }

        return true;
    }
}
