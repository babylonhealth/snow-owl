package org.protege.editor.core.ui.util;



import javax.swing.*;

import org.eclipse.core.runtime.IExtension;

import java.awt.*;
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
 * Medical Informatics Group<br>
 * Date: Mar 22, 2006<br><br>
 * <p/>
 * matthew.horridge@cs.man.ac.uk<br>
 * www.cs.man.ac.uk/~horridgm<br><br>
 */
class ComponentExceptionPanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = -975497929104417874L;
    public static final int BORDER_THICKNESS = 4;


    public ComponentExceptionPanel(String message, Throwable exception, IExtension extension) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(BORDER_THICKNESS,
                                                                                     BORDER_THICKNESS,
                                                                                     BORDER_THICKNESS,
                                                                                     BORDER_THICKNESS),
                                                     BorderFactory.createMatteBorder(1, 1, 1, 1, Color.RED)));
        JLabel label = new JLabel("<html><body>" + message + "<br>" + exception.getClass().getSimpleName() + ":<br>" + exception.getMessage() + "<br>" + "</body></html>",
                                  JLabel.CENTER);
        label.setForeground(Color.RED);
        add(label, BorderLayout.NORTH);
    }
}
