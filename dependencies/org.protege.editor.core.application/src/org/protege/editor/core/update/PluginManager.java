package org.protege.editor.core.update;

import static org.protege.editor.core.update.PluginRegistryImpl.PluginRegistryType.PLUGIN_DOWNLOAD_REGISTRY;
import static org.protege.editor.core.update.PluginRegistryImpl.PluginRegistryType.PLUGIN_UPDATE_REGISTRY;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.protege.editor.core.ProtegeApplication;
import org.protege.editor.core.prefs.Preferences;
import org.protege.editor.core.prefs.PreferencesManager;
import org.protege.editor.core.ui.progress.BackgroundTask;

/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Medical Informatics Group<br>
 * Date: 25-Aug-2006<br><br>
 * <p/>
 * matthew.horridge@cs.man.ac.uk<br>
 * www.cs.man.ac.uk/~horridgm<br><br>
 */
public class PluginManager {
    private static PluginManager instance;

    public static final String AUTO_UPDATE_KEY = "CheckForUpdates";

    public static final String PLUGIN_REGISTRY_KEY = "plugin.registry-4.2-release.url";
    public static final String DEFAULT_REGISTRY = "http://protege.googlecode.com/svn/trunk/autoupdate/4_2/4.2-plugins-2012-03-19.repository";

    private PluginRegistry pluginRegistry;

    private PluginManager() {

    }


    public static synchronized PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }


    private Preferences getPrefs() {
        PreferencesManager man = PreferencesManager.getInstance();
        return man.getApplicationPreferences(PluginManager.class);
    }


    public void setAutoUpdateEnabled(boolean b) {
        getPrefs().putBoolean(AUTO_UPDATE_KEY, b);
    }


    public boolean isAutoUpdateEnabled() {
        return getPrefs().getBoolean(AUTO_UPDATE_KEY, true);
    }


    public URL getPluginRegistryLocation() {
    	String pluginRegistryLoc = getPrefs().getString(PLUGIN_REGISTRY_KEY, DEFAULT_REGISTRY);
        try {
            return new URL(pluginRegistryLoc);
        }
        catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }


    public void setPluginRegistryLocation(URL url) {
    	String oldPluginRegistryLoc = getPrefs().getString(PLUGIN_REGISTRY_KEY, DEFAULT_REGISTRY);
    	String newPluginRegistryLoc = url.toString();
    	if (!newPluginRegistryLoc.equals(oldPluginRegistryLoc)) {
    		getPrefs().putString(PLUGIN_REGISTRY_KEY, newPluginRegistryLoc);
    		pluginRegistry = null;
    	}
    }


    public PluginRegistry getPluginRegistry(){
        if (pluginRegistry == null){
            pluginRegistry = new PluginRegistryImpl(getPluginRegistryLocation(), PLUGIN_DOWNLOAD_REGISTRY);
        }
        return pluginRegistry;
    }


    public void checkForUpdates(){
        final BackgroundTask task = ProtegeApplication.getBackgroundTaskManager().startTask("searching for updates");
        Runnable runnable = new Runnable() {
            public void run() {
            	PluginRegistry updatesProvider;
            	java.util.List<PluginInfo> updates;
            	try {
            		updatesProvider = new PluginRegistryImpl(getPluginRegistryLocation(), PLUGIN_UPDATE_REGISTRY);
            		updates = updatesProvider.getAvailableDownloads();
            	}
            	finally {
            		ProtegeApplication.getBackgroundTaskManager().endTask(task);
            	}
                if (!updates.isEmpty()) {
                    Map<String, PluginRegistry> map = new LinkedHashMap<String, PluginRegistry>();
                    map.put("Updates", updatesProvider);
                    showUpdatesDialog(map);
                }
                else{
                    JOptionPane.showMessageDialog(null, "No updates available at this time.");
                }
            }
        };
        Thread t = new Thread(runnable, "Check for updates");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();

    }


    public void checkForDownloads() {
        final BackgroundTask task = ProtegeApplication.getBackgroundTaskManager().startTask("searching for downloads");

        Runnable runnable = new Runnable() {
            public void run() {
            	List<PluginInfo> downloads;
            	try {
            		PluginRegistry registry = getPluginRegistry();
            		downloads = registry.getAvailableDownloads();
            	}
            	finally {
            		ProtegeApplication.getBackgroundTaskManager().endTask(task);
            	}
                if (!downloads.isEmpty()){
                    Map<String, PluginRegistry> map = new LinkedHashMap<String, PluginRegistry>();
                    map.put("Downloads", getPluginRegistry());
                    showUpdatesDialog(map);
                }
                else{
                    JOptionPane.showMessageDialog(null, "No downloads available at this time.");
                }
            }
        };
        Thread t = new Thread(runnable, "Check for downloads");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();

    }


    public void performAutoUpdate() {
        final BackgroundTask autoUpdateTask = ProtegeApplication.getBackgroundTaskManager().startTask("autoupdate");
        Runnable runnable = new Runnable() {
        	PluginRegistry updatesProvider;
        	List<PluginInfo> updates;
            public void run() {
            	try {
            		updatesProvider = new PluginRegistryImpl(getPluginRegistryLocation(), PLUGIN_UPDATE_REGISTRY);
            		updates = updatesProvider.getAvailableDownloads();
            	}
            	finally {
            		ProtegeApplication.getBackgroundTaskManager().endTask(autoUpdateTask);
            	}
                if (!updates.isEmpty()) {
                    Map<String, PluginRegistry> map = new LinkedHashMap<String, PluginRegistry>();
                    map.put(PLUGIN_UPDATE_REGISTRY.getLabel(), updatesProvider);
                    map.put(PLUGIN_DOWNLOAD_REGISTRY.getLabel(), new PluginRegistryImpl(getPluginRegistryLocation(), PLUGIN_DOWNLOAD_REGISTRY));
                    showUpdatesDialog(map);
                }
            }
        };
        Thread t = new Thread(runnable, "Auto-update");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }


    public void performCheckPlugins() {
        final BackgroundTask autoUpdateTask = ProtegeApplication.getBackgroundTaskManager().startTask("searching for plugins");
        Runnable runnable = new Runnable() {
            public void run() {
                PluginRegistry updatesProvider = new PluginRegistryImpl(getPluginRegistryLocation(), PLUGIN_UPDATE_REGISTRY);
                List<PluginInfo> updates = updatesProvider.getAvailableDownloads();
                if (!updates.isEmpty()) {
                    ProtegeApplication.getBackgroundTaskManager().endTask(autoUpdateTask);
                    Map<String, PluginRegistry> map = new LinkedHashMap<String, PluginRegistry>();
                    map.put(PLUGIN_UPDATE_REGISTRY.getLabel(), updatesProvider);
                    map.put(PLUGIN_DOWNLOAD_REGISTRY.getLabel(), new PluginRegistryImpl(getPluginRegistryLocation(), PLUGIN_DOWNLOAD_REGISTRY));
                    showUpdatesDialog(map);
                }
                else{
                    PluginRegistry registry = getPluginRegistry();
                    final List<PluginInfo> downloads = registry.getAvailableDownloads();
                    ProtegeApplication.getBackgroundTaskManager().endTask(autoUpdateTask);
                    if (!downloads.isEmpty()){
                        Map<String, PluginRegistry> map = new LinkedHashMap<String, PluginRegistry>();
                        map.put("Downloads", registry);
                        map.put("Updates", updatesProvider);
                        showUpdatesDialog(map);
                    }
                    else {
                        JOptionPane.showMessageDialog(null, "No additional plugins / updates available at this time.");                        
                    }
                }
            }
        };
        Thread t = new Thread(runnable, "Check plugins");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }


    public void showUpdatesDialog(Map<String, PluginRegistry> downloadsProviders) {
        List<PluginInfo> selUpdates = PluginPanel.showDialog(downloadsProviders, null);
        if (selUpdates != null){
            PluginInstaller installer = new PluginInstaller(selUpdates);
            installer.run();
            // @@TODO remove the installed plugins from the updatesProvider
        }
    }


    public void checkForUpdatesUI() {

    }
}
