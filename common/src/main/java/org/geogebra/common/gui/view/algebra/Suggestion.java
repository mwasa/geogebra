package org.geogebra.common.gui.view.algebra;

import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.util.StringUtil;

public class Suggestion {
	private String[] labels;

	public Suggestion(String... labels) {
		this.labels = labels;
	}

	public String getLabels(GeoElementND geo) {
		if (labels[labels.length - 1] == null) {
			labels[labels.length - 1] = geo.getLabelSimple();
		}
		return labels.length == 1 ? labels[0]
				: "{" + StringUtil.join(", ", labels) + "}";
	}
}