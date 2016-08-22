package org.protege.editor.owl.ui.editor;

import org.protege.editor.core.ui.util.InputVerificationStatusChangedListener;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.cache.OWLExpressionUserCache;
import org.protege.editor.owl.ui.clsdescriptioneditor.ExpressionEditor;
import org.protege.editor.owl.ui.clsdescriptioneditor.OWLExpressionChecker;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLException;

import javax.swing.*;
import java.util.Collections;
import java.util.Set;
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
 * Date: Feb 26, 2009<br><br>
 */
public class OWLClassExpressionExpressionEditor extends AbstractOWLClassExpressionEditor{

    private ExpressionEditor<OWLClassExpression> editor;

    private JScrollPane scroller;

    public void initialise() throws Exception {
        final OWLEditorKit eKit = getOWLEditorKit();
        final OWLExpressionChecker<OWLClassExpression> checker = eKit.getModelManager().getOWLExpressionCheckerFactory().getOWLClassExpressionChecker();
        editor = new ExpressionEditor<OWLClassExpression>(eKit, checker);

        scroller = new JScrollPane(editor);
    }


    public JComponent getComponent() {
        return scroller;
    }


    public boolean isValidInput() {
        return editor.isWellFormed();
    }


    public boolean setDescription(OWLClassExpression description) {
        editor.setExpressionObject(description);
        return true;
    }


    public Set<OWLClassExpression> getClassExpressions() {
        try {
            if (editor.isWellFormed()) {
                OWLClassExpression owlDescription = editor.createObject();
                OWLExpressionUserCache.getInstance(getOWLEditorKit().getModelManager()).add(owlDescription, editor.getText());
                return Collections.singleton(owlDescription);
            }
            else {
                return null;
            }
        }
        catch (OWLException e) {
            return null;
        }
    }


    public void addStatusChangedListener(InputVerificationStatusChangedListener l) {
        editor.addStatusChangedListener(l);
    }


    public void removeStatusChangedListener(InputVerificationStatusChangedListener l) {
        editor.removeStatusChangedListener(l);
    }


    public void dispose() throws Exception {
        // surely ExpressionEditor should be disposable?
    }
}
