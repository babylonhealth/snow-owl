package org.protege.editor.owl.ui.renderer.context;

import org.semanticweb.owlapi.model.OWLDataProperty;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 01/10/2012
 */
public class NullDataPropertySatisfiabilityChecker implements DataPropertySatisfiabilityChecker {

    public static final boolean DEFAULT_SATISFIABLE_VALUE = true;

    public boolean isSatisfiable(OWLDataProperty property) {
        return DEFAULT_SATISFIABLE_VALUE;
    }
}
