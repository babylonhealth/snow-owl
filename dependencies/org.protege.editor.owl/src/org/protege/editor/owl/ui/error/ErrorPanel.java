package org.protege.editor.owl.ui.error;

import org.protege.editor.core.ui.error.ErrorExplainer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
/*
* Copyright (C) 2007, University of Manchester
*
* Modifications to the initial code base are copyright of their
* respective authors, or their employers as appropriate.  Authorship
* of the modifications may be determined from the ChangeLog placed at
* the end of this file.
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.

* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.

* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

/**
 * Author: drummond<br>
 * http://www.cs.man.ac.uk/~drummond/<br><br>
 * <p/>
 * The University Of Manchester<br>
 * Bio Health Informatics Group<br>
 * Date: Dec 11, 2008<br><br>
 */
public class ErrorPanel<O extends Throwable> extends JPanel {

    private JTextComponent stackTracePane;

    protected JComponent errorMessageComponent;

    private JComponent stackTraceComponent;

    private JTabbedPane tabs;


    public ErrorPanel(final ErrorExplainer.ErrorExplanation<O> explanation, URI loc) {

        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(7, 7, 7, 7));

        JEditorPane errorMessagePanel = new JEditorPane();
        errorMessagePanel.setEditable(false);
        errorMessagePanel.setText(explanation.getMessage());
        errorMessagePanel.setBorder(new EmptyBorder(7, 7, 7, 7));
        JScrollPane errorScroller = new JScrollPane(errorMessagePanel);

        stackTracePane = new JTextArea(20, 60);
        setErrorMessage(stackTracePane, explanation.getCause());
        stackTracePane.setBorder(new EmptyBorder(7, 7, 7, 7));
        JScrollPane stackTraceScroller = new JScrollPane(stackTracePane);

        tabs = new JTabbedPane();
        tabs.addTab("Error", errorScroller);
        tabs.addTab("Stack Trace", stackTraceScroller);

        add(tabs, BorderLayout.CENTER);
    }


    protected void setErrorMessage(JTextComponent component, Throwable exception) {

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter writer = new PrintWriter(stringWriter);

        final String message = exception.getMessage();
        if (message != null){
            writer.write(message);
            writer.write("\n\n\n");
        }
        writer.write("Full Stack Trace\n-----------------------------------------------------------------------------------------\n\n");

        exception.printStackTrace(writer);
        writer.flush();
        component.setText(stringWriter.toString());
        component.setCaretPosition(0);
    }


    protected JComponent getTabs(){
        return tabs;
    }
}
