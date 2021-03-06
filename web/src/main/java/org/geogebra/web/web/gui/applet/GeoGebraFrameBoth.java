package org.geogebra.web.web.gui.applet;

import java.util.ArrayList;

import org.geogebra.common.euclidian.EuclidianConstants;
import org.geogebra.common.javax.swing.SwingConstants;
import org.geogebra.common.kernel.ModeSetter;
import org.geogebra.common.main.App;
import org.geogebra.common.main.App.InputPosition;
import org.geogebra.common.main.Feature;
import org.geogebra.common.util.debug.Log;
import org.geogebra.keyboard.web.TabbedKeyboard;
import org.geogebra.web.html5.awt.GDimensionW;
import org.geogebra.web.html5.gui.FastClickHandler;
import org.geogebra.web.html5.gui.GeoGebraFrameW;
import org.geogebra.web.html5.gui.GuiManagerInterfaceW;
import org.geogebra.web.html5.gui.laf.GLookAndFeelI;
import org.geogebra.web.html5.gui.tooltip.ToolTipManagerW;
import org.geogebra.web.html5.gui.util.CancelEventTimer;
import org.geogebra.web.html5.gui.util.MathKeyboardListener;
import org.geogebra.web.html5.gui.util.StandardButton;
import org.geogebra.web.html5.main.AppW;
import org.geogebra.web.html5.util.ArticleElement;
import org.geogebra.web.html5.util.Dom;
import org.geogebra.web.html5.util.debug.LoggerW;
import org.geogebra.web.html5.util.keyboard.VirtualKeyboardW;
import org.geogebra.web.web.css.MaterialDesignResources;
import org.geogebra.web.web.gui.GuiManagerW;
import org.geogebra.web.web.gui.HeaderPanelDeck;
import org.geogebra.web.web.gui.MyHeaderPanel;
import org.geogebra.web.web.gui.app.GGWMenuBar;
import org.geogebra.web.web.gui.app.GGWToolBar;
import org.geogebra.web.web.gui.app.ShowKeyboardButton;
import org.geogebra.web.web.gui.laf.GLookAndFeel;
import org.geogebra.web.web.gui.layout.DockGlassPaneW;
import org.geogebra.web.web.gui.layout.DockManagerW;
import org.geogebra.web.web.gui.layout.DockPanelW;
import org.geogebra.web.web.gui.layout.panels.AlgebraPanelInterface;
import org.geogebra.web.web.gui.layout.panels.EuclidianDockPanelW;
import org.geogebra.web.web.gui.pagecontrolpanel.PageListPanel;
import org.geogebra.web.web.gui.toolbar.mow.MOWToolbar;
import org.geogebra.web.web.gui.toolbarpanel.ToolbarPanel;
import org.geogebra.web.web.gui.util.VirtualKeyboardGUI;
import org.geogebra.web.web.gui.view.algebra.AlgebraViewW;
import org.geogebra.web.web.main.GDevice;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Frame for applets with GUI
 *
 */
