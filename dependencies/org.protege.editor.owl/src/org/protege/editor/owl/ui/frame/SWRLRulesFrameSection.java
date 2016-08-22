package org.protege.editor.owl.ui.frame;

import java.util.Comparator;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.ui.editor.OWLObjectEditor;
import org.protege.editor.owl.ui.editor.SWRLRuleEditor;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.SWRLRule;


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
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 06-Jul-2007<br><br>
 */
public class SWRLRulesFrameSection extends AbstractOWLFrameSection<OWLOntology, SWRLRule, SWRLRule> {


    public SWRLRulesFrameSection(OWLEditorKit editorKit, OWLFrame<? extends OWLOntology> owlFrame) {
        super(editorKit, "Rules", "Rule", owlFrame);
    }


    protected SWRLRule createAxiom(SWRLRule object) {
        return object;
    }


    public OWLObjectEditor<SWRLRule> getObjectEditor() {
        return new SWRLRuleEditor(getOWLEditorKit());
    }


    public boolean canAdd() {
        return true;
    }


    protected void refill(OWLOntology ontology) {
        for (SWRLRule rule : ontology.getAxioms(AxiomType.SWRL_RULE)) {
            addRow(new SWRLRuleFrameSectionRow(getOWLEditorKit(), this, ontology, ontology, rule));
        }
    }


    protected void clear() {
    }


    public Comparator<OWLFrameSectionRow<OWLOntology, SWRLRule, SWRLRule>> getRowComparator() {
        return null;
    }

    @Override
    protected boolean isResettingChange(OWLOntologyChange change) {
    	if (!change.isAxiomChange()) {
    		return false;
    	}
    	return change.getAxiom() instanceof SWRLRule;
    }

}
