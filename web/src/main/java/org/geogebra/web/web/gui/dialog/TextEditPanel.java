package org.geogebra.web.web.gui.dialog;

import java.util.ArrayList;

import org.geogebra.common.awt.GFont;
import org.geogebra.common.gui.dialog.options.model.TextOptionsModel;
import org.geogebra.common.gui.inputfield.DynamicTextElement;
import org.geogebra.common.gui.inputfield.DynamicTextProcessor;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoText;
import org.geogebra.common.main.App;
import org.geogebra.common.main.Feature;
import org.geogebra.common.main.GeoElementSelectionListener;
import org.geogebra.common.main.Localization;
import org.geogebra.common.util.debug.Log;
import org.geogebra.web.html5.gui.inputfield.GeoTextEditor;
import org.geogebra.web.html5.gui.inputfield.ITextEditPanel;
import org.geogebra.web.html5.main.AppW;
import org.geogebra.web.web.css.GuiResources;
import org.geogebra.web.web.css.MaterialDesignResources;
import org.geogebra.web.web.gui.util.MyToggleButtonW;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.resources.client.impl.ImageResourcePrototype;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.himamis.retex.editor.share.util.Unicode;

/**
 * 
 * Panel to manage editing of GeoText strings.
 * 
 * @author G. Sturr
 * 
 */
