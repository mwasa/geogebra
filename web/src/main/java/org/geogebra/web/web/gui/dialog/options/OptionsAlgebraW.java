package org.geogebra.web.web.gui.dialog.options;

import java.util.Arrays;
import java.util.List;

import org.geogebra.common.gui.SetLabels;
import org.geogebra.common.gui.dialog.options.OptionsAdvanced;
import org.geogebra.common.gui.view.algebra.AlgebraView.SortMode;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.main.settings.AbstractSettings;
import org.geogebra.common.main.settings.AlgebraSettings;
import org.geogebra.common.main.settings.SettingListener;
import org.geogebra.web.html5.gui.util.FormLabel;
import org.geogebra.web.html5.gui.util.LayoutUtilW;
import org.geogebra.web.html5.main.AppW;
import org.geogebra.web.html5.util.tabpanel.MultiRowsTabPanel;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author csilla
 *
 */
public class OptionsAlgebraW extends OptionsAdvanced
 implements OptionPanelW,
		SetLabels, SettingListener {

	private AppW app;
	private AlgebraTab algebraTab;
	/**
	 * tabs (for now only algebra)
	 */
	protected MultiRowsTabPanel tabPanel;

	/**
	 * @author csilla
	 *
	 */
	protected class AlgebraTab extends FlowPanel
			implements ClickHandler, ChangeHandler {
		private FlowPanel optionsPanel;
		private Label lblShow;
		private CheckBox showAuxiliaryObjects;
		private ListBox sortMode;
		private AlgebraStyleListBox description;
		private FormLabel lblCoordStyle;
		private ListBox coordStyle;
		private FormLabel lblAngleUnit;
		private ListBox angleUnit;
		private FormLabel lblSortMode;
		private FormLabel lblDescriptionMode;
		private List<SortMode> supportedModes = Arrays.asList(SortMode.DEPENDENCY,
			SortMode.TYPE, SortMode.ORDER, SortMode.LAYER);

		/**
		 * algebra tab in algebra settings panel
		 */
		public AlgebraTab() {
			createGUI();
			updateGUI();
			if (getApp().isUnbundledOrWhiteboard()) {
				setStyleName("propMaterialTab");
			} else {
				setStyleName("propertiesTab");
			}
			add(optionsPanel);
		}

		private void createGUI() {
			optionsPanel = new FlowPanel();
			lblShow = new Label();
			lblShow.addStyleName("panelTitle");
			showAuxiliaryObjects = new CheckBox();
			showAuxiliaryObjects.addClickHandler(this);

			sortMode = new ListBox();
			lblSortMode = new FormLabel().setFor(sortMode);
			lblSortMode.addStyleName("panelTitle");
			lblDescriptionMode = new FormLabel().setFor(sortMode);
			lblDescriptionMode.addStyleName("panelTitle");

			description = new AlgebraStyleListBox(getApp(), false);
			coordStyle = new ListBox();
			lblCoordStyle = new FormLabel(
					getApp().getLocalization().getMenu("Coordinates") + ":")
							.setFor(coordStyle);
			angleUnit = new ListBox();
			lblAngleUnit = new FormLabel(
					getApp().getLocalization().getMenu("AngleUnit") + ":")
							.setFor(angleUnit);

			optionsPanel.add(lblShow);
			optionsPanel.add(LayoutUtilW.panelRowIndent(showAuxiliaryObjects));
			optionsPanel.add(lblSortMode);
			optionsPanel.add(LayoutUtilW.panelRowIndent(sortMode));
			optionsPanel.add(lblDescriptionMode);
			optionsPanel.add(LayoutUtilW.panelRowIndent(description));

			optionsPanel.add(LayoutUtilW.panelRowIndent(lblCoordStyle, coordStyle));
			coordStyle.addChangeHandler(this);
			optionsPanel.add(LayoutUtilW.panelRowIndent(lblAngleUnit, angleUnit));
			angleUnit.addChangeHandler(this);
			sortMode.addChangeHandler(this);
			description.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				int idx = getDescription().getSelectedIndex();
				getApp().getKernel()
						.setAlgebraStyle(
							AlgebraSettings.getStyleModeAt(idx));
				getApp().getKernel().updateConstruction();
				}
			});
			setLabels();
		}

		/**
		 * @return sort mode combo box
		 */
		public ListBox getSortMode() {
			return sortMode;
		}

		/**
		 * @return show aux obj check box
		 */
		public CheckBox getShowAuxiliaryObjects() {
			return showAuxiliaryObjects;
		}

		/**
		 * @return coord style combo box
		 */
		public ListBox getCoordStyle() {
			return coordStyle;
		}

		/**
		 * @return angle unit combo box
		 */
		public ListBox getAngleUnit() {
			return angleUnit;
		}

		/**
		 * update sort mode combo box
		 */
		public void updateSortMode() {
			sortMode.clear();
			for (SortMode mode : supportedModes) {
				sortMode.addItem(
						getApp().getLocalization().getMenu(mode.toString()));
			}
			SortMode selectedMode = getApp().getAlgebraView().getTreeMode();
			sortMode.setSelectedIndex(supportedModes.indexOf(selectedMode));
		}

		/**
		 * update coord style combo box content
		 */
		public void updateCoordStyle() {
			lblCoordStyle
					.setText(getApp().getLocalization().getMenu("Coordinates")
							+ ":");
			coordStyle.clear();
			coordStyle
					.addItem(getApp().getLocalization().getMenu("A = (x, y)"));
			coordStyle.addItem(getApp().getLocalization().getMenu("A(x | y)"));
			coordStyle.addItem(getApp().getLocalization().getMenu("A: (x, y)"));
			coordStyle.setSelectedIndex(getApp().getKernel().getCoordStyle());
			getApp().getKernel().updateConstruction();
		}

		/**
		 * update angle unit
		 */
		public void updateAngleUnit() {
			lblAngleUnit
					.setText(getApp().getLocalization().getMenu("AngleUnit") + ":");
			angleUnit.clear();
			angleUnit.addItem(getApp().getLocalization().getMenu("Degree"));
			angleUnit.addItem(getApp().getLocalization().getMenu("Radiant"));
			angleUnit.setSelectedIndex(
					getApp().getKernel().getAngleUnit() == Kernel.ANGLE_RADIANT
							? 1 : 0);
			getApp().getKernel().updateConstruction();
			getApp().setUnsaved();
		}

		/**
		 * update content GUI
		 */
		public void updateGUI() {
			showAuxiliaryObjects.setValue(getApp().showAuxiliaryObjects);
			updateSortMode();
			description.update();
			updateCoordStyle();
			updateAngleUnit();
		}

		/**
		 * @return description combo box
		 */
		public AlgebraStyleListBox getDescription() {
			return description;
		}

		/**
		 * @param description
		 *            - list of description style
		 */
		public void setDescription(AlgebraStyleListBox description) {
			this.description = description;
		}

		/**
		 * set text of labels
		 */
		public void setLabels() {
			lblShow.setText(getApp().getLocalization().getMenu("Show"));
			showAuxiliaryObjects
					.setText(getApp().getLocalization()
							.getMenu("AuxiliaryObjects"));
			lblSortMode.setText(getApp().getLocalization().getMenu("SortBy"));
			lblDescriptionMode.setText(
					getApp().getLocalization().getMenu("AlgebraDescriptions"));
		}

		@Override
		public void onChange(ChangeEvent event) {
			Object source = event.getSource();
			if (source == getSortMode()) {
				int i = getSortMode().getSelectedIndex();
				getApp().getSettings().getAlgebra()
						.setTreeMode(supportedModes.get(i));
			} else if (source == getCoordStyle()) {
				int i = getCoordStyle().getSelectedIndex();
				getApp().getKernel().setCoordStyle(i);
				getApp().getKernel().updateConstruction();
			} else if (source == getAngleUnit()) {
				int i = getAngleUnit().getSelectedIndex();
				getApp().getKernel().setAngleUnit(
						i == 0 ? Kernel.ANGLE_DEGREE : Kernel.ANGLE_RADIANT);
				getApp().getKernel().updateConstruction();
				getApp().setUnsaved();
			}
		}

		@Override
		public void onClick(ClickEvent event) {
			Object source = event.getSource();
			if (source == getShowAuxiliaryObjects()) {
				getApp().setShowAuxiliaryObjects(
						getShowAuxiliaryObjects().getValue());
			}
		}

	}

	/**
	 * @param app
	 *            application
	 */
	public OptionsAlgebraW(AppW app) {
		this.app = app;
		tabPanel = new MultiRowsTabPanel();
		algebraTab = new AlgebraTab();
		tabPanel.add(algebraTab, app.getLocalization().getMenu("Algebra"));
		updateGUI();
		tabPanel.selectTab(0);
		app.setDefaultCursor();
		app.getSettings().getAlgebra().addListener(this);
	}

	@Override
	public Widget getWrappedPanel() {
		return tabPanel;
    }

	@Override
    public void onResize(int height, int width) {
		// TO DO
    }

	/**
	 * @return application
	 */
	public AppW getApp() {
		return app;
	}

	/**
	 * @param app
	 *            application
	 */
	public void setApp(AppW app) {
		this.app = app;
	}

	/**
	 * @return algebra tab
	 */
	public AlgebraTab getAlgebraTab() {
		return algebraTab;
	}

	@Override
	public void settingsChanged(AbstractSettings settings) {
		updateGUI();
	}

	@Override
	public MultiRowsTabPanel getTabPanel() {
		return null;
	}

	@Override
	public void setLabels() {
		algebraTab.setLabels();
		algebraTab.updateSortMode();
		algebraTab.getDescription().update();
		algebraTab.updateCoordStyle();
		algebraTab.updateAngleUnit();
	}

	@Override
	public void updateGUI() {
		algebraTab.updateGUI();
	}
}
