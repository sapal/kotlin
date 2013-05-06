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
import com.google.common.collect.Sets;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;

import java.util.List;
import java.util.Set;

public class AddNameToArgumentFix extends JetIntentionAction<JetValueArgument> {

    @NotNull
    private final List<String> possibleNames;

    public AddNameToArgumentFix(@NotNull JetValueArgument argument, @NotNull List<String> possibleNames) {
        super(argument);
        this.possibleNames = possibleNames;
    }

    @NotNull
    private static List<String> generatePossibleNames(@NotNull JetValueArgument argument) {
        Set<String> names = Sets.newHashSet();
        JetCallElement callElement = PsiTreeUtil.getParentOfType(argument, JetCallElement.class);
        assert callElement != null : "The argument has to be inside a function call";
        BindingContext context = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((JetFile) argument.getContainingFile()).getBindingContext();
        JetExpression callee = callElement.getCalleeExpression();
        if (!(callee instanceof JetReferenceExpression)) return Lists.newArrayList();
        ResolvedCall<? extends CallableDescriptor> resolvedCall =
                context.get(BindingContext.RESOLVED_CALL, (JetReferenceExpression) callee);
        if (resolvedCall == null) return Lists.newArrayList();
        CallableDescriptor callableDescriptor = resolvedCall.getResultingDescriptor();
        JetType type = context.get(BindingContext.EXPRESSION_TYPE, argument.getArgumentExpression());
        for (ValueParameterDescriptor parameter: callableDescriptor.getValueParameters()) {
            if (type == null || TypeUtils.equalTypes(parameter.getType(), type)) names.add(parameter.getName().getName());
        }
        return Lists.newArrayList(names);
    }

    @Override
    protected void invoke(@NotNull Project project, Editor editor, JetFile file) {
        addName(project, element, possibleNames.get(0));
    }

    private static void addName(@NotNull Project project, JetValueArgument argument, String name) {
        JetValueArgument newArgument =
                JetPsiFactory.createCallArguments(project, "(" + name + "=" + getArgumentExpression(argument).getText()+")").getArguments().get(0);
        argument.replace(newArgument);
    }

    @NotNull
    @Override
    public String getText() {
        if (possibleNames.size() == 1) {
            JetExpression argumentExpression = getArgumentExpression(element);
            return JetBundle.message("add.name.to.argument.single", possibleNames.get(0), argumentExpression.getText());
        } else {
            return JetBundle.message("add.name.to.argument.multiple");
        }
    }

    @NotNull
    private static JetExpression getArgumentExpression(@NotNull JetValueArgument argument) {
        JetExpression argumentExpression = argument.getArgumentExpression();
        assert argumentExpression != null : "Element must have an expression - it should be already parsed";
        return argumentExpression;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("add.name.to.argument.family");
    }
    @NotNull
    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetValueArgument argument = QuickFixUtil.getParentElementOfType(diagnostic, JetValueArgument.class);
                if (argument == null) return null;
                List<String> possibleNames = generatePossibleNames(argument);
                return possibleNames.isEmpty() ? null : new AddNameToArgumentFix(argument, possibleNames);
            }
        };
    }
}
