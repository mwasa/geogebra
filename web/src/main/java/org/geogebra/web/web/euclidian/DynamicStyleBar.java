package org.geogebra.web.web.euclidian;

import org.geogebra.common.awt.GRectangle2D;
import org.geogebra.common.euclidian.EuclidianConstants;
import org.geogebra.common.euclidian.EuclidianView;
import org.geogebra.common.util.debug.Log;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;

public class DynamicStyleBar extends EuclidianStyleBarW {

	public DynamicStyleBar(EuclidianView ev) {
		super(ev, -1);
		addStyleName("DynamicStyleBar");

		// app.getSelectionManager().addSelectionListener(new
		// GeoElementSelectionListener() {
		// @Override
		// public void geoElementSelected(GeoElement geo,
		// boolean addToSelection) {
		// if(addToSelection){
		// return;
		// }
		// DynamicStyleBar.this.setVisible(addToSelection);
		// }
		// });
		stopPointer(getElement());
	}

	private native void stopPointer(Element element) /*-{
														if ($wnd.PointerEvent) {
														var evts = [ "PointerDown", "PointerUp" ];
														for ( var k in evts) {
														element.addEventListener(evts[k].toLowerCase(), function(e) {
														e.stopPropagation()
														});
														}
														}
														
														}-*/;

	/**
	 * Sets the position of dynamic style bar. newPos position of right top
	 * corner of bounding box of drawable
	 */
	@Override
	public void setPosition(GRectangle2D gRectangle2D, boolean hasBoundingBox) {

		// Log.printStacktrace("setPosition");
		Log.debug("rectangle:" + gRectangle2D.getMinX() + " " + gRectangle2D.getMaxX() + " " + gRectangle2D.getMinY()
				+ " " + gRectangle2D.getMaxY());
		Log.debug("hasboundinbox: " + hasBoundingBox);

		boolean oldVisible = this.isVisible();
		this.setVisible(true);

		// make sure it reflects selected geos
		setOpen(true);
		// updateButtons();
		setMode(EuclidianConstants.MODE_MOVE);
		super.updateStyleBar();

		// Calculates the x param. of distance between the start of dynamic
		// stylebar and the three dot button.
		this.getElement().getStyle().setTop(-10000, Unit.PX);

		// int move = this.getViewButton().getAbsoluteLeft()
		// - this.getAbsoluteLeft();
		int move = this.getOffsetWidth();
		int height = this.getOffsetHeight();
		Log.debug("move: " + move + " height: " + height);
		this.setVisible(oldVisible);

		double left, top;
		if (hasBoundingBox) {
			left = gRectangle2D.getMaxX() - move;
			top = gRectangle2D.getMinY() - height - 5;

		} else { // line has no bounding box
			left = gRectangle2D.getMaxX() - height / 2.0;
			top = gRectangle2D.getMinY();
		}

		// if there is no enough place on the top of bounding box, dynamic
		// stylebar will be visible at the bottom of bounding box.
		if (top < 0) {
			top = gRectangle2D.getMaxY() + 5;
		}

		int maxtop = app.getActiveEuclidianView().getHeight() - height - 5;
		if (top > maxtop) {
			top = maxtop;
		}

		if (left < 0) {
			left = 0;
		}

		if (left + this.getOffsetWidth() > app.getActiveEuclidianView().getWidth()) {
			left = app.getActiveEuclidianView().getWidth() - this.getOffsetWidth();
		}

		Log.debug("left: " + left + "top: " + top);
		this.getElement().getStyle().setLeft(left, Unit.PX);
		this.getElement().getStyle().setTop(top, Unit.PX);
	}
	
	protected boolean isDynamicStylebar(){
		return true;
	}
	
}
