package org.protege.editor.owl.ui.frame.annotationproperty;

import java.util.Arrays;
import java.util.List;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.ui.editor.OWLAnnotationPropertyEditor;
import org.protege.editor.owl.ui.frame.AbstractOWLFrameSectionRow;
import org.protege.editor.owl.ui.frame.OWLFrameSection;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
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
public class OWLSubAnnotationPropertyFrameSectionRow extends AbstractOWLFrameSectionRow<OWLAnnotationProperty, OWLSubAnnotationPropertyOfAxiom, OWLAnnotationProperty> {

    public OWLSubAnnotationPropertyFrameSectionRow(OWLEditorKit owlEditorKit, 
    											   OWLFrameSection<OWLAnnotationProperty, OWLSubAnnotationPropertyOfAxiom, OWLAnnotationProperty> section,
    											   OWLOntology ontology, OWLAnnotationProperty property,
    											   OWLSubAnnotationPropertyOfAxiom axiom) {
        super(owlEditorKit, section, ontology, property, axiom);
    }


    protected OWLAnnotationPropertyEditor getObjectEditor() {
        final OWLAnnotationPropertyEditor editor = new OWLAnnotationPropertyEditor(getOWLEditorKit());
        editor.setEditedObject(getAxiom().getSuperProperty());
        return editor;
    }


    protected OWLSubAnnotationPropertyOfAxiom createAxiom(OWLAnnotationProperty property) {
        return getOWLDataFactory().getOWLSubAnnotationPropertyOfAxiom(getRootObject(), property);
    }


    public List<OWLAnnotationProperty> getManipulatableObjects() {
        return Arrays.asList(getAxiom().getSuperProperty());
    }
}