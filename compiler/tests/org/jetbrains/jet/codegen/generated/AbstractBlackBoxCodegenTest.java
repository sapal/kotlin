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

package org.jetbrains.jet.codegen.generated;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.codegen.CodegenTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractBlackBoxCodegenTest extends CodegenTestCase {
    public void doTest(@NotNull String filename) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFileByFullPath(filename);
    }

    public void doTestWithJava(@NotNull String filename) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        blackBoxFileWithJavaByFullPath(filename);
    }

    public void doTestWithStdlib(@NotNull String filename) {
        myEnvironment = JetTestUtils.createEnvironmentWithFullJdk(getTestRootDisposable());
        blackBoxFileByFullPath(filename);
    }

    public void doTestMultiFile(@NotNull String folderName) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL);

        final List<String> files = new ArrayList<String>(2);
        FileUtil.processFilesRecursively(new File(folderName), new Processor<File>() {
            @Override
            public boolean process(File file) {
                if (file.getName().endsWith(".kt")) {
                    files.add(file.getPath());
                }
                return true;
            }
        });

        blackBoxMultiFileByFullPath(files.toArray(new String[files.size()]));
    }
}
