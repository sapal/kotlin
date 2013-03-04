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

package org.jetbrains.jet.plugin.framework;

import com.google.common.collect.Sets;
import com.intellij.framework.library.LibraryVersionProperties;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.LibraryKind;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.LibraryEditor;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetPluginUtil;
import org.jetbrains.jet.plugin.framework.ui.CreateJavaScriptLibraryDialog;
import org.jetbrains.jet.plugin.framework.ui.FileUIUtils;
import org.jetbrains.jet.utils.KotlinPaths;
import org.jetbrains.jet.utils.PathUtil;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JSLibraryDescription extends CustomLibraryDescription {
    public static final LibraryKind KOTLIN_JAVASCRIPT_KIND = LibraryKind.create("kotlin-js-stdlib");
    public static final String LIBRARY_NAME = "KotlinJavaScript";

    private static final String JAVA_SCRIPT_LIBRARY_CREATION = "JavaScript Library Creation";

    @NotNull
    @Override
    public Set<? extends LibraryKind> getSuitableLibraryKinds() {
        return Sets.newHashSet(KOTLIN_JAVASCRIPT_KIND);
    }

    @Nullable
    @Override
    public NewLibraryConfiguration createNewLibrary(@NotNull JComponent parentComponent, @Nullable VirtualFile contextDirectory) {
        CreateJavaScriptLibraryDialog dialog = new CreateJavaScriptLibraryDialog(null, "Create Kotlin JavaScript Library", contextDirectory);
        dialog.show();

        if (dialog.isOK()) {
            KotlinPaths paths = PathUtil.getKotlinPathsForIdeaPlugin();

            File libraryFile = paths.getJsLibJarPath();
            if (!libraryFile.exists()) {
                Messages.showErrorDialog(String.format("JavaScript standard library was not found in %s", paths.getLibPath()),
                                         JAVA_SCRIPT_LIBRARY_CREATION);
                return null;
            }

            String libraryName = LIBRARY_NAME;

            String copyIntoPath = dialog.getCopyIntoPath();
            if (copyIntoPath != null) {
                List<File> copyFiles = new ArrayList<File>();

                if (dialog.isCopyLibraryFiles()) copyFiles.add(libraryFile);
                if (dialog.isCopyECMA3()) copyFiles.add(paths.getJsLibJsPath());

                Map<File,File> copiedFiles =
                        FileUIUtils.copyWithOverwriteDialog(parentComponent, JAVA_SCRIPT_LIBRARY_CREATION, copyIntoPath, copyFiles);
                if (copiedFiles == null) {
                    return null;
                }

                if (dialog.isCopyLibraryFiles()) {
                    libraryFile = copiedFiles.get(libraryFile);
                    libraryName = LIBRARY_NAME + "-" + JetPluginUtil.getPluginVersion();
                }
            }

            final String libraryFileUrl = VfsUtil.getUrlForLibraryRoot(libraryFile);
            return new NewLibraryConfiguration(libraryName, getDownloadableLibraryType(), new LibraryVersionProperties()) {
                @Override
                public void addRoots(@NotNull LibraryEditor editor) {
                    editor.addRoot(libraryFileUrl, OrderRootType.SOURCES);
                }
            };
        }

        return null;
    }
}