public class TextEditPanel extends VerticalPanel implements ClickHandler,
        FocusHandler, ITextEditPanel {

	protected AppW app;
	protected DynamicTextProcessor dTProcessor;
	protected GeoTextEditor editor;
	protected FlowPanel toolBar;
	protected TextPreviewPanelW previewer;

	protected ToggleButton btnInsert;


	/** GeoText edited by this panel */
	protected GeoText editGeo = null;

	private MyToggleButtonW btnBold, btnItalic;
	private MyToggleButtonW btnSerif, btnLatex;
	private GeoElementSelectionListener sl;
	private DisclosurePanel disclosurePanel;
	private Localization loc;
	private TextEditAdvancedPanel advancedPanel;
	private boolean mayDetectLaTeX = true;

	/*****************************************************
	 * @param app
	 */
	public TextEditPanel(App app) {
		super();
		this.app = (AppW) app;
		loc = app.getLocalization();

		dTProcessor = new DynamicTextProcessor(app);

		editor = new GeoTextEditor(app, this);
		editor.addStyleName("textEditor");
		
		createToolBar();
		
		// TODO: advancedPanel is slow loading. Make this happen on demand.
		advancedPanel = new TextEditAdvancedPanel(app, this);
		previewer = advancedPanel.getPreviewer();
		
		disclosurePanel = new DisclosurePanel(GuiResources.INSTANCE.triangle_down(), GuiResources.INSTANCE.triangle_right(), loc.getMenu("Advanced"));
		disclosurePanel.setContent(advancedPanel);
		disclosurePanel. getContent().removeStyleName("content");
		disclosurePanel.getContent().addStyleName(
		        "textEditorDisclosurePanelContent");
		disclosurePanel.getHeader().addStyleName(
		        "textEditorDisclosurePanelHeader");

		// build our panel
		setSize("100%", "100%");
		add(toolBar);
		add(editor);
		add(disclosurePanel);

		registerListeners();
		setLabels();

		// force a dummy geo to be created on first use
		setEditGeo(null);

	}

	// ======================================================
	// Event Handlers
	// ======================================================

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (!visible) {
			app.setSelectionListenerMode(null);
			previewer.removePreviewGeoText();
		} else {
			editor.updateFonts();
			previewer.updateFonts();
			advancedPanel.updateGeoList(); 
			updatePreviewPanel();
			
		}
	}

	@Override
	public void onFocus(FocusEvent event) {
		app.setSelectionListenerMode(sl);
	}

	/**
	 * Updates PreviewPanel with current editor content.
	 */
	@Override
	public void updatePreviewPanel() {

		if (previewer == null) {
			return;
		}
		boolean wasLaTeX = isLatex();
		boolean isLaTeX = previewer.updatePreviewText(editGeo, dTProcessor
				.buildGeoGebraString(
				editor.getDynamicTextList(), isLatex()), isLatex(),
				mayDetectLaTeX);
		if (!wasLaTeX && isLaTeX) {
			btnLatex.setValue(true);
			if (editGeo != null) {
				editGeo.setLaTeX(true, false);
			}
		}
	}

	@Override
	public void onClick(ClickEvent event) {
		Object source = event.getSource();

		if (source == btnBold || source == btnItalic) {
			int style = 0;
			if (btnBold.getValue()) {
				style += 1;
			}
			if (btnItalic.getValue()) {
				style += 2;
			}
			editGeo.setFontStyle(style);
			updatePreviewPanel();

		} else if (source == btnLatex) {
			editGeo.setLaTeX(btnLatex.getValue(), false);
			// latex detection override
			mayDetectLaTeX = btnLatex.getValue();
			updatePreviewPanel();

		} else if (source == btnSerif) {
			editGeo.setSerifFont(btnSerif.getValue());
			updatePreviewPanel();

		} else if (source == btnInsert) {
			if (btnInsert.isDown()) {
				// open a symbol table
			} else {
				// close a symbol table
			}
		}

	}

	public void setLabels() {

		disclosurePanel.getHeaderTextAccessor()
		        .setText(loc.getMenu("Advanced"));
		if (!app.has(Feature.DIALOG_DESIGN)) {
			btnBold.setText(loc.getMenu("Bold.Short"));
			btnItalic.setText(loc.getMenu("Italic.Short"));
		}
		btnLatex.setText(loc.getMenu("LaTeXFormula"));
		btnSerif.setText(loc.getMenu("Serif"));
		if (advancedPanel != null) {
			advancedPanel.setLabels();
		}

	}

	private void registerListeners() {

		sl = new GeoElementSelectionListener() {
			@Override
			public void geoElementSelected(GeoElement geo,
			        boolean addToSelection) {
				if (geo != editGeo) {
					editor.insertGeoElement(geo);
				}
			}
		};

		editor.addFocusHandler(this);
	}

	// ======================================================
	// Getters/Setters
	// ======================================================

	public void setEditGeo(GeoText editGeo) {
		if (editGeo == null) {
			// create dummy GeoText to maintain the visual properties
			this.editGeo = new GeoText(app.getKernel().getConstruction());
		} else {
			this.editGeo = editGeo;
		}
	}

	@Override
	public GeoText getEditGeo() {
		return editGeo;
	}

	public boolean isLatex() {
		return btnLatex.getValue();
	}

	public boolean isSerif() {
		return btnSerif.getValue();
	}

	public boolean isBold() {
		return btnBold.getValue();
	}

	public boolean isItalic() {
		return btnItalic.getValue();
	}

	public GeoTextEditor getTextArea() {
		return editor;
	}

	// ======================================================
	// ToolBar
	// ======================================================

	private void createToolBar() {

		btnInsert = new ToggleButton(Unicode.alpha + "");
		btnInsert.addClickHandler(this);

		
		
		if (app.has(Feature.DIALOG_DESIGN)) {
			btnBold = new MyToggleButtonW(
					new ImageResourcePrototype(
							null, MaterialDesignResources.INSTANCE
									.text_bold_black().getSafeUri(),
							0, 0, 24, 24, false, false));
		} else {
			btnBold = new MyToggleButtonW(loc.getMenu("Bold.Short"));
		}
		btnBold.addClickHandler(this);
		btnBold.addStyleName("btnBold");

		if (app.has(Feature.DIALOG_DESIGN)) {
			btnItalic = new MyToggleButtonW(
					new ImageResourcePrototype(
							null, MaterialDesignResources.INSTANCE
									.text_italic_black().getSafeUri(),
							0, 0, 24, 24, false, false));
		} else {
			btnItalic = new MyToggleButtonW(loc.getMenu("Italic.Short"));
		}
		btnItalic.addClickHandler(this);
		btnItalic.addStyleName("btnItalic");
		
		btnSerif = new MyToggleButtonW(loc.getMenu("Serif"));
		btnSerif.addClickHandler(this);
		btnSerif.addStyleName("btnSerif");
		
		String latexTr = loc.getMenu("LaTeXFormula");
		btnLatex = new MyToggleButtonW(latexTr);
		btnLatex.addClickHandler(this);
		btnLatex.addStyleName("btnLatex");
		
		// TODO: put styles in css stylesheet
		

		HorizontalPanel leftPanel = new HorizontalPanel();
		leftPanel.setSpacing(2);
		// leftPanel.getElement().getStyle().setFloat(Style.Float.LEFT);
		leftPanel.add(btnBold);
		leftPanel.add(btnItalic);
		leftPanel.add(btnSerif);
		leftPanel.add(btnLatex);

		FlowPanel rightPanel = new FlowPanel();
		rightPanel.getElement().getStyle().setFloat(Style.Float.RIGHT);
		rightPanel.add(btnInsert);

		toolBar = new FlowPanel();
		// toolBar.setSize("100%", "20px");
		toolBar.getElement().getStyle().setFloat(Style.Float.LEFT);
		toolBar.getElement().getStyle().setFontSize(80, Unit.PCT);
		toolBar.add(leftPanel);
		// toolBar.add(rightPanel);

	}

	// ======================================================
	// Text Handlers
	// ======================================================

	public void setText(String text) {
		editor.setHTML(text);
	}

	/**
	 * @return Current editor content converted to a GeoText string.
	 */
	public String getText() {
		Log.debug("ggb text string: "
		        + dTProcessor.buildGeoGebraString(editor.getDynamicTextList(),
		                isLatex()));
		return dTProcessor.buildGeoGebraString(editor.getDynamicTextList(),
		        isLatex());
	}

	/**
	 * Sets editor content to represent the text string of a given GeoText.
	 * 
	 * @param geo
	 *            GeoText
	 */
	public void setText(GeoText geo) {

		ArrayList<DynamicTextElement> list = dTProcessor
		        .buildDynamicTextList(geo);

		editor.setText(list);

		if (geo == null) {
			btnLatex.setValue(false);
			btnSerif.setValue(false);
			btnBold.setValue(false);
			btnItalic.setValue(false);

		} else {

			btnLatex.setValue(geo.isLaTeX());
			btnSerif.setValue(geo.isSerifFont());
			int style = geo.getFontStyle();
			btnBold.setValue(style == GFont.BOLD
			        || style == (GFont.BOLD + GFont.ITALIC));
			btnItalic.setValue(style == GFont.ITALIC
			        || style == (GFont.BOLD + GFont.ITALIC));
		}

		updatePreviewPanel();

	}

	/**
	 * Inserts into the editor a dynamic text element representing a given
	 * GeoElement. The element is inserted at the current caret position.
	 * 
	 * @param geo
	 */
	@Override
	public void insertGeoElement(GeoElement geo) {
		editor.insertGeoElement(geo);

	}

	/**
	 * Inserts a text string into the editor at the current caret position.
	 * 
	 * @param text
	 * @param isLatex
	 */
	@Override
	public void insertTextString(String text, boolean isLatex) {
		editor.insertTextString(text, isLatex);
	}
	
	public void updateFonts(){
		editor.updateFonts();
	}

	public int getFontStyle() {
		return TextOptionsModel.getFontStyle(isBold(), isItalic());
	}

	@Override
	public void ensureLaTeX() {
		btnLatex.setValue(true);
		editGeo.setLaTeX(true, false);
		updatePreviewPanel();

	}
}
