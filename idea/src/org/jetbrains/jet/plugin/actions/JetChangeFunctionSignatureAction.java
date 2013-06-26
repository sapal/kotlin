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

import com.google.common.collect.Lists;
import com.intellij.codeInsight.hint.QuestionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManagerUtil;
import org.jetbrains.jet.plugin.codeInsight.CodeInsightUtils;
import org.jetbrains.jet.plugin.refactoring.changeSignature.*;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Changes method signature to one of provided signatures.
 * Based on {@link JetAddImportAction}
 */
public class JetChangeFunctionSignatureAction implements QuestionAction {

    private final Project project;
    private final Editor editor;
    private final JetNamedFunction element;
    private final List<SignatureChange> signatures;

    /**
     * @param project    Project where action takes place.
     * @param editor     Editor where modification should be done.
     * @param element    Function element which signature should be changed.
     * @param signatures Variants for new function signature.
     */
    public JetChangeFunctionSignatureAction(
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull JetNamedFunction element,
            @NotNull Collection<SignatureChange> signatures
    ) {
        this.project = project;
        this.editor = editor;
        this.element = element;
        this.signatures = new ArrayList<SignatureChange>(signatures);
    }

    private static void changeSignature(final JetNamedFunction element, final Project project, final SignatureChange signatureChange) {

        PsiDocumentManager.getInstance(project).commitAllDocuments();
        BindingContext context = KotlinCacheManagerUtil.getDeclarationsFromProject(element).getBindingContext();
        final FunctionDescriptor currentSignature = context.get(BindingContext.FUNCTION, element);
        assert currentSignature != null : "current signature should be correct";

        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        FunctionDescriptor signature = signatureChange.getNewSignature();
                        JetFunctionPlatformDescriptor platformDescriptor = new JetFunctionPlatformDescriptorImpl(currentSignature, element);
                        JetChangeSignatureDialog dialog = new JetChangeSignatureDialog(
                                project,
                                platformDescriptor,
                                element,
                                getSignatureString(signature));

                        dialog.setParameterInfos(generateParameterInfos(signatureChange));
                        dialog.setReturnType(signature.getReturnType());
                        dialog.setVisibility(signature.getVisibility());
                        if (ApplicationManager.getApplication().isUnitTestMode()) {
                            performRefactoringSilently(project, dialog, getSignatureString(signature));
                        }
                        else {
                            dialog.show();
                        }
                    }
                });
            }
        }, JetBundle.message("change.function.signature.action"), null);
    }

    private static List<JetParameterInfo> generateParameterInfos(SignatureChange signatureChange) {
        List<JetParameterInfo> parameterInfos = Lists.newArrayList();
        List<Integer> originalParameterIndices = signatureChange.getOriginalParametersIndices();
        int idx = 0;
        for (ValueParameterDescriptor parameter : signatureChange.getNewSignature().getValueParameters()) {
            Integer originalIndex = originalParameterIndices.get(idx);
            if (originalIndex != null) {
                parameterInfos.add(new JetParameterInfo(
                        originalIndex,
                        parameter.getName().asString(),
                        parameter.getType(),
                        null,
                        null));
            }
            else {
                parameterInfos.add(new JetParameterInfo(parameter.getName().asString(), parameter.getType()));
            }
            idx++;
        }
        return parameterInfos;
    }

    private static String getSignatureString(FunctionDescriptor signature) {
        return CodeInsightUtils.createFunctionSignatureStringFromDescriptor(signature, /* shortTypeNames = */ true);
    }

    private static void performRefactoringSilently(final Project project, final JetChangeSignatureDialog dialog, final String actionName) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                JetChangeInfo changeInfo = dialog.evaluateChangeInfo();
                JetChangeSignatureProcessor processor = new JetChangeSignatureProcessor(project, changeInfo, actionName);
                processor.run();
            }
        });
    }

    @Override
    public boolean execute() {
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        if (!element.isValid() || signatures.isEmpty()) {
            return false;
        }

        if (signatures.size() == 1 || !editor.getComponent().isShowing()) {
            changeSignature(element, project, signatures.get(0));
        }
        else {
            chooseSignatureAndChange();
        }

        return true;
    }

    private BaseListPopupStep getSignaturePopup() {
        return new BaseListPopupStep<SignatureChange>(
                JetBundle.message("change.function.signature.chooser.title"), signatures) {
            @Override
            public boolean isAutoSelectionEnabled() {
                return false;
            }

            @Override
            public PopupStep onChosen(SignatureChange selectedValue, boolean finalChoice) {
                if (finalChoice) {
                    changeSignature(element, project, selectedValue);
                }
                return FINAL_CHOICE;
            }

            @Override
            public Icon getIconFor(SignatureChange aValue) {
                return PlatformIcons.FUNCTION_ICON;
            }

            @NotNull
            @Override
            public String getTextFor(SignatureChange aValue) {
                return getSignatureString(aValue.getNewSignature());
            }
        };
    }

    private void chooseSignatureAndChange() {
        JBPopupFactory.getInstance().createListPopup(getSignaturePopup()).showInBestPositionFor(editor);
    }

    public static class SignatureChange {
        private final FunctionDescriptor newSignature;
        private final List<Integer> originalParametersIndices;

        public SignatureChange(FunctionDescriptor newSignature, List<Integer> originalParametersIndices) {
            this.newSignature = newSignature;
            this.originalParametersIndices = originalParametersIndices;
        }

        public FunctionDescriptor getNewSignature() {
            return newSignature;
        }

        public List<Integer> getOriginalParametersIndices() {
            return originalParametersIndices;
        }
    }
}
