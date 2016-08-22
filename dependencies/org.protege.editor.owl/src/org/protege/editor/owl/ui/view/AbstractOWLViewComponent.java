package org.protege.editor.owl.ui.view;

import org.apache.log4j.Logger;
import org.protege.editor.core.ProtegeApplication;
import org.protege.editor.core.prefs.Preferences;
import org.protege.editor.core.prefs.PreferencesManager;
import org.protege.editor.core.ui.view.ViewComponent;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.OWLWorkspace;
import org.protege.editor.owl.ui.find.OWLEntityFindPanel;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;


/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Medical Informatics Group<br>
 * Date: Mar 18, 2006<br><br>
 * <p/>
 * matthew.horridge@cs.man.ac.uk<br>
 * www.cs.man.ac.uk/~horridgm<br><br>
 */
public abstract class AbstractOWLViewComponent extends ViewComponent {

    /**
     * 
     */
    private static final long serialVersionUID = -3089829405015180530L;

    private static final String DIALOGS_ALWAYS_CENTRED = "DIALOGS_ALWAYS_CENTRED";

    private static final Logger logger = Logger.getLogger(AbstractOWLViewComponent.class);

    private static final String INPUT_MAP_KEY = "FIND";


    public OWLModelManager getOWLModelManager() {
        return getOWLWorkspace().getOWLEditorKit().getModelManager();
    }


    public OWLEditorKit getOWLEditorKit() {
        return getOWLWorkspace().getOWLEditorKit();
    }


    protected abstract void initialiseOWLView() throws Exception;


    final public void initialise() throws Exception {
        setupFinder();
        initialiseOWLView();

//        if(this instanceof Copyable) {
        prepareCopyable();
//        }
//        if(this instanceof Pasteable) {
        preparePasteable();
//        }
        prepareCuttable();

        // add refresh components here (perhaps) when the class trees don't do their crazy stuff
    }


    private void prepareCopyable() {
        // The "global" copy action should take precedence over anything, so remove any
        // copy key bindings from children
        // Remove copy from the input map of any children
        removeFromInputMap(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                           this);
    }


    private void preparePasteable() {
        // The "global" paste action should take precedence over anything, so remove any
        // paste action key bindings from children
        removeFromInputMap(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                           this);
    }


    private void prepareCuttable() {
        // The "global" cut action should take precedence over anything, so remove any
        // cut action key bindings from children
        removeFromInputMap(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                           this);
    }


    private static void removeFromInputMap(KeyStroke ks, JComponent c) {
        // Most likely stored in the ancestor of focused component map,
        // but...
        removeKeyBinding(c.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT), ks);
        removeKeyBinding(c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW), ks);
        removeKeyBinding(c.getInputMap(JComponent.WHEN_FOCUSED), ks);
        // Process children recursively
        for (Component child : c.getComponents()) {
            if (child instanceof JComponent) {
                removeFromInputMap(ks, (JComponent) child);
            }
        }
    }


    private static void removeKeyBinding(InputMap im, KeyStroke ks) {
        // Remove the key binding from the second "tier" input map
        // User bindings are stored in the first tier, where as UI
        // bindings (installed by the LAF) are stored in the second tier;
        if (im.getParent() != null) {
            im.getParent().remove(ks);
        }
    }


    private void setupFinder() {
        if (this instanceof Findable) {
            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F,
                                                                                                  Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                                                                           INPUT_MAP_KEY);
            getActionMap().put(INPUT_MAP_KEY, new AbstractAction() {
                /**
                 * 
                 */
                private static final long serialVersionUID = 8090385169368686926L;

                public void actionPerformed(ActionEvent e) {
                    handleFind();
                }
            });
        }
    }


    private void handleFind() {
        logger.debug("Handling find in " + toString());
        Component parent = getDialogParent();
        OWLEntity foundEntity = OWLEntityFindPanel.showDialog(parent, getOWLEditorKit(), (Findable) this);
        if (foundEntity == null) {
            return;
        }
        ((Findable) this).show(foundEntity);
    }


    private Component getDialogParent() {
        // @@TODO move prefs somewhere more central
        Preferences prefs = PreferencesManager.getInstance().getApplicationPreferences(ProtegeApplication.ID);
        return prefs.getBoolean(DIALOGS_ALWAYS_CENTRED, false) ? SwingUtilities.getAncestorOfClass(Frame.class, getParent()) : getParent();
    }



    protected OWLDataFactory getOWLDataFactory() {
        return getOWLModelManager().getOWLDataFactory();
    }


    final public void dispose() {
        disposeOWLView();
    }


    protected abstract void disposeOWLView();


    public OWLWorkspace getOWLWorkspace() {
        return (OWLWorkspace) getWorkspace();
    }


    protected OWLObject getObjectToCopy() {
        return null;
    }


    protected void handlePaste(List<OWLObject> objects) {
    }
}
