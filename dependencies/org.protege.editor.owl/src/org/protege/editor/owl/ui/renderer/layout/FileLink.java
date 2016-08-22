package org.protege.editor.owl.ui.renderer.layout;

import org.protege.editor.core.FileUtils;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import org.apache.log4j.Logger;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 11/11/2011
 * <p>
 *     Represents a link that points to a local file.  When the link is clicked the file is show.
 * </p>
 * <p>
 * <b>This class is subject to change and it is therefore not recommended for public use</b>.
 * </p>
 */
public class FileLink extends Link {

    private static Logger logger = Logger.getLogger(FileLink.class);

    private File file;

    public FileLink(File file) {
        super(file.toString());
        this.file = file;
    }

    @Override
    public void activate(Component component, MouseEvent event) {
        try {
            FileUtils.showFile(file);
        }
        catch (IOException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    @Override
    public boolean isRollOverLink() {
        return false;
    }
}
