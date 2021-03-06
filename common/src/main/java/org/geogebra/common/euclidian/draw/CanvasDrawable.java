package org.geogebra.common.euclidian.draw;

import org.geogebra.common.awt.GDimension;
import org.geogebra.common.awt.GFont;
import org.geogebra.common.awt.GGraphics2D;
import org.geogebra.common.awt.GPoint;
import org.geogebra.common.awt.GRectangle;
import org.geogebra.common.awt.font.GTextLayout;
import org.geogebra.common.euclidian.Drawable;
import org.geogebra.common.euclidian.EuclidianStatic;
import org.geogebra.common.factories.AwtFactory;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.TextProperties;
import org.geogebra.common.main.App;

public abstract class CanvasDrawable extends Drawable {
	private static final int HIGHLIGHT_MARGIN = 2;

	// private boolean drawingOnCanvas;
	private GFont labelFont;
	GPoint labelSize = new GPoint(0, 0);
	private int labelFontSize;
	private GRectangle hitRect = AwtFactory.getPrototype().newRectangle();
	int boxLeft;
	int boxTop;
	int boxWidth;
	int boxHeight;

	public static boolean isLatexString(String text) {
		return text.length() > 1 && text.startsWith("$")
				&& text.trim().endsWith("$");
	}

	protected GDimension measureLatex(GGraphics2D g2, GeoElement geo0,
			GFont font, String text) {

		return drawLatex(g2, geo0, font, text, Integer.MIN_VALUE,
				Integer.MIN_VALUE);
	}

	protected GDimension drawLatex(GGraphics2D g2, GeoElement geo0, GFont font,
			String text, int x, int y) {
		App app = view.getApplication();

		boolean serif = false;
		if (geo0 instanceof TextProperties) {
			serif = ((TextProperties) geo0).isSerifFont();
		}

		return app.getDrawEquation().drawEquation(app, geo0, g2, x, y, text,
				font, serif, geo.getObjectColor(), geo.getBackgroundColor(),
				false, false, null);
	}

	public static GDimension measureLatex(App app,
			GeoElement geo0, GFont font, String text) {
		return app.getDrawEquation().measureEquation(app, geo0, text, font,
				false);
	}

	protected boolean measureLabel(GGraphics2D g2, GeoElement geo0,
			String text) {
		boolean latex = false;
		if (geo.isLabelVisible()) {
			latex = isLatexString(text);
			// no drawing, just measuring.
			if (latex) {
				GDimension d = measureLatex(g2, geo0, getLabelFont(), text);
				labelSize.x = d.getWidth();
				labelSize.y = d.getHeight();
			} else {
				g2.setFont(getLabelFont());
				setLabelSize(
						EuclidianStatic.drawIndexedString(view.getApplication(),
								g2, text, 0, 0, false, false, null, null));
			}
			calculateBoxBounds(latex);
		} else {
			calculateBoxBounds();
		}
		return latex;
	}

	protected void calculateBoxBounds(boolean latex) {
		boxLeft = xLabel + labelSize.x + 2;
		boxTop = latex
				? yLabel + (labelSize.y - getPreferredSize().getHeight()) / 2
				: yLabel;
		boxWidth = getPreferredSize().getWidth();
		boxHeight = getPreferredSize().getHeight();
	}

	protected void calculateBoxBounds() {
		boxLeft = xLabel + 2;
		boxTop = yLabel;
		boxWidth = getPreferredSize().getWidth();
		boxHeight = getPreferredSize().getHeight();
	}

	protected void highlightLabel(GGraphics2D g2, boolean latex) {
		if (geo.isLabelVisible() && geo.doHighlighting()) {
			if (latex) {
				g2.fillRect(xLabel, yLabel, labelSize.x, labelSize.y);
			} else {
				g2.fillRect(xLabel, yLabel + ((boxHeight - getH()) / 2),
						labelSize.x, getH() + HIGHLIGHT_MARGIN);
			}
		}
	}

	protected int getTextBottom() {
		return (getPreferredSize().getHeight() / 2)
				+ (int) (getLabelFontSize() * 0.4);
	}

	protected int getH() {
		return (int) (getLabelFontSize() * 1.2 + HIGHLIGHT_MARGIN);
	}

	protected void drawLabel(GGraphics2D g2, GeoElement geo0, String text) {
		if (isLatexString(text)) {
			drawLatex(g2, geo0, getLabelFont(), text, xLabel, yLabel);
		} else {
			g2.setPaint(geo.getObjectColor());

			EuclidianStatic.drawIndexedString(view.getApplication(), g2, text,
					xLabel, yLabel + getTextBottom(), false, view,
					geo.getObjectColor());
		}

	}

	protected void drawOnCanvas(GGraphics2D g2, String text) {
		App app = view.getApplication();
		getPreferredSize();

		GFont vFont = view.getFont();
		setLabelFont(app.getFontCanDisplay(text, false, vFont.getStyle(),
				getLabelFontSize()));

		g2.setFont(getLabelFont());
		g2.setStroke(EuclidianStatic.getDefaultStroke());

		g2.setPaint(geo.getObjectColor());

		if (geo.isVisible()) {
			drawWidget(g2);
		}
	}


	protected int getTextDescent(GGraphics2D g2, String text) {
		// make sure layout won't be null ("" makes it null).

		GTextLayout layout = getLayout(g2, text, getLabelFont());
		return (int) (layout.getDescent());
	}

	protected GTextLayout getLayout(GGraphics2D g2, String text, GFont font) {
		// make sure layout won't be null ("" makes it null).
		return getTextLayout("".equals(text) ? "A" : text, font, g2);
	}

	protected abstract void drawWidget(GGraphics2D g2);

	/**
	 * was this object clicked at? (mouse pointer location (x,y) in screen
	 * coords)
	 */
	@Override
	public boolean hit(int x, int y, int hitThreshold) {
		int left = xLabel;
		int top = boxTop;
		int right = left + labelSize.x + boxWidth;
		int bottom = top + boxHeight;
		//
		return (x > left && x < right && y > top && y < bottom)
				|| (x > xLabel && x < xLabel + labelSize.x && y > yLabel
						&& y < yLabel + labelSize.y);
	}

	public GFont getLabelFont() {
		// deriveFont() as quick fix for GGB-2094
		return labelFont.deriveFont(GFont.PLAIN);
	}

	private void setLabelFont(GFont labelFont) {
		this.labelFont = labelFont;
	}

	private void setLabelSize(GPoint labelSize) {
		this.labelSize = labelSize;
	}

	public int getLabelFontSize() {
		return labelFontSize;
	}

	public void setLabelFontSize(int labelFontSize) {
		this.labelFontSize = labelFontSize;
	}

	public abstract GDimension getPreferredSize();

	@Override
	public boolean isInside(GRectangle rect) {
		return rect.contains(labelRectangle);
	}

	@Override
	public boolean intersectsRectangle(GRectangle rect) {
		return hitRect.intersects(rect);
	}

	/**
	 * Returns false
	 */
	@Override
	public boolean hitLabel(int x, int y) {
		return false;
	}

	@Override
	final public GeoElement getGeoElement() {
		return geo;
	}

	@Override
	public void setGeoElement(GeoElement geo) {
		this.geo = geo;
	}

	protected abstract void showWidget();

	protected abstract void hideWidget();

	public void setWidgetVisible(boolean show) {
		if (geo.isEuclidianVisible() && view.isVisibleInThisView(geo) && show) {
			showWidget();
		} else {
			hideWidget();
		}
	}

	public GRectangle getHitRect() {
		return labelRectangle;
	}
}