public class GeoGebraFrameBoth extends GeoGebraFrameW implements
		HeaderPanelDeck {

	private AppletFactory factory;
	private DockGlassPaneW glass;
	private GGWToolBar ggwToolBar = null;
	private GGWMenuBar ggwMenuBar;
	private KeyboardState keyboardState;
	private final SimplePanel kbButtonSpace = new SimplePanel();
	private GDevice device;
	private boolean[] childVisible = new boolean[0];
	private boolean keyboardShowing = false;
	private ShowKeyboardButton showKeyboardButton;
	private int keyboardHeight;
	private DockPanelW dockPanelKB;
	private HeaderPanel lastBG;
	private MOWToolbar mowToolbar;
	private StandardButton openMenuButton;
	private PageListPanel pageListPanel;

	/**
	 * @param factory
	 *            factory for applets (2D or 3D)
	 * @param laf
	 *            look and feel
	 * @param device
	 *            browser/tablet; if left null, defaults to browser
	 * @param mainTag
	 *            TODO remove, if GGB-2051 released.
	 */
	public GeoGebraFrameBoth(AppletFactory factory, GLookAndFeel laf,
			GDevice device, boolean mainTag) {
		super(laf, mainTag);
		this.device = device;
		this.factory = factory;
		kbButtonSpace.addStyleName("kbButtonSpace");
		this.add(kbButtonSpace);

	}

	@Override
	protected AppW createApplication(ArticleElement article,
			GLookAndFeelI laf) {
		AppW application = factory.getApplet(article, this, laf, this.device);
		getArticleMap().put(article.getId(), application);
		
		if (app!= null && app.has(Feature.SHOW_ONE_KEYBOARD_BUTTON_IN_FRAME)){
			kbButtonSpace.addStyleName("kbButtonSpace");
			this.add(kbButtonSpace);
		}
		
		if (app != null && app.isUnbundled()) {
			addStyleName("newToolbar");
		}

		this.glass = new DockGlassPaneW(new GDimensionW(
				article.getDataParamWidth(), article.getDataParamHeight()));
		this.add(glass);

		return application;
	}


	/**
	 * Main entry points called by geogebra.web.Web.startGeoGebra()
	 * 
	 * @param geoGebraMobileTags
	 *            list of &lt;article&gt; elements of the web page
	 * @param factory
	 *            applet factory
	 * @param laf
	 *            look and feel
	 * @param device
	 *            browser/tablet; if left null, defaults to browser
	 */
	public static void main(ArrayList<ArticleElement> geoGebraMobileTags,
			AppletFactory factory, GLookAndFeel laf, GDevice device) {

		for (final ArticleElement articleElement : geoGebraMobileTags) {
			final GeoGebraFrameW inst = new GeoGebraFrameBoth(factory, laf,
					device,
					ArticleElement.getDataParamFitToScreen(articleElement));
			inst.ae = articleElement;
			LoggerW.startLogger(inst.ae);
			inst.createSplash(articleElement);
			RootPanel.get(articleElement.getId()).add(inst);
		}
		if (geoGebraMobileTags.isEmpty()) {
			return;
		}

		if (geoGebraMobileTags.size() > 0) {
		// // now we can create dummy elements before & after each applet
		// // with tabindex 10000, for ticket #5158
		// tackleFirstDummy(geoGebraMobileTags.get(0));
		//
		//
			tackleLastDummy(geoGebraMobileTags
					.get(geoGebraMobileTags.size() - 1));
		// // programFocusEvent(firstDummy, lastDummy);
		}
	}

	/**
	 * @param el
	 *            html element to render into
	 * @param factory
	 *            applet factory
	 * @param laf
	 *            look and feel
	 * @param clb
	 *            call this after rendering
	 */
	public static void renderArticleElement(Element el, AppletFactory factory,
			GLookAndFeel laf, JavaScriptObject clb) {

		GeoGebraFrameW.renderArticleElementWithFrame(el, new GeoGebraFrameBoth(
				factory, laf, null, ArticleElement.getDataParamFitToScreen(el)),
				clb);

		GeoGebraFrameW.reCheckForDummies(el);
	}

	/**
	 * @return glass pane for view moving
	 */
	public DockGlassPaneW getGlassPane() {
		return this.glass;
	}

	/**
	 * Attach glass pane to frame
	 */
	public void attachGlass() {
		if (this.glass != null) {
			this.add(glass);
		}
	}

	@Override
	public void showBrowser(HeaderPanel bg) {
		keyBoardNeeded(false, null);
		GeoGebraFrameW frameLayout = this;
		ToolTipManagerW.hideAllToolTips();
		final int count = frameLayout.getWidgetCount();
		final int oldHeight = this.getOffsetHeight();
		final int oldWidth = this.getOffsetWidth();
		childVisible = new boolean[count];
		for (int i = 0; i < count; i++) {
			childVisible[i] = frameLayout.getWidget(i).isVisible();
			frameLayout.getWidget(i).setVisible(false);
		}
		frameLayout.add(bg);
		bg.setHeight(oldHeight + "px");
		bg.setWidth(oldWidth + "px");
		bg.onResize();
		bg.setVisible(true);

		if (bg instanceof MyHeaderPanel) {
			((MyHeaderPanel) bg).setFrame(this);
		}
		this.lastBG = bg;
		// frameLayout.forceLayout();

	}

	@Override
	public void hideBrowser(MyHeaderPanel bg) {
		remove(bg);
		lastBG = null;
		ToolTipManagerW.hideAllToolTips();
		final int count = getWidgetCount();
		for (int i = 0; i < count; i++) {
			if (childVisible.length > i) {
				getWidget(i).setVisible(childVisible[i]);
			}
		}
		// frameLayout.setLayout(app);
		// frameLayout.forceLayout();
		if (ae.getDataParamFitToScreen()) {
			setSize(Window.getClientWidth(), ae.computeHeight());
		} else {
			app.updateViewSizes();
		}

	}

	@Override
	public void setSize(int width, int height) {
		// setPixelSize(width, height);
		if (lastBG != null) {
			((MyHeaderPanel) lastBG).setPixelSize(width, height);
			((MyHeaderPanel) lastBG).resizeTo(width, height);
		} else {
			super.setSize(width, height);
			app.adjustViews(true, app.isUnbundled());
		}
	}

	@Override
	public void doShowKeyBoard(final boolean show,
			MathKeyboardListener textField) {
		if (keyboardState == KeyboardState.ANIMATING_IN
				|| keyboardState == KeyboardState.ANIMATING_OUT) {
			return;
		}

		if (this.isKeyboardShowing() == show) {
			app.getGuiManager().setOnScreenKeyboardTextField(textField);
			return;
		}

		GuiManagerInterfaceW gm = app.getGuiManager();
		if (gm != null && !show) {
			gm.onScreenEditingEnded();
		}

		// this.mainPanel.clear();
		app.getEuclidianView1().setKeepCenter(false);
		if (show) {
			showZoomPanel(false);
			keyboardState = KeyboardState.ANIMATING_IN;
			app.hideMenu();
			app.persistWidthAndHeight();
			ToolTipManagerW.hideAllToolTips();
			addKeyboard(textField, true);
		} else {
			showZoomPanel(true);
			keyboardState = KeyboardState.ANIMATING_OUT;
			app.persistWidthAndHeight();
			showKeyboardButton(textField);
			removeKeyboard(textField);
			keyboardState = KeyboardState.HIDDEN;
		}

		// this.mainPanel.add(this.dockPanel);

		Timer timer = new Timer() {
			@Override
			public void run() {

				scrollToInputField();

			}
		};
		timer.schedule(0);
	}

	private void removeKeyboard(MathKeyboardListener textField) {
		final VirtualKeyboardGUI keyBoard = getOnScreenKeyboard(textField);
		this.setKeyboardShowing(false);

		ToolbarPanel toolbarPanel = ((GuiManagerW) app.getGuiManager())
				.getToolbarPanelV2();
		if (toolbarPanel != null) {
			toolbarPanel.updateMoveButton();
		}
		app.updateSplitPanelHeight();

		keyboardHeight = 0;
		keyBoard.remove(new Runnable() {
			@Override
			public void run() {

				keyBoard.resetKeyboardState();
				app.centerAndResizePopups();

			}
		});
	}

	/**
	 * Show keyboard and connect it to textField
	 * 
	 * @param textField
	 *            keyboard listener
	 * @param animated
	 *            whether to animate the keyboard in
	 */
	void addKeyboard(final MathKeyboardListener textField, boolean animated) {
		final VirtualKeyboardW keyBoard = getOnScreenKeyboard(textField);
		this.setKeyboardShowing(true);

		ToolbarPanel toolbarPanel = ((GuiManagerW) app.getGuiManager())
				.getToolbarPanelV2();
		if (toolbarPanel != null) {
			toolbarPanel.hideMoveFloatingButton();
		}


		keyBoard.prepareShow(animated);
		if (app.has(Feature.KEYBOARD_BEHAVIOUR)) {
			app.addAsAutoHidePartnerForPopups(keyBoard.asWidget().getElement());
		}
		CancelEventTimer.keyboardSetVisible();
		// this.mainPanel.addSouth(keyBoard, keyBoard.getOffsetHeight());
		this.add(keyBoard);
		Runnable callback = new Runnable() {

			@Override
			public void run() {
				// this is async, maybe we canceled the keyboard
				if (!isKeyboardShowing()) {
					remove(keyBoard);
					return;
				}
				final boolean showPerspectivesPopup = app
						.isPerspectivesPopupVisible();
				onKeyboardAdded(keyBoard);
				if (showPerspectivesPopup) {
					app.showPerspectivesPopup();
				}
				if (app.has(Feature.KEYBOARD_BEHAVIOUR)) {
					if (textField != null) {
						textField.setFocus(true, true);
					}
				}
			}
		};
		if (animated) {
			keyBoard.afterShown(callback);
		} else {
			callback.run();
		}
	}

	// @Override
	// public void showInputField() {
	// Timer timer = new Timer() {
	// @Override
	// public void run() {
	// scrollToInputField();
	// }
	// };
	// timer.schedule(0);
	// }
	/**
	 * Callback for keyboard; takes care of resizing
	 * 
	 * @param keyBoard
	 *            keyboard
	 */
	protected void onKeyboardAdded(final VirtualKeyboardW keyBoard) {
		keyboardHeight = keyBoard.getOffsetHeight();
		if (keyboardHeight == 0) {
			keyboardHeight = estimateKeyboardHeight();
		}

		app.updateSplitPanelHeight();

		// TODO maybe too expensive?
		app.updateCenterPanelAndViews();
		add(keyBoard);
		keyBoard.setVisible(true);
		if (showKeyboardButton != null) {
			showKeyboardButton.hide();
		}
		app.centerAndResizePopups();
		keyboardState = KeyboardState.SHOWN;

	}

	/**
	 * Scroll to the input-field, if the input-field is in the algebraView.
	 */
	void scrollToInputField() {
		if (app.showAlgebraInput()
				&& app.getInputPosition() == InputPosition.algebraView) {
			AlgebraPanelInterface dp = (AlgebraPanelInterface) (app
					.getGuiManager()
					.getLayout().getDockManager().getPanel(App.VIEW_ALGEBRA));

			dp.scrollToActiveItem();
		}
	}

	private void showZoomPanel(boolean show) {
		if (app.isPortrait()) {
			return;
		}

		EuclidianDockPanelW dp = (EuclidianDockPanelW) (app.getGuiManager()
				.getLayout().getDockManager().getPanel(App.VIEW_EUCLIDIAN));
		if (show) {
			dp.showZoomPanel();
		} else {
			dp.hideZoomPanel();
		}

	}

	@Override
	public boolean showKeyBoard(boolean show, MathKeyboardListener textField,
			boolean forceShow) {
		if (forceShow) {
			doShowKeyBoard(show, textField);
			return true;
		}
		return keyBoardNeeded(show, textField);
	}

	@Override
	public boolean keyBoardNeeded(boolean show,
			MathKeyboardListener textField) {
		if (this.keyboardState == KeyboardState.ANIMATING_IN) {
			return true;
		}
		if (this.keyboardState == KeyboardState.ANIMATING_OUT) {
			return false;
		}
		
		if (app.isUnbundled() && !app.isWhiteboardActive()
				&& ((GuiManagerW) app.getGuiManager())
						.getToolbarPanelV2() != null
				&& !((GuiManagerW) app.getGuiManager()).getToolbarPanelV2()
						.isOpen()) {
			return false;
		}
		if (app.getLAF().isTablet()
				|| isKeyboardShowing()
									// showing, we don't have
									// to handle the showKeyboardButton
				|| (app.getGuiManager() != null
						&& app.getGuiManager().getKeyboardShouldBeShownFlag())) {
			doShowKeyBoard(show, textField);
			return true;
		}
		showKeyboardButton(textField);
		return false;

	}

	private void showKeyboardButton(MathKeyboardListener textField) {
		if (!appNeedsKeyboard()) {
			return;
		}
		if (showKeyboardButton == null) {
			DockManagerW dm = (DockManagerW) app.getGuiManager().getLayout()
					.getDockManager();
			dockPanelKB = dm.getPanelForKeyboard();

			if (dockPanelKB != null) {
				showKeyboardButton = new ShowKeyboardButton(this, dm,
						dockPanelKB, app);
				dockPanelKB.setKeyBoardButton(showKeyboardButton);
			}

		}

		if (showKeyboardButton != null) {
			if (app.has(Feature.SHOW_ONE_KEYBOARD_BUTTON_IN_FRAME)) {
				this.setKeyboardButton();
			}
			// showKeyboardButton.show(app.isKeyboardNeeded(), textField);
			showKeyboardButton.show(app.isKeyboardNeeded(), textField);
			if (app.has(Feature.SHOW_ONE_KEYBOARD_BUTTON_IN_FRAME)) {
				showKeyboardButton.addStyleName("openKeyboardButton2");
			}
		}
	}

	private void setKeyboardButton() {
		//this.keyboardButton = button;
		//kbButtonSpace.add(button);
		this.add(showKeyboardButton);
	}
	
	private boolean appNeedsKeyboard() {
		if (app.showAlgebraInput()
				&& app.getInputPosition() == InputPosition.algebraView
				&& app.showView(App.VIEW_ALGEBRA)) {
			return true;
		}
		if (app.has(Feature.KEYBOARD_BEHAVIOUR)) {
			return (app.showAlgebraInput()
					&& app.getInputPosition() != InputPosition.algebraView)
					|| app.showView(App.VIEW_CAS)
					|| app.showView(App.VIEW_SPREADSHEET)
					|| app.showView(App.VIEW_PROBABILITY_CALCULATOR);
		}
		return app.showView(App.VIEW_CAS);
	}



	/**
	 * @param show
	 *            whether to show keyboard button
	 */
	public void showKeyboardButton(boolean show) {
		if (showKeyboardButton == null) {
			return;
		}
		if (show) {
			showKeyboardButton.setVisible(true);
		} else {
			showKeyboardButton.hide();
		}
	}

	@Override
	public void refreshKeyboard() {
		if (isKeyboardShowing()) {
			final VirtualKeyboardW keyBoard = getOnScreenKeyboard(null);
			if (app.isKeyboardNeeded()) {
				ensureKeyboardDeferred();
				add(keyBoard);
			} else {
				removeKeyboard(null);
				if (this.showKeyboardButton != null) {
					this.showKeyboardButton.hide();
				}
			}
		} else {
			if (app != null && app.isKeyboardNeeded() && appNeedsKeyboard()) {
				if (!app.isStartedWithFile()
						&& !app.getArticleElement().preventFocus()) {
					if (app.getGuiManager().isKeyboardClosedByUser()) {
						ensureKeyboardEditing();
						return;
					}
					setKeyboardShowing(true);
					app.invokeLater(new Runnable() {

						@Override
						public void run() {
							if (app.isWhiteboardActive()) {
								return;
							}
							app.persistWidthAndHeight();
							addKeyboard(null, false);
							app.getGuiManager().focusScheduled(false, false,
									false);
							ensureKeyboardDeferred();

						}
					});
				} else {
					this.showKeyboardButton(null);
					getOnScreenKeyboard(null).showOnFocus();
					app.adjustScreen(true);
				}

			} else if (app != null && app.isKeyboardNeeded()) {
				this.showKeyboardButton(true);
			}

			else if (app != null && !app.has(Feature.SHOW_ONE_KEYBOARD_BUTTON_IN_FRAME)) {

				if (!app.isKeyboardNeeded()
						&& this.showKeyboardButton != null) {
					this.showKeyboardButton.hide();
				}
			}
		}
	}

	private VirtualKeyboardGUI getOnScreenKeyboard(
			MathKeyboardListener textField) {
		if (app.getGuiManager() != null) {
			return ((GuiManagerW) app.getGuiManager())
				.getOnScreenKeyboard(textField, this);
		}
		return null;
	}

	/**
	 * Schedule keyboard editing in 500ms
	 */
	protected void ensureKeyboardDeferred() {
		new Timer() {

			@Override
			public void run() {

				if (app.getGuiManager().hasAlgebraView()) {
					AlgebraViewW av = (AlgebraViewW) app.getAlgebraView();
					// av.clearActiveItem();
					av.setDefaultUserWidth();

				}

				ensureKeyboardEditing();

			}

		}.schedule(500);

	}

	/**
	 * Make sure keyboard is editing
	 */
	protected void ensureKeyboardEditing() {
		DockManagerW dm = (DockManagerW) app.getGuiManager().getLayout()
				.getDockManager();
		MathKeyboardListener ml = app.getGuiManager()
				.getKeyboardListener(dm.getPanelForKeyboard());
		dm.setFocusedPanel(dm.getPanelForKeyboard());

		((GuiManagerW) app.getGuiManager())
					.setOnScreenKeyboardTextField(ml);

		if (ml != null) {
			ml.setFocus(true, true);
			ml.ensureEditing();
		}

	}
	@Override
	public boolean isKeyboardShowing() {
		return this.keyboardShowing;
	}

	@Override
	public void showKeyboardOnFocus() {
		if (app != null) {
			getOnScreenKeyboard(null).showOnFocus();
		}
	}

	@Override
	public void updateKeyboardHeight() {
		if(isKeyboardShowing()){
			int newHeight = getOnScreenKeyboard(null)
					.getOffsetHeight();
			if (newHeight == 0) {
				newHeight = estimateKeyboardHeight();
			}
			if (newHeight > 0) {

				app.updateSplitPanelHeight();
				keyboardHeight = newHeight;
				app.updateCenterPanelAndViews();
				add(getOnScreenKeyboard(null));
			}
		}
	}

	private int estimateKeyboardHeight() {
		int newHeight = app.needsSmallKeyboard() ? TabbedKeyboard.SMALL_HEIGHT
				: TabbedKeyboard.BIG_HEIGHT;
		// add switcher height
		newHeight += 40;

		return newHeight;
	}

	@Override
	public double getKeyboardHeight() {
		return isKeyboardShowing() ? keyboardHeight : 0;
	}

	/**
	 * Adds menu; if toolbar is missing also add it
	 * 
	 * @param app1
	 *            application
	 */
	public void attachMenubar(AppW app1) {
		if (app1.isUnbundled()) {
			return;
		}
		if (ggwToolBar == null) {
			ggwToolBar = new GGWToolBar();
			ggwToolBar.init(app1);
			insert(ggwToolBar, 0);
		}
		ggwToolBar.attachMenubar();
	}

	/**
	 * Adds toolbar
	 * 
	 * @param app1
	 *            application
	 */
	public void attachToolbar(AppW app1) {
		if (app1.has(Feature.MOW_TOOLBAR)) {
			attachMOWToolbar(app1);
			attachOpenMenuButton();
			if (app1.has(Feature.MOW_MULTI_PAGE)) {
				initPageControlPanel(app1);
			}
			return;
		}

		if (app1.isUnbundled()) {
			// do not attach old toolbar
			return;
		}

		// reusing old toolbar is probably a good decision
		if (ggwToolBar == null) {
			ggwToolBar = new GGWToolBar();
			ggwToolBar.init(app1);
		} else {
			ggwToolBar.updateClassname(app1.getToolbarPosition());
		}

		if (app1.getToolbarPosition() == SwingConstants.SOUTH) {
			add(ggwToolBar);
		} else {
			insert(ggwToolBar, 0);
		}


	}

	private void attachMOWToolbar(AppW app1) {
		if (mowToolbar == null) {
			mowToolbar = new MOWToolbar(app1);
		}
		if (app1.getToolbarPosition() == SwingConstants.SOUTH) {
			add(mowToolbar);
		} else {
			insert(mowToolbar, 0);
		}

		add(mowToolbar.getUndoRedoButtons());
		if (app1.has(Feature.MOW_MULTI_PAGE)) {
			add(mowToolbar.getPageControlButton());
		}
		int currentMode = mowToolbar.getCurrentMode();
		if (currentMode != -1) {
			app1.setMode(currentMode, ModeSetter.TOOLBAR);
		} else {
			// set pen as start tool
			app1.setMode(EuclidianConstants.MODE_PEN, ModeSetter.TOOLBAR);
		}

	}

	/**
	 * @return MOW toolbar
	 */
	public MOWToolbar getMOWToorbar() {
		return mowToolbar;
	}

	@Override
	public GGWToolBar getToolbar() {
		return ggwToolBar;
	}

	@Override
	public void setMenuHeight(boolean linearInputbar) {
		// TODO in app mode we need to change menu height when inputbar is
		// visible
	}

	/**
	 * @param app1
	 *            application
	 * @return menubar
	 */
	public GGWMenuBar getMenuBar(AppW app1) {
		if (ggwMenuBar == null) {
			ggwMenuBar = new GGWMenuBar();
			((GuiManagerW) app1.getGuiManager()).getObjectPool()
					.setGgwMenubar(
					ggwMenuBar);
		}
		return ggwMenuBar;
	}

	/**
	 * Close all popups and if event was not from menu, also close menu
	 * 
	 * @param event
	 *            browser event
	 */
	public void closePopupsAndMaybeMenu(NativeEvent event) {
		Log.debug("closePopups");
		// app.closePopups(); TODO
		if (app.isMenuShowing()
				&& !Dom.eventTargetsElement(event, ggwMenuBar.getElement())
				&& !Dom.eventTargetsElement(event, getToolbarMenuElement())
				&& !getGlassPane().isDragInProgress()
				&& !app.isUnbundled() && lastBG == null) {
			app.toggleMenu();
		}
	}

	private Element getToolbarMenuElement() {
		return getToolbar() == null ? null
				: getToolbar().getOpenMenuButtonElement();
	}

	@Override
	public void onBrowserEvent(Event event) {
		if (app == null || !app.getUseFullGui()) {
			return;
		}
		final int eventType = DOM.eventGetType(event);
		if (eventType == Event.ONMOUSEDOWN || eventType == Event.ONTOUCHSTART) {
			closePopupsAndMaybeMenu(event);
		}
	}

	/**
	 * Attach keyboard button
	 */
	public void attachKeyboardButton() {
		if(showKeyboardButton != null){
			add(this.showKeyboardButton);
		}
		
	}

	@Override
	public boolean isHeaderPanelOpen() {
		return lastBG != null;
	}

	private void attachOpenMenuButton() {
		openMenuButton = new StandardButton(
				MaterialDesignResources.INSTANCE.toolbar_menu_black(),
				null, 24,
				app);

		/*
		 * openMenuButton.getUpHoveringFace()
		 * .setImage(MOWToolbar.getImage(pr.menu_header_open_menu_hover(), 32));
		 */
		openMenuButton.addFastClickHandler(new FastClickHandler() {
			@Override
			public void onClick(Widget source) {
				onMenuButtonPressed();
			}
		});

		openMenuButton.addDomHandler(new KeyUpHandler() {
			@Override
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					app.toggleMenu();
				}
				// if (event.getNativeKeyCode() == KeyCodes.KEY_LEFT) {
				// GGWToolBar.this.selectMenuButton(0);
				// }
				// if (event.getNativeKeyCode() == KeyCodes.KEY_RIGHT) {
				// GGWToolBar.this.toolBar.selectMenu(0);
				// }
			}
		}, KeyUpEvent.getType());

		openMenuButton.addStyleName("mowOpenMenuButton");
		add(openMenuButton);
	}

	/**
	 * Actions performed when menu button is pressed
	 */
	protected void onMenuButtonPressed() {
		app.hideKeyboard();
		app.closePopups();
		app.toggleMenu();
		if (app.has(Feature.MOW_MULTI_PAGE)) {
			pageListPanel.close();
		}
	}

	/**
	 * Update undo/redo in MOW toolbar
	 */
	public void updateMOWToorbar() {
		if (mowToolbar == null) {
			return;
		}
		mowToolbar.update();
	}

	/**
	 * @param mode
	 *            new mode for MOW toolbar
	 */
	public void setMOWToorbarMode(int mode) {
		if (mowToolbar == null) {
			return;
		}
		mowToolbar.setMode(mode);
	}

	private void setKeyboardShowing(boolean keyboardShowing) {
		this.keyboardShowing = keyboardShowing;
	}

	private void initPageControlPanel(AppW app1) {
		if (!app1.has(Feature.MOW_MULTI_PAGE)) {
			return;
		}
		if (pageListPanel == null) {
			pageListPanel = new PageListPanel(app1);
		}
	}

	/**
	 * 
	 * @return pageControlPanel
	 */
	public PageListPanel getPageControlPanel() {
		return pageListPanel;
	}
}