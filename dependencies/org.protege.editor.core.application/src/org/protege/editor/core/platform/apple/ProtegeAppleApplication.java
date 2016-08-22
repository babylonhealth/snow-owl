package org.protege.editor.core.platform.apple;

import java.io.File;

import org.protege.editor.core.ProtegeApplication;
import org.protege.editor.core.ProtegeManager;
import org.protege.editor.core.editorkit.EditorKit;
import org.protege.editor.core.ui.about.AboutPanel;
import org.protege.editor.core.ui.preferences.PreferencesDialogPanel;
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
 * Date: Sep 19, 2008<br><br>
 */
public class ProtegeAppleApplication extends AbstractAppleApplicationWrapper {

    private EditorKit eKit;

    private static ProtegeAppleApplication instance;


    public static ProtegeAppleApplication getInstance(){
        if (instance == null){
            instance = new ProtegeAppleApplication();
        }
        return instance;
    }


    private ProtegeAppleApplication() {
    }


    public void setEditorKit(EditorKit eKit){
        this.eKit = eKit;
        setEnabledPreferencesMenu(eKit != null);
    }
    
    @Override
    protected void editFile(String fileName) throws Exception {
        ProtegeManager.getInstance().getApplication().editURI(new File(fileName).toURI());
    }


    protected boolean handlePreferencesRequest() {
        if (eKit != null){
            PreferencesDialogPanel.showPreferencesDialog(null, eKit);
            return true;
        }
        return false;
    }


    protected boolean handleAboutRequest() {
        AboutPanel.showDialog();
        return true;
    }


    protected boolean handleQuitRequest() {
        return ProtegeApplication.handleQuit();
    }
}
