package org.geogebra.stepbystep;

import org.geogebra.commands.CommandsTest;
import org.geogebra.common.kernel.stepbystep.solution.SolutionBuilder;
import org.geogebra.common.kernel.stepbystep.solution.SolutionStep;
import org.geogebra.common.kernel.stepbystep.steptree.StepExpression;
import org.geogebra.common.kernel.stepbystep.steptree.StepNode;
import org.geogebra.common.kernel.stepbystep.steptree.StepVariable;
import org.geogebra.common.main.App;
import org.geogebra.common.util.debug.Log;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class DifferentiateStepTest {
	private static App app;

	@BeforeClass
	public static void setupApp() {
		app = CommandsTest.createApp();
	}

	private static HtmlStepBuilder htmlBuilder = new HtmlStepBuilder();
	private boolean needsHeading;
	private static int caseCounter = 0;

	@Test
	public void differentiateTest() {
		d("12x + 6x^2 + x^3", "x", "(12 + (12)(x) + (3)((x)^(2)))");
		d("log(x) + log_3(x)", "x", "((1)/(x) + (1)/((log_(e)(3))(x)))");
		d("sin(x) + cos(x) + tan(x)", "x", "(cos(x)-sin(x) + (1)/((cos(x))^(2)))");
		d("arcsin(x) + arccos(x) + arctan(x)", "x", "(1)/(((x)^(2) + 1))");
		d("sqrt(x) + nroot(x, 3) + nroot(x, 4)", "x",
				"((nroot(x, 2))/((2)(x)) + (1)/((3)(nroot((x)^(2), 3))) + (1)/((4)(nroot((x)^(3), 4))))");
		d("sin(x)*cos(x)", "x", "(-(sin(x))^(2) + (cos(x))^(2))");
		d("sin(x) / cos(x)", "x", "(((cos(x))^(2) + (sin(x))^(2)))/((cos(x))^(2))");
		d("e^x + 3^x", "x", "((e)^(x) + (log_(e)(3))((3)^(x)))");

		d("y*x", "x", "y");
		d("y*x", "y", "x");
		d("(sin(x))^2", "x", "(2)(sin(x))(cos(x))");
		d("e^(cos(x) * (ln(x))^2)", "x",
				"((e)^((cos(x))((log_(e)(x))^(2))))((((cos(x))(2)(log_(e)(x)))/(x) + (-sin(x))((log_(e)(x))^(2))))");
		d("(sin(x)^2)", "x", "(2)(sin(x))(cos(x))");
	}

	public void d(String toDifferentiate, String variable, String expectedResult) {
		if (needsHeading) {
			Throwable t = new Throwable();
			htmlBuilder.addHeading(t.getStackTrace()[1].getMethodName(), 1);
			needsHeading = false;
		}
		htmlBuilder.addHeading("Testcase " + (caseCounter++), 2);

		SolutionBuilder sb = new SolutionBuilder();
		
		StepExpression sn = (StepExpression) StepNode.getStepTree(toDifferentiate, app.getKernel().getParser());
		StepExpression input = StepNode.differentiate(sn, new StepVariable(variable));
		String result = input.differentiateOutput(sb).toString();

		SolutionStep steps = sb.getSteps();
		steps.getListOfSteps(htmlBuilder, app.getLocalization());

		Assert.assertEquals(expectedResult, result);
	}

	@Before
	public void resetHeading() {
		needsHeading = true;
	}

	@AfterClass
	public static void printHtml() {
		htmlBuilder.printReport("differentiate.html");
	}
}
