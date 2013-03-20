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

package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.references.BuiltInsReferenceResolver;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class DescriptorToDeclarationUtil {
    private DescriptorToDeclarationUtil() {
    }

    public static PsiElement getDeclaration(JetFile file, DeclarationDescriptor descriptor, BindingContext bindingContext) {
        Collection<PsiElement> elements = BindingContextUtils.descriptorToDeclarations(bindingContext, descriptor);

        if (elements.isEmpty()) {
            BuiltInsReferenceResolver libraryReferenceResolver =
                    file.getProject().getComponent(BuiltInsReferenceResolver.class);
            elements = libraryReferenceResolver.resolveStandardLibrarySymbol(descriptor);
        }

        if (!elements.isEmpty()) {
            return elements.iterator().next();
        }

        return null;
    }

    private static String displayableVisibility(MemberDescriptor descriptor) {
        Visibility visibility = descriptor.getVisibility().normalize();
        return visibility != Visibilities.INTERNAL ? visibility.toString() + " ": "";
    }

    private static String renderType(JetType type) {
        return DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(type);
    }

    private static void addReceiverParameter(CallableDescriptor descriptor, StringBuilder bodyBuilder) {
        ReceiverParameterDescriptor receiverParameter = descriptor.getReceiverParameter();
        if (receiverParameter != null) {
            bodyBuilder.append(receiverParameter.getType()).append(".");
        }
    }

    @NotNull
    public static String createOverridedFunctionSignatureStringFromDescriptor(
            @NotNull Project project,
            @NotNull SimpleFunctionDescriptor descriptor
    ) {
        return createOverridedFunctionDeclarationFromDescriptor(project, descriptor).getText().trim();
    }

    @NotNull
    public static JetNamedFunction createOverridedFunctionDeclarationFromDescriptor(@NotNull Project project, @NotNull SimpleFunctionDescriptor descriptor) {
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append(displayableVisibility(descriptor));
        bodyBuilder.append("override fun ");

        List<String> whereRestrictions = new ArrayList<String>();
        if (!descriptor.getTypeParameters().isEmpty()) {
            bodyBuilder.append("<");
            boolean first = true;
            for (TypeParameterDescriptor param : descriptor.getTypeParameters()) {
                if (!first) {
                    bodyBuilder.append(", ");
                }

                bodyBuilder.append(param.getName());
                Set<JetType> upperBounds = param.getUpperBounds();
                if (!upperBounds.isEmpty()) {
                    boolean firstUpperBound = true;
                    for (JetType upperBound : upperBounds) {
                        String upperBoundText = ": " + renderType(upperBound);
                        if (!KotlinBuiltIns.getInstance().getDefaultBound().equals(upperBound)) {
                            if (firstUpperBound) {
                                bodyBuilder.append(upperBoundText);
                            }
                            else {
                                whereRestrictions.add(param.getName() + upperBoundText);
                            }
                        }
                        firstUpperBound = false;
                    }
                }

                first = false;
            }
            bodyBuilder.append(">");
        }

        addReceiverParameter(descriptor, bodyBuilder);

        bodyBuilder.append(descriptor.getName()).append("(");
        //boolean isAbstractFun = descriptor.getModality() == Modality.ABSTRACT;
        //StringBuilder delegationBuilder = new StringBuilder();
        //if (isAbstractFun) {
        //    delegationBuilder.append("throw UnsupportedOperationException()");
        //}
        //else {
        //    delegationBuilder.append("super<").append(descriptor.getContainingDeclaration().getName());
        //    delegationBuilder.append(">.").append(descriptor.getName()).append("(");
        //}
        boolean first = true;
        for (ValueParameterDescriptor parameterDescriptor : descriptor.getValueParameters()) {
            if (!first) {
                bodyBuilder.append(", ");
                //if (!isAbstractFun) {
                //    delegationBuilder.append(", ");
                //}
            }
            first = false;
            bodyBuilder.append(parameterDescriptor.getName());
            bodyBuilder.append(": ");
            bodyBuilder.append(renderType(parameterDescriptor.getType()));

            //if (!isAbstractFun) {
            //    delegationBuilder.append(parameterDescriptor.getName());
            //}
        }
        bodyBuilder.append(")");
        //if (!isAbstractFun) {
        //    delegationBuilder.append(")");
        //}
        JetType returnType = descriptor.getReturnType();
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();

        boolean returnsNotUnit = returnType != null && !builtIns.getUnitType().equals(returnType);
        if (returnsNotUnit) {
            bodyBuilder.append(": ").append(renderType(returnType));
        }
        if (!whereRestrictions.isEmpty()) {
            bodyBuilder.append("\n").append("where ").append(StringUtil.join(whereRestrictions, ", "));
        }
        // bodyBuilder.append("{").append(returnsNotUnit && !isAbstractFun ? "return " : "").append(delegationBuilder.toString()).append("}");

        return JetPsiFactory.createFunction(project, bodyBuilder.toString());
    }
}
