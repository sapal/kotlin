package org.jetbrains.jet.plugin.framework.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CreateJavaScriptLibraryDialog extends DialogWrapper {
    private final CopyIntoPanel copyIntoPanel;

    private JPanel contentPane;
    private JCheckBox copyLibraryCheckbox;
    private JCheckBox ECMAScript3JavaScriptRuntimeCheckBox;
    private JPanel compilerSourcePanelPlace;
    private JPanel copyIntoPanelPlace;

    public CreateJavaScriptLibraryDialog(@Nullable Project project, @NotNull String title, VirtualFile contextDirectory) {
        super(project);

        setTitle(title);

        init();

        ChooseCompilerSourcePanel compilerSourcePanel = new ChooseCompilerSourcePanel();
        compilerSourcePanelPlace.add(compilerSourcePanel.getContentPane(), BorderLayout.CENTER);

        copyIntoPanel = new CopyIntoPanel(project, contextDirectory);
        copyIntoPanel.addValidityListener(new ValidityListener() {
            @Override
            public void validityChanged(boolean isValid) {
                updateComponents();
            }
        });
        copyIntoPanelPlace.add(copyIntoPanel.getContentPane(), BorderLayout.CENTER);

        ActionListener updateComponentsListener = new ActionListener() {
            @Override
            public void actionPerformed(@NotNull ActionEvent e) {
                updateComponents();
            }
        };

        copyLibraryCheckbox.addActionListener(updateComponentsListener);
        ECMAScript3JavaScriptRuntimeCheckBox.addActionListener(updateComponentsListener);

        updateComponents();
    }

    @Nullable
    public String getCopyIntoPath() {
        return copyIntoPanel.getPath();
    }

    public boolean isCopyLibraryFiles() {
        return copyLibraryCheckbox.isSelected();
    }

    public boolean isCopyECMA3() {
        return ECMAScript3JavaScriptRuntimeCheckBox.isSelected();
    }

    private void updateComponents() {
        copyIntoPanel.setEnabled(copyLibraryCheckbox.isSelected() ||
                                 ECMAScript3JavaScriptRuntimeCheckBox.isSelected());

        setOKActionEnabled(!copyIntoPanel.hasErrors());
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }
}
