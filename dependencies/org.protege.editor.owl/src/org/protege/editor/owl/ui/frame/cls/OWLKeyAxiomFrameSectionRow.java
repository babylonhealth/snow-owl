package org.protege.editor.owl.ui.frame.cls;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.ui.editor.OWLPropertySetEditor;
import org.protege.editor.owl.ui.frame.AbstractOWLFrameSectionRow;
import org.protege.editor.owl.ui.frame.OWLFrameSection;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
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
 * Date: Jun 4, 2009<br><br>
 */
public class OWLKeyAxiomFrameSectionRow extends AbstractOWLFrameSectionRow<OWLClass, OWLHasKeyAxiom, Set<OWLPropertyExpression>> {

    public OWLKeyAxiomFrameSectionRow(OWLEditorKit owlEditorKit, 
    								  OWLFrameSection<OWLClass, OWLHasKeyAxiom, Set<OWLPropertyExpression>> section,
                                      OWLOntology ontology, OWLClass rootObject,
                                      OWLHasKeyAxiom axiom) {
        super(owlEditorKit, section, ontology, rootObject, axiom);
    }


    protected OWLHasKeyAxiom createAxiom(Set<OWLPropertyExpression> properties) {
    	/*
    	 * Degenericized to be compatible with changing OWLAPI interfaces
    	 */
    	return getOWLDataFactory().getOWLHasKeyAxiom(getRootObject(), (Set) properties);
    }


    protected OWLPropertySetEditor getObjectEditor() {
        final OWLPropertySetEditor editor = new OWLPropertySetEditor(getOWLEditorKit());
    	/*
    	 * Degenericized to be compatible with changing OWLAPI interfaces
    	 */
        editor.setEditedObject((Set) getAxiom().getPropertyExpressions());
        return editor;
    }


    public List<OWLPropertyExpression> getManipulatableObjects() {
        return new ArrayList<OWLPropertyExpression>(getAxiom().getPropertyExpressions());
    }}
