/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

package org.geogebra.common.kernel.geos;

import java.util.ArrayList;

import org.geogebra.common.awt.GPoint2D;
import org.geogebra.common.awt.GRectangle2D;
import org.geogebra.common.euclidian.EuclidianBoundingBoxHandler;
import org.geogebra.common.euclidian.event.AbstractEvent;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.MyPoint;
import org.geogebra.common.kernel.Path;
import org.geogebra.common.kernel.PathMover;
import org.geogebra.common.kernel.PathMoverLocus;
import org.geogebra.common.kernel.PathParameter;
import org.geogebra.common.kernel.SegmentType;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.Matrix.Coords;
import org.geogebra.common.kernel.algos.AlgoLocusSliderInterface;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.kernel.kernelND.GeoPointND;
import org.geogebra.common.kernel.kernelND.GeoSegmentND;
import org.geogebra.common.plugin.GeoClass;
import org.geogebra.common.util.debug.Log;

/**
 * Locus of points
 * 
 * @author Markus
 * @param <T>
 *            2D or 3D point type
 */
public abstract class GeoLocusND<T extends MyPoint> extends GeoElement
		implements Path, Traceable, GeoLocusNDInterface {

	/** maximal number of runs through the path when computing */
	public static final int MAX_PATH_RUNS = 10;

	private boolean defined;

	/** coords of points on locus */
	protected ArrayList<T> myPointList;
	private ArrayList<GPoint2D> nonScaledPointList;
	private double nonScaledWidth;
	private double nonScaledHeight;
	private ArrayList<T> poitsWithoutControl;
	private StringBuilder sbToString = new StringBuilder(80);
	private double closestPointDist;
	/**
	 * index of point closest to changingPoint
	 */
	protected int closestPointIndex;
	/**
	 * parameter of point closest to changingPoint
	 */
	protected double closestPointParameter;

	private boolean trace;

	/**
	 * Creates new locus
	 * 
	 * @param c
	 *            construction
	 */
	public GeoLocusND(Construction c) {
		super(c);

		// moved from GeoElement's constructor
		// must be called from the subclass, see
		// http://benpryor.com/blog/2008/01/02/dont-call-subclass-methods-from-a-superclass-constructor/
		setConstructionDefaults(); // init visual settings

		myPointList = new ArrayList<>(500);
	}

	@Override
	public GeoElement copy() {
		GeoLocusND<T> ret = newGeoLocus();
		ret.set(this);
		return ret;
	}

	/**
	 * 
	 * @return new GeoLocus of same type
	 */
	abstract protected GeoLocusND<T> newGeoLocus();

	@SuppressWarnings("unchecked")
	@Override
	public void set(GeoElementND geo) {
		if (geo instanceof GeoLocusND) {
			GeoLocusND<T> locus = (GeoLocusND<T>) geo;
			defined = locus.defined;

			myPointList.clear();
			for (MyPoint pt : locus.myPointList) {
				myPointList.add((T) pt.copy());
			}
		}
	}

	/**
	 * Number of valid points in x and y arrays.
	 * 
	 * @return number of valid points in x and y arrays.
	 */
	final public int getPointLength() {
		return myPointList.size();
	}

	/**
	 * Clears list of points defining this locus
	 */
	public void clearPoints() {
		myPointList.clear();
	}

	/**
	 * @return list of points that define this locus
	 */
	@Override
	public ArrayList<T> getPoints() {
		return myPointList;
	}

	/**
	 * Updates the points when resizing the locus with bounding box handler
	 * 
	 * @param handler
	 *            handler was hit
	 * @param event
	 *            event to handle
	 * @param gRectangle2D
	 *            bounding box rectangle
	 */
	public void updatePoints(EuclidianBoundingBoxHandler handler,
			AbstractEvent event, GRectangle2D gRectangle2D) {
		double minX = gRectangle2D.getMinX();
		double maxX = gRectangle2D.getMaxX();
		double minY = gRectangle2D.getMinY();
		double maxY = gRectangle2D.getMaxY();

		// save the original rates when scaling first time
		if (nonScaledPointList == null) {
			nonScaledPointList = new ArrayList<>(myPointList.size());
			nonScaledWidth = maxX - minX;
			nonScaledHeight = maxY - minY;
			for (int i = 0; i < myPointList.size(); i++) {
				double x = myPointList.get(i).getX();
				double y = myPointList.get(i).getY();
				
				if (Double.isNaN(x)) {
					nonScaledPointList
							.add(new GPoint2D.Double(Double.NaN, Double.NaN));
				} else {
					nonScaledPointList.add(new GPoint2D.Double(
							kernel.getApplication().getActiveEuclidianView()
									.toScreenCoordX(x) - minX,
							kernel.getApplication().getActiveEuclidianView()
									.toScreenCoordY(y) - minY));
				}
			}
		}

		switch (handler) {
		case TOP:
			updatePointsY((maxY - event.getY()) / nonScaledHeight,
					event.getY());
			break;
		case BOTTOM:
			updatePointsY((event.getY() - minY) / nonScaledHeight,
					minY);
			break;
		case LEFT:
			updatePointsX((maxX - event.getX()) / nonScaledWidth, event.getX());
			break;
		case RIGHT:
			updatePointsX((event.getX() - minX) / nonScaledWidth, minX);
			break;
		default:
			Log.warn("unhandled case");
		}
	}

	private void updatePointsX(double scaleX, double newMinX) {
		for (int i = 0; i < myPointList.size(); i++) {
			double newPointScreenX = nonScaledPointList.get(i).getX() * scaleX
					+ newMinX;
			myPointList.get(i)
					.setX(kernel.getApplication().getActiveEuclidianView()
							.toRealWorldCoordX(newPointScreenX));
		}
	}

	private void updatePointsY(double scaleY, double newMinY) {
		for (int i = 0; i < myPointList.size(); i++) {
			double newPointScreenY = nonScaledPointList.get(i).getY() * scaleY
					+ newMinY;
			myPointList.get(i)
					.setY(kernel.getApplication().getActiveEuclidianView()
							.toRealWorldCoordY(newPointScreenY));
		}
	}

	/**
	 * Reset list of points for XML
	 */
	public void resetPointsWithoutControl() {
		poitsWithoutControl = null;
	}
	/**
	 * @return points without control points
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<T> getPointsWithoutControl() {
		if (poitsWithoutControl == null) {
			poitsWithoutControl = new ArrayList<>();
			for (MyPoint t : myPointList) {
				if (t.getSegmentType() != SegmentType.CONTROL) {
					poitsWithoutControl.add((T) t.copy());
				}
			}
		}
		return poitsWithoutControl;
	}

	@Override
	public String toString(StringTemplate tpl) {
		sbToString.setLength(0);
		sbToString.append(label);
		sbToString.append(" = ");
		sbToString.append(getDefinition(tpl));
		return sbToString.toString();
	}

	@Override
	public boolean showInAlgebraView() {
		return true;
	}

	@Override
	public GeoClass getGeoClassType() {
		return GeoClass.LOCUS;
	}

	/**
	 * returns all class-specific xml tags for getXML
	 */
	@Override
	protected void getXMLtags(StringBuilder sb) {
		super.getXMLtags(sb);
		getLineStyleXML(sb);
	}

	@Override
	public boolean isDefined() {
		return defined;
	}

	/**
	 * @param flag
	 *            true to make this locus defined
	 */
	public void setDefined(boolean flag) {
		defined = flag;
	}

	@Override
	public void setUndefined() {
		defined = false;
	}

	@Override
	public String toValueString(StringTemplate tpl) {
		return getDefinition(tpl);
	}

	@Override
	protected boolean showInEuclidianView() {
		return isDefined();
	}

	@Override
	public boolean isGeoLocus() {
		return true;
	}

	@Override
	public double getMaxParameter() {
		return myPointList.size() - 1;
	}

	@Override
	public double getMinParameter() {
		return 0;
	}

	@Override
	public boolean isClosedPath() {
		if (myPointList.size() > 0) {
			MyPoint first = myPointList.get(0);
			MyPoint last = myPointList.get(myPointList.size() - 1);
			return first.isEqual(last);
		}
		return false;
	}

	@Override
	public boolean isOnPath(GeoPointND P, double eps) {

		setChangingPoint(P);
		MyPoint closestPoint = getClosestPoint();
		if (closestPoint != null) {
			return closestPointDist < eps;
		}
		return false;
	}

	/**
	 * set infos for current changing point
	 * 
	 * @param P
	 *            point
	 */
	abstract protected void setChangingPoint(GeoPointND P);

	/**
	 * 
	 * @param segment
	 *            segment
	 * @return closest parameter on the segment from the changing point
	 */
	abstract protected double getChangingPointParameter(GeoSegmentND segment);

	/**
	 * @return closest point to changing point
	 */
	protected MyPoint getClosestPoint() {

		getClosestLine();

		boolean temp = cons.isSuppressLabelsActive();
		cons.setSuppressLabelCreation(true);
		GeoSegmentND closestSegment = newGeoSegment();
		cons.setSuppressLabelCreation(temp);

		if (closestPointIndex == -1) {
			return null;
		}

		MyPoint locusPoint = myPointList.get(closestPointIndex);
		MyPoint locusPoint2 = myPointList.get(closestPointIndex + 1);

		closestSegment.setCoords(locusPoint, locusPoint2);

		closestPointParameter = getChangingPointParameter(closestSegment);

		if (closestPointParameter < 0) {
			closestPointParameter = 0;
		} else if (closestPointParameter > 1) {
			closestPointParameter = 1;
		}

		return locusPoint.barycenter(closestPointParameter, locusPoint2);
	}

	/**
	 * 
	 * @return new GeoSegment
	 */
	abstract protected GeoSegmentND newGeoSegment();

	/**
	 * 
	 * @param segment
	 *            segment
	 * @return distance from current point infos to segment
	 */
	abstract protected double changingPointDistance(GeoSegmentND segment);

	/**
	 * Returns the point of this locus that is closest to current point infos.
	 */
	private void getClosestLine() {
		int size = myPointList.size();
		if (size == 0) {
			return;
		}

		// search for closest point on path
		// MyPoint closestPoint = null;
		closestPointDist = Double.MAX_VALUE;
		closestPointIndex = -1;

		// make a segment and points to reuse
		GeoSegmentND segment = newGeoSegment();

		// search for closest point
		for (int i = 0; i < size - 1; i++) {
			MyPoint locusPoint = myPointList.get(i);
			MyPoint locusPoint2 = myPointList.get(i + 1);

			// not a line, just a move (eg Voronoi Diagram)
			if (locusPoint2.getSegmentType() == SegmentType.MOVE_TO) {
				continue;
			}

			// line thro' 2 points
			segment.setCoords(locusPoint, locusPoint2);

			double dist = changingPointDistance(segment);
			if (dist < closestPointDist) {
				closestPointDist = dist;
				closestPointIndex = i;
			}
		}

	}

	@Override
	public void pathChanged(GeoPointND P) {

		// if kernel doesn't use path/region parameters, do as if point changed
		// its coords
		// #4405 if segment number changed during file loading (EV viewport)
		// also don't use the new path update method
		if (!getKernel().usePathAndRegionParameters(P)
				|| cons.isFileLoading()) {
			pointChanged(P);
			return;
		}

		// find closest point on changed path to P
		if (getParentAlgorithm() instanceof AlgoLocusSliderInterface) {
			pointChanged(P);
			return;
		}

		// new method
		// keep point on same segment, the same proportion along it
		// better for loci with very few segments eg from ShortestDistance[ ]
		PathParameter pp = P.getPathParameter();

		int n = (int) Math.floor(pp.t);

		double t = pp.t - n; // between 0 and 1

		// check n and n+1 are in a sensible range
		// might occur if locus has changed no of segments/points
		if (n >= myPointList.size() || n < 0) {
			n = (n < 0) ? 0 : myPointList.size() - 1;
		}

		MyPoint locusPoint = myPointList.get(n);
		MyPoint locusPoint2 = myPointList.get((n + 1) % myPointList.size());

		P.set(t, 1 - t, locusPoint, locusPoint2);

	}

	/**
	 * @param P
	 *            point
	 * @param pp
	 *            path parameter
	 */
	public void pathChanged(Coords P, PathParameter pp) {
		int n = (int) Math.floor(pp.t);

		double t = pp.t - n; // between 0 and 1

		// check n and n+1 are in a sensible range
		// might occur if locus has changed no of segments/points
		if (n >= myPointList.size() || n < 0) {
			n = (n < 0) ? 0 : myPointList.size() - 1;
		}

		MyPoint locusPoint = myPointList.get(n);
		MyPoint locusPoint2 = myPointList.get((n + 1) % myPointList.size());

		P.set(t, 1 - t, locusPoint, locusPoint2);
	}

	@Override
	public boolean isPath() {
		return true;
	}

	// Michael Borcherds 2008-04-30
	@Override
	final public boolean isEqual(GeoElementND geo) {
		// return false if it's a different type, otherwise use equals() method
		return false;
		// TODO?
		// if (geo.isGeoLocus()) return xxx else return false;
	}

	/**
	 * Returns whether the value (e.g. equation) should be shown as part of the
	 * label description
	 */
	@Override
	final public boolean isLabelValueShowable() {
		return true;
	}

	@Override
	final public boolean isLabelShowable() {
		return true;
	}

	/**
	 * @param al
	 *            list of points that definr this locus
	 */
	public void setPoints(ArrayList<T> al) {
		myPointList = al;

	}

	@Override
	final public boolean isAuxiliaryObjectByDefault() {
		return true;
	}

	@Override
	public void setTrace(boolean trace) {
		this.trace = trace;
	}

	@Override
	public boolean getTrace() {
		return trace;
	}

	@Override
	public boolean isTraceable() {
		return true;
	}

	@Override
	public boolean isFillable() {
		return true;
	}

	@Override
	public boolean isInverseFillable() {
		return true;
	}

	@Override
	final public PathMover createPathMover() {
		return new PathMoverLocus<>(this);
	}

	@Override
	public boolean hasDrawable3D() {
		return true;
	}

	@Override
	public GeoLocusND<? extends MyPoint> getLocus() {
		return this;
	}
}
