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

package org.jetbrains.jet.lang.psi;

import com.google.common.collect.Maps;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.ImportPath;

import javax.inject.Inject;
import java.util.Map;

public class JetPsiBuilder {
    @NotNull
    private Project project;

    private final Map<ImportPath, JetImportDirective> importsCache = Maps.newHashMap();

    @Inject
    public void setProject(@NotNull Project project) {
        importsCache.clear();
        this.project = project;
    }

    public JetImportDirective createImportDirective(@NotNull ImportPath importPath) {
        JetImportDirective directive = importsCache.get(importPath);
        if (directive != null) {
            return directive;
        }

        JetImportDirective createdDirective = JetPsiFactory.createImportDirective(project, importPath);
        importsCache.put(importPath, createdDirective);

        return createdDirective;
    }
}
