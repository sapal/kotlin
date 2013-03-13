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

package org.jetbrains.jet.plugin.internal.codewindow;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.Progress;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.internal.Location;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.util.LongRunningReadTask;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class BytecodeToolwindow extends JPanel implements Disposable {
    private static final int UPDATE_DELAY = 1000;
    private static final String DEFAULT_TEXT = "/*\n" +
                                               "Generated bytecode for Kotlin source file.\n" +
                                               "No Kotlin source file is opened.\n" +
                                               "*/";

    public class UpdateBytecodeToolWindowTask extends LongRunningReadTask<Location, String> {
        @Override
        protected Location prepareRequestInfo() {
            Location location = Location.fromEditor(FileEditorManager.getInstance(myProject).getSelectedTextEditor(), myProject);
            if (location.getEditor() == null || location.getJetFile() == null) {
                return null;
            }

            return location;
        }

        @NotNull
        @Override
        protected Location cloneRequestInfo(@NotNull Location location) {
            Location newLocation = super.cloneRequestInfo(location);
            assert location.equals(newLocation) : "cloneRequestInfo should generate same location object";
            return newLocation;
        }

        @Override
        protected void hideResultOnInvalidLocation() {
            setText(DEFAULT_TEXT);
        }

        @NotNull
        @Override
        protected String processRequest(@NotNull Location location) {
            JetFile jetFile = location.getJetFile();
            assert jetFile != null;

            GenerationState state;
            try {
                AnalyzeExhaust exhaust = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(jetFile);
                if (exhaust.isError()) {
                    return printStackTraceToString(exhaust.getError());
                }
                state = new GenerationState(jetFile.getProject(), ClassBuilderFactories.TEXT, Progress.DEAF, exhaust.getBindingContext(),
                                            Collections.singletonList(jetFile), BuiltinToJavaTypesMapping.ENABLED, true, true, true);
                KotlinCodegenFacade.compileCorrectFiles(state, CompilationErrorHandler.THROW_EXCEPTION);
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Exception e) {
                return printStackTraceToString(e);
            }

            StringBuilder answer = new StringBuilder();

            ClassFileFactory factory = state.getFactory();
            for (String filename : factory.files()) {
                answer.append("// ================");
                answer.append(filename);
                answer.append(" =================\n");
                answer.append(factory.asText(filename)).append("\n\n");
            }

            return answer.toString();
        }

        @Override
        protected void onResultReady(@NotNull Location requestInfo, String resultText) {
            Editor editor = requestInfo.getEditor();
            assert editor != null;

            if (resultText == null) {
                return;
            }

            setText(resultText);

            int fileStartOffset = requestInfo.getStartOffset();
            int fileEndOffset = requestInfo.getEndOffset();

            Document document = editor.getDocument();
            int startLine = document.getLineNumber(fileStartOffset);
            int endLine = document.getLineNumber(fileEndOffset);
            if (endLine > startLine && fileEndOffset > 0 && document.getCharsSequence().charAt(fileEndOffset - 1) == '\n') {
                endLine--;
            }

            Document byteCodeDocument = myEditor.getDocument();

            Pair<Integer, Integer> linesRange = mapLines(byteCodeDocument.getText(), startLine, endLine);
            int endSelectionLineIndex = Math.min(linesRange.second + 1, byteCodeDocument.getLineCount());

            int startOffset = byteCodeDocument.getLineStartOffset(linesRange.first);
            int endOffset = Math.min(byteCodeDocument.getLineStartOffset(endSelectionLineIndex), byteCodeDocument.getTextLength());

            myEditor.getCaretModel().moveToOffset(endOffset);
            myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
            myEditor.getCaretModel().moveToOffset(startOffset);
            myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);

            myEditor.getSelectionModel().setSelection(startOffset, endOffset);
        }
    }

    private final Editor myEditor;
    private final Alarm myUpdateAlarm;
    private final Project myProject;

    private UpdateBytecodeToolWindowTask currentTask = null;

    public BytecodeToolwindow(Project project) {
        super(new BorderLayout());
        myProject = project;
        myEditor = EditorFactory.getInstance().createEditor(
                EditorFactory.getInstance().createDocument(""), project, JavaFileType.INSTANCE, true);
        add(myEditor.getComponent());

        myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
        myUpdateAlarm.addRequest(new Runnable() {
            @Override
            public void run() {
                myUpdateAlarm.addRequest(this, UPDATE_DELAY);
                UpdateBytecodeToolWindowTask task = new UpdateBytecodeToolWindowTask();
                task.init();

                if (task.shouldStart(currentTask)) {
                    currentTask = task;
                    currentTask.run();
                }
            }
        }, UPDATE_DELAY);

        setText(DEFAULT_TEXT);
    }

    private static Pair<Integer, Integer> mapLines(String text, int startLine, int endLine) {
        int byteCodeLine = 0;
        int byteCodeStartLine = -1;
        int byteCodeEndLine = -1;

        List<Integer> lines = new ArrayList<Integer>();
        for (String line : text.split("\n")) {
            line = line.trim();

            if (line.startsWith("LINENUMBER")) {
                int ktLineNum = new Scanner(line.substring("LINENUMBER".length())).nextInt() - 1;
                lines.add(ktLineNum);
            }
        }
        Collections.sort(lines);

        for (Integer line : lines) {
            if (line >= startLine) {
                startLine = line;
                break;
            }
        }

        for (String line : text.split("\n")) {
            line = line.trim();

            if (line.startsWith("LINENUMBER")) {
                int ktLineNum = new Scanner(line.substring("LINENUMBER".length())).nextInt() - 1;

                if (byteCodeStartLine < 0 && ktLineNum == startLine) {
                    byteCodeStartLine = byteCodeLine;
                }

                if (byteCodeStartLine > 0 && ktLineNum > endLine) {
                    byteCodeEndLine = byteCodeLine - 1;
                    break;
                }
            }

            if (byteCodeStartLine >= 0 && (line.startsWith("MAXSTACK") || line.startsWith("LOCALVARIABLE") || line.isEmpty())) {
                byteCodeEndLine = byteCodeLine - 1;
                break;
            }


            byteCodeLine++;
        }


        if (byteCodeStartLine == -1 || byteCodeEndLine == -1) {
            return new Pair<Integer, Integer>(0, 0);
        }
        else {
            return new Pair<Integer, Integer>(byteCodeStartLine, byteCodeEndLine);
        }
    }

    private static String printStackTraceToString(Throwable e) {
        StringWriter out = new StringWriter(1024);
        PrintWriter printWriter = new PrintWriter(out);
        try {
            e.printStackTrace(printWriter);
            return out.toString().replace("\r", "");
        }
        finally {
            printWriter.close();
        }
    }

    private void setText(@NotNull final String resultText) {
        new WriteCommandAction(myProject) {
            @Override
            protected void run(Result result) throws Throwable {
                myEditor.getDocument().setText(resultText);
            }
        }.execute();
    }

    @Override
    public void dispose() {
        EditorFactory.getInstance().releaseEditor(myEditor);
    }
}