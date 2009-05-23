package com.sporkmonger.rmud.rcp;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

/**
 * An action bar advisor is responsible for creating, adding, and disposing of
 * the actions added to a workbench window. Each window will be populated with
 * new actions.
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	private IWorkbenchWindow window;
	

    // generic actions
    private IWorkbenchAction aboutAction;
    private IWorkbenchAction openPreferencesAction;
    private IWorkbenchAction savePerspectiveAction;
    private IWorkbenchAction resetPerspectiveAction;
    private IWorkbenchAction editActionSetAction;
    private IWorkbenchAction closePerspAction;
    private IWorkbenchAction closeAllPerspsAction;

    // generic file actions
//    private IWorkbenchAction newConnectionAction;
//    private IWorkbenchAction newScriptAction;
    private IWorkbenchAction closeAction;
    private IWorkbenchAction closeAllAction;
    private IWorkbenchAction saveAction;
    private IWorkbenchAction saveAsAction;
    private IWorkbenchAction saveAllAction;
    
    // generic retarget actions
    private IWorkbenchAction quitAction;

	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}

	protected void makeActions(final IWorkbenchWindow window) {
		this.window = window;
		
		// Creates the actions and registers them.
		// Registering is needed to ensure that key bindings work.
		// The corresponding commands keybindings are defined in the plugin.xml
		// file.
		// Registering also provides automatic disposal of the actions when
		// the window is closed.

	    // generic actions
        aboutAction = ActionFactory.ABOUT.create(window);
        register(aboutAction);
        openPreferencesAction = ActionFactory.PREFERENCES.create(window);
        register(openPreferencesAction);
        savePerspectiveAction = ActionFactory.SAVE_PERSPECTIVE.create(window);
        register(savePerspectiveAction);
        editActionSetAction = ActionFactory.EDIT_ACTION_SETS.create(window);
        register(editActionSetAction);
        resetPerspectiveAction = ActionFactory.RESET_PERSPECTIVE.create(window);
        register(resetPerspectiveAction);
        closePerspAction = ActionFactory.CLOSE_PERSPECTIVE.create(window);
        register(closePerspAction);
        closeAllPerspsAction = ActionFactory.CLOSE_ALL_PERSPECTIVES.create(window);
        register(closeAllPerspsAction);

	    // generic file actions
        closeAction = ActionFactory.CLOSE.create(window);
        register(closeAction);
        closeAllAction = ActionFactory.CLOSE_ALL.create(window);
        register(closeAllAction);
        saveAction = ActionFactory.SAVE.create(window);
        register(saveAction);
        saveAsAction = ActionFactory.SAVE_AS.create(window);
        register(saveAsAction);
        saveAllAction = ActionFactory.SAVE_ALL.create(window);
        register(saveAllAction);
	    
	    // generic retarget actions
		quitAction = ActionFactory.QUIT.create(window);
		register(quitAction);
	}

	protected void fillMenuBar(IMenuManager menuBar) {
        menuBar.add(createFileMenu());
        menuBar.add(createWindowMenu());
	}
	
    /**
     * Creates and returns the File menu.
     */
    private MenuManager createFileMenu() {
		MenuManager menu = new MenuManager("&File", IWorkbenchActionConstants.M_FILE);
				
        menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));
//        menu.add(newConnectionAction);
//        menu.add(newScriptAction);
        menu.add(new GroupMarker(IWorkbenchActionConstants.NEW_EXT));
        menu.add(new Separator());

        menu.add(closeAction);
        menu.add(closeAllAction);
        menu.add(new GroupMarker(IWorkbenchActionConstants.CLOSE_EXT));
        menu.add(new Separator());
        menu.add(saveAction);
        menu.add(saveAsAction);
        menu.add(saveAllAction);
        menu.add(new GroupMarker(IWorkbenchActionConstants.SAVE_EXT));
        menu.add(new Separator());

        menu.add(ContributionItemFactory.REOPEN_EDITORS.create(getWindow()));
        menu.add(new GroupMarker(IWorkbenchActionConstants.MRU));
        menu.add(new Separator());
        
        // If we're on OS X we shouldn't show this command in the File menu. It
		// should be invisible to the user. However, we should not remove it -
		// the carbon UI code will do a search through our menu structure
		// looking for it when Cmd-Q is invoked (or Quit is chosen from the
		// application menu.
		ActionContributionItem quitItem = new ActionContributionItem(quitAction);
		quitItem.setVisible(!Util.isMac());
		menu.add(quitItem);
		menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));

		return menu;
    }
	
    /**
     * Creates and returns the Window menu.
     */
    private MenuManager createWindowMenu() {
        MenuManager menu = new MenuManager("&Window", IWorkbenchActionConstants.M_WINDOW);
        addPerspectiveActions(menu);
        menu.add(new Separator());
        Separator sep = new Separator(IWorkbenchActionConstants.MB_ADDITIONS);
		sep.setVisible(!Util.isMac());
		menu.add(sep);
        
        // See the comment for quit in createFileMenu
        ActionContributionItem openPreferencesItem = new ActionContributionItem(openPreferencesAction);
        openPreferencesItem.setVisible(!Util.isMac());
        menu.add(openPreferencesItem);

        menu.add(ContributionItemFactory.OPEN_WINDOWS.create(getWindow()));
        return menu;
    }

    /**
     * Adds the perspective actions to the specified menu.
     */
    private void addPerspectiveActions(MenuManager menu) {
        {
            MenuManager changePerspMenuMgr = new MenuManager("Open Perspective", "openPerspective");
            IContributionItem changePerspMenuItem = ContributionItemFactory.PERSPECTIVES_SHORTLIST
                    .create(getWindow());
            changePerspMenuMgr.add(changePerspMenuItem);
            menu.add(changePerspMenuMgr);
        }
        {
            MenuManager showViewMenuMgr = new MenuManager("Show View", "showView");
            IContributionItem showViewMenu = ContributionItemFactory.VIEWS_SHORTLIST.create(window);
            showViewMenuMgr.add(showViewMenu);
            menu.add(showViewMenuMgr);
        }
        menu.add(new Separator());
        menu.add(editActionSetAction);
        menu.add(savePerspectiveAction);
        menu.add(resetPerspectiveAction);
        menu.add(closePerspAction);
        menu.add(closeAllPerspsAction);
    }

    /**
     * Returns the window to which this action builder is contributing.
     */
    private IWorkbenchWindow getWindow() {
        return window;
    }
}
