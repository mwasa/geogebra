package org.geogebra.common.kernel.stepbystep.steptree;

import java.util.List;

import org.geogebra.common.kernel.stepbystep.solution.SolutionBuilder;
import org.geogebra.common.kernel.stepbystep.solution.SolutionStepType;
import org.geogebra.common.kernel.stepbystep.steps.RegroupTracker;
import org.geogebra.common.kernel.stepbystep.steps.StepStrategies;
import org.geogebra.common.plugin.Operation;

public abstract class StepExpression extends StepNode {

	@Override
	public abstract StepExpression deepCopy();

	/**
	 * !This does not mean that getValue() will not return NaN.
	 * StepArbitraryConstants are constants, but cannot be evaluated
	 * 
	 * @return whether this expression contains variables
	 */
	public boolean isConstant() {
		return isConstantIn(null);
	}

	/**
	 * If the expression is a polynomial in var, returns its degree, -1 otherwise
	 * @param var variable of polynomial
	 * @return degree or -1
	 */
	public abstract int degree(StepVariable var);

	public abstract boolean isConstantIn(StepVariable sv);

	/**
	 * It guarantees that getValue() returns a non-NaN value. Except when division,
	 * Math.pow, or Math.sqrt fails.
	 * 
	 * @return whether this expression can be evaluated
	 */
	public abstract boolean canBeEvaluated();

	/**
	 * @return the numeric value of the tree.
	 */
	public abstract double getValue();

	/**
	 * @param variable
	 *            the name of the variable to be replaced
	 * @param value
	 *            the value to be replaced with
	 * @return the value of the tree after replacement
	 */
	public abstract double getValueAt(StepVariable variable, double value);

	/**
	 * @return the non-variable coefficient of the tree (ex: 3 sqrt(3) x -> 3
	 *         sqrt(3))
	 */
	public StepExpression getCoefficient() {
		return getCoefficientIn(null);
	}

	/**
	 * @return the variable part of the tree (ex: 3 x (1/sqrt(x)) -> x (1/sqrt(x)))
	 */
	public StepExpression getVariable() {
		return getVariableIn(null);
	}

	public abstract StepExpression getCoefficientIn(StepVariable sv);

	public abstract StepExpression getVariableIn(StepVariable sv);

	/**
	 * @return the StepConstant coefficient of the tree (ex: 3 sqrt(3) -> 3)
	 */
	public abstract StepExpression getIntegerCoefficient();

	/**
	 * @return the non-StepConstant part of the tree (ex: 3 sqrt(3) -> sqrt(3))
	 */
	public abstract StepExpression getNonInteger();

	/**
	 * Non-special constants are StepConstants and minus(StepConstant)s, except for
	 * pi and e
	 * 
	 * @return whether the current node is a nonSpecialConstant
	 */
	public boolean nonSpecialConstant() {
		return this instanceof StepConstant && !isEqual(getValue(), Math.PI) && !isEqual(getValue(), Math.E) &&
				!Double.isInfinite(getValue()) || isOperation(Operation.MINUS) && ((StepOperation) this).getOperand(0)
				.nonSpecialConstant();
	}

	/**
	 * Special constants are pi and e
	 * 
	 * @return whether the current node is a specialConstant
	 */
	public boolean specialConstant() {
		return this instanceof StepConstant && (isEqual(getValue(), Math.PI) || isEqual(getValue(), Math.E));
	}

	/**
	 * @return whether the current node is an integer (not Integer..)
	 */
	public boolean isInteger() {
		return canBeEvaluated() && isEqual(Math.round(getValue()), getValue());
	}

	/**
	 * @return whether the current node is a squreRoot (that is nroot, with an
	 *         exponent of 2)
	 */
	public boolean isSquareRoot() {
		return isOperation(Operation.NROOT) && isEqual(((StepOperation) this).getOperand(1), 2);
	}


	/**
	 * @param expr
	 *            expression to find
	 * @return all subexpression which contain expr
	 */
	public StepExpression findExpression(StepExpression expr) {
		if (equals(expr)) {
			return this;
		}

		if (this instanceof StepOperation) {
			StepOperation so = (StepOperation) this;

			if (so.isOperation(Operation.MULTIPLY)) {
				for (StepExpression operand : so) {
					if (operand.equals(expr)) {
						return so;
					}
				}
			} else if (so.isOperation(Operation.MINUS)) {
				return minus(so.getOperand(0).findExpression(expr));
			} else if (so.isOperation(Operation.PLUS)) {
				StepExpression found = null;
				for (int i = 0; i < so.noOfOperands(); i++) {
					found = add(found, so.getOperand(i).findExpression(expr));
				}
				return found;
			} else if (so.isOperation(Operation.DIVIDE) && so.getOperand(1).isConstant()) {
				StepExpression nominator = so.getOperand(0).findExpression(expr);

				if (nominator != null) {
					return divide(nominator, so.getOperand(1));
				}
			}
		}

		return null;
	}

	/**
	 * @param expr
	 *            expression to find
	 * @return the coefficient of expr in the tree
	 */
	public StepExpression findCoefficient(StepExpression expr) {
		if (equals(expr)) {
			return StepConstant.create(1);
		}

		if (this instanceof StepOperation) {
			StepOperation so = (StepOperation) this;

			if (so.isOperation(Operation.MULTIPLY)) {
				StepExpression coeff = null;
				boolean found = false;

				for (StepExpression operand : so) {
					StepExpression current = operand.findCoefficient(expr);
					if (current == null) {
						coeff = nonTrivialProduct(coeff, operand);
					} else {
						found = true;
						coeff = nonTrivialProduct(coeff, current);
					}
				}

				if (found) {
					return coeff;
				}
			} else if (so.isOperation(Operation.MINUS)) {
				return minus(so.getOperand(0).findCoefficient(expr));
			} else if (so.isOperation(Operation.PLUS)) {
				StepExpression found = null;
				for (StepExpression operand : so) {
					found = add(found, operand.findCoefficient(expr));
				}
				return found;
			} else if (so.isOperation(Operation.DIVIDE) && so.getOperand(1).isConstant()) {
				StepExpression nominator = so.getOperand(0).findCoefficient(expr);

				if (nominator != null) {
					return divide(nominator, so.getOperand(1));
				}
			}
		}

		return null;
	}

	/**
	 * @return subexpression which does not contain any variable part
	 */
	public StepExpression findConstantIn(StepVariable sv) {
		if (isConstantIn(sv)) {
			return this;
		}

		if (this instanceof StepOperation) {
			StepOperation so = (StepOperation) this;

			if (so.isOperation(Operation.MINUS)) {
				return minus(so.getOperand(0).findConstantIn(sv));
			} else if (so.isOperation(Operation.PLUS)) {
				StepExpression found = null;
				for (StepExpression operand : so) {
					found = add(found, operand.findConstantIn(sv));
				}
				return found;
			} else if (so.isOperation(Operation.DIVIDE) && so.getOperand(1).isConstant()) {
				StepExpression nominator = so.getOperand(0).findConstantIn(sv);

				if (nominator != null) {
					return divide(nominator, so.getOperand(1));
				}
			}
		}

		return null;
	}


	public int countNonConstOperation(Operation operation, StepVariable variable) {
		if (this instanceof StepOperation) {
			StepOperation so = (StepOperation) this;

			if (so.isOperation(operation) && !so.isConstantIn(variable)) {
				return 1;
			}

			int operations = 0;
			for (StepExpression operand : so) {
				operations += operand.countNonConstOperation(operation, variable);
			}
			return operations;
		}

		return 0;
	}

	public int countOperation(Operation operation) {
		if (this instanceof StepOperation) {
			StepOperation so = (StepOperation) this;

			if (so.isOperation(operation)) {
				return 1;
			}

			int operations = 0;
			for (StepExpression operand : so) {
				operations += operand.countOperation(operation);
			}
			return operations;
		}

		return 0;
	}

	public boolean containsSquareRoot() {
		if (isSquareRoot()) {
			return true;
		}
		if (isOperation(Operation.MULTIPLY) || isOperation(Operation.MINUS)) {
			for (StepExpression operand : (StepOperation) this) {
				if (operand.containsSquareRoot()) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean integerCoefficients(StepVariable sv) {
		if (degree(sv) >= 0) {
			StepExpression[] coefficients = convertToPolynomial(sv);

			for (StepExpression coefficient : coefficients) {
				if (coefficient != null && !coefficient.isInteger()) {
					return false;
				}
			}

			return true;
		}

		return false;
	}

	/**
	 * A square is an expression of the form a^(2n), or a nonSpecialConstant. This
	 * definition is useful for factoring
	 * 
	 * @return whether the current node is a square
	 */
	public boolean isSquare() {
		return nonSpecialConstant() || isOperation(Operation.POWER) && isEven(((StepOperation) this).getOperand(1));
	}

	public boolean isCube() {
		return isOperation(Operation.MINUS) && ((StepOperation) this).getOperand(0).isCube() || nonSpecialConstant()
				|| isOperation(Operation.POWER) && isEqual(((StepOperation) this).getOperand(1), 3);
	}

	/**
	 * Only if isSquare() is true!
	 * 
	 * @return the square root of the expression
	 */
	public StepExpression getSquareRoot() {
		if (isSquare()) {
			if (nonSpecialConstant()) {
				if (isEqual(Math.sqrt(getValue()), Math.floor(Math.sqrt(getValue())))) {
					return StepConstant.create(Math.sqrt(getValue()));
				}
				return root(this, 2);
			}

			StepOperation so = (StepOperation) this;
			return nonTrivialPower(so.getOperand(0), so.getOperand(1).getValue() / 2);
		}

		return null;
	}

	public StepExpression getCubeRoot() {
		if (isCube()) {
			if (nonSpecialConstant()) {
				if (isEqual(Math.cbrt(getValue()), Math.floor(Math.cbrt(getValue())))) {
					return StepConstant.create(Math.cbrt(getValue()));
				}
				return root(this, 3);
			}

			StepOperation so = (StepOperation) this;

			if (isOperation(Operation.MINUS)) {
				return so.getOperand(0).getCubeRoot().negate();
			}

			return nonTrivialPower(so.getOperand(0), so.getOperand(1).getValue() / 3);
		}

		return null;
	}

	/**
	 * @return whether the current node is a trigonometric function
	 */
	public boolean isTrigonometric() {
		return isOperation(Operation.SIN) || isOperation(Operation.COS) || isOperation(Operation.TAN)
				|| isOperation(Operation.CSC) || isOperation(Operation.SEC) || isOperation(Operation.CSC);
	}

	/**
	 * @return whether the current node is an inverse trigonometric function
	 */
	public boolean isInverseTrigonometric() {
		return isOperation(Operation.ARCSIN) || isOperation(Operation.ARCCOS) || isOperation(Operation.ARCTAN);
	}

	public boolean isNaturalLog() {
		return isOperation(Operation.LOG) && ((StepOperation) this).getOperand(0).equals(StepConstant.E);
	}

	public boolean isFraction() {
		return isOperation(Operation.DIVIDE) ||
				isOperation(Operation.MINUS) && ((StepOperation) this).getOperand(0).isFraction();
	}

	/**
	 * @return the tree, regrouped (destroys the tree, use only in assignments)
	 */
	public StepExpression regroup() {
		return regroup(null);
	}

	/**
	 * @param sb
	 *            SolutionBuilder for the regroup steps
	 * @return the tree, regrouped (destroys the tree, use only in assignments)
	 */
	public StepExpression regroup(SolutionBuilder sb) {
		if (this instanceof StepOperation) {
			return (StepExpression) StepStrategies.defaultRegroup(this, sb);
		}

		return this;
	}

	public StepExpression regroupOutput(SolutionBuilder sb) {
		sb.add(SolutionStepType.SIMPLIFY, this);
		return regroup(sb);
	}

	/**
	 * @return the tree, regrouped and expanded (destroys the tree, use only in
	 *         assignments)
	 */
	public StepExpression expand() {
		return expand(null);
	}

	/**
	 * @param sb
	 *            SolutionBuilder for the expansion steps
	 * @return the tree, regrouped and expanded (destroys the tree, use only in
	 *         assignments)
	 */
	public StepExpression expand(SolutionBuilder sb) {
		if (this instanceof StepOperation) {
			return (StepExpression) StepStrategies.defaultExpand(this, sb);
		}

		return this;
	}

	public StepExpression expandOutput(SolutionBuilder sb) {
		sb.add(SolutionStepType.EXPAND, this);
		return expand(sb);
	}

	/**
	 * @return the tree, factored (destroys the tree, use only in assignments)
	 */
	public StepExpression factor() {
		return factor(null);
	}

	/**
	 * @param sb
	 *            SolutionBuilder for the factoring steps
	 * @return the tree, factored (destroys the tree, use only in assignments)
	 */
	public StepExpression factor(SolutionBuilder sb) {
		if (this instanceof StepOperation) {
			return (StepExpression) StepStrategies.defaultFactor(this, sb, new RegroupTracker());
		}

		return this;
	}

	public StepExpression factorOutput(SolutionBuilder sb) {
		sb.add(SolutionStepType.FACTOR, this);
		return factor(sb);
	}

	public StepExpression differentiate() {
		return differentiate(null);
	}

	public StepExpression differentiate(SolutionBuilder sb) {
		if (this instanceof StepOperation) {
			return (StepExpression) StepStrategies.defaultDifferentiate(this, sb);
		}

		return this;
	}

	public StepExpression differentiateOutput(SolutionBuilder sb) {
		sb.add(SolutionStepType.DIFFERENTIATE, this);
		return differentiate(sb);
	}

	/**
	 * @param from
	 *            StepNode to replace
	 * @param to
	 *            StepNode to replace with
	 * @return the tree, replaced
	 */
	public StepExpression replace(StepExpression from, StepExpression to) {
		if (equals(from)) {
			return to.deepCopy();
		}
		if (this instanceof StepOperation) {
			StepOperation so = new StepOperation(((StepOperation) this).getOperation());
			for (StepExpression operand : (StepOperation) this) {
				so.addOperand(operand.replace(from, to));
			}
			return so;
		}
		return deepCopy();
	}

	/**
	 * @param var
	 *            variable to group in
	 * @return toConvert in a polynomial format (as an array of coefficients)
	 *         toConvert = sum(returned[i] * var^i)
	 */
	public StepExpression[] convertToPolynomial(StepVariable var) {
		StepExpression[] poli = new StepExpression[degree(var) + 1];

		poli[0] = findConstantIn(var);

		for (int pow = 1; pow <= degree(var); pow++) {
			poli[pow] = findCoefficient(pow == 1 ? var : power(var, pow));
		}

		return poli;
	}

	public StepExpression getDenominator() {
		if (isOperation(Operation.MINUS)) {
			return ((StepOperation) this).getOperand(0).getDenominator();
		} else if (isOperation(Operation.DIVIDE)) {
			return ((StepOperation) this).getOperand(1);
		}

		return null;
	}

	/**
	 * @return the denominator of the tree, if it's an integer. 0 otherwise
	 */
	public long getConstantDenominator() {
		if (isOperation(Operation.MINUS)) {
			return ((StepOperation) this).getOperand(0).getConstantDenominator();
		} else if (isOperation(Operation.DIVIDE)) {
			if (closeToAnInteger(((StepOperation) this).getOperand(1))) {
				return Math.round(((StepOperation) this).getOperand(1).getValue());
			}
		} else if (isConstant()) {
			return 1;
		}

		return 0;
	}

	/**
	 * @return the numerator of the tree.
	 */
	public StepExpression getNumerator() {
		if (isOperation(Operation.MINUS)) {
			return minus(((StepOperation) this).getOperand(0).getNumerator());
		} else if (isOperation(Operation.DIVIDE)) {
			return ((StepOperation) this).getOperand(0);
		}
		return this;
	}

	/**
	 * Returns the value of arcsin(a), arccos(a) and arctg(a) for simple a-s
	 * 
	 * @param so inverse trigonometric expression (such as arccos(1))
	 * @return the value, if it can be evaluated, null otherwise
	 */
	public static StepExpression inverseTrigoLookup(StepOperation so) {
		String[] arguments = new String[] { "-1", "-(nroot(3, 2))/(2)", "-(nroot(2, 2))/(2)", "-(1)/(2)", "0",
				"(1)/(2)", "(nroot(2, 2))/(2)", "(nroot(3, 2))/(2)", "1" };
		String[] argumentsTan = new String[] { "", "-nroot(3, 2)", "-1", "-nroot(3, 2)/3", "0", "nroot(3, 2)/3", "1",
				"nroot(3, 2)", "" };

		StepExpression pi = StepConstant.create(Math.PI);
		StepExpression[] valuesSinTan = new StepExpression[] { minus(divide(pi, 2)), minus(divide(pi, 3)),
				minus(divide(pi, 4)), minus(divide(pi, 6)), StepConstant.create(0), divide(pi, 6), divide(pi, 4),
				divide(pi, 3), divide(pi, 2) };
		StepExpression[] valuesCos = new StepExpression[] { pi, divide(multiply(5, pi), 6), divide(multiply(3, pi), 4),
				divide(multiply(2, pi), 3), divide(pi, 2), divide(pi, 3), divide(pi, 4), divide(pi, 6),
				StepConstant.create(0) };

		String currentArgument = so.getOperand(0).toString();
		for (int i = 0; i < arguments.length; i++) {
			if (currentArgument.equals(arguments[i])) {
				if (so.isOperation(Operation.ARCSIN)) {
					return valuesSinTan[i];
				} else if (so.isOperation(Operation.ARCCOS)) {
					return valuesCos[i];
				}
			} else if (currentArgument.equals(argumentsTan[i])) {
				if (so.isOperation(Operation.ARCTAN)) {
					return valuesSinTan[i];
				}
			}
		}

		return null;
	}

	public static void getBasesAndExponents(StepExpression sn, StepExpression currentExp, List<StepExpression> bases,
			List<StepExpression> exponents) {
		if (sn instanceof StepOperation) {
			StepOperation so = (StepOperation) sn;

			switch (so.getOperation()) {
			case MULTIPLY:
				for (int i = 0; i < so.noOfOperands(); i++) {
					getBasesAndExponents(so.getOperand(i), currentExp, bases, exponents);
				}
				return;
			case MINUS:
				if (!so.getOperand(0).nonSpecialConstant()) {
					bases.add(StepConstant.create(-1));
					exponents.add(StepConstant.create(1));
					getBasesAndExponents(so.getOperand(0), currentExp, bases, exponents);
					return;
				}
				break;
			case DIVIDE:
				getBasesAndExponents(so.getOperand(0), currentExp, bases, exponents);
				getBasesAndExponents(so.getOperand(1), multiply(-1, currentExp), bases, exponents);
				return;
			case POWER:
				bases.add(so.getOperand(0));
				exponents.add(multiply(currentExp, so.getOperand(1)));
				return;
			}
		}

		if (sn != null) {
			bases.add(sn);
			exponents.add(currentExp == null ? StepConstant.create(1) : currentExp);
		}
	}

	public StepExpression reciprocate() {
		if (isOperation(Operation.DIVIDE)) {
			if (isEqual(((StepOperation) this).getOperand(0), 1)) {
				return ((StepOperation) this).getOperand(1);
			}
			return divide(((StepOperation) this).getOperand(1), ((StepOperation) this).getOperand(0));
		} else if (isEqual(this, 1) || isEqual(this, -1)) {
			return this;
		} else {
			return divide(StepConstant.create(1), this);
		}
	}

	/**
	 * calculates currentFraction * base ^ exponent, and writes it in a nice form
	 * i.e.: makeFraction((x+1)/x, x+1, 1) -> ((x+1)(x+1))/x,
	 * makeFraction((x+1)/(x(x+1)), x, -1) -> (x+1)/(x(x+1)x)
	 */
	public static StepExpression makeFraction(StepExpression currentFraction, StepExpression base,
			StepExpression exponent) {
		StepExpression nominator;
		StepExpression denominator;

		if (currentFraction != null && currentFraction.isOperation(Operation.DIVIDE)) {
			nominator = ((StepOperation) currentFraction).getOperand(0);
			denominator = ((StepOperation) currentFraction).getOperand(1);
		} else {
			nominator = currentFraction;
			denominator = null;
		}

		if (!exponent.canBeEvaluated() || exponent.getValue() >= 0) {
			if (!isEqual(exponent.getValue(), 1) && closeToAnInteger(1 / exponent.getValue())) {
				nominator = nonTrivialProduct(nominator, root(base, 1 / exponent.getValue()));
			} else {
				nominator = nonTrivialProduct(nominator, nonTrivialPower(base, exponent));
			}
		} else {
			if (!isEqual(exponent.getValue(), -1) && closeToAnInteger(1 / exponent.getValue())) {
				nominator = nonTrivialProduct(denominator, root(base, -1 / exponent.getValue()));
			} else {
				denominator = nonTrivialProduct(denominator, nonTrivialPower(base, exponent.negate()));
			}
		}

		return divide(nominator, denominator);
	}

	/**
	 * returns a^b, except if b == 1, then it returns a, or if b == 0, then it
	 * returns 1
	 */
	public static StepExpression nonTrivialPower(StepExpression a, StepExpression b) {
		if (a != null && b != null) {
			if (isEqual(b, 1)) {
				return a;
			} else if (isEqual(b, 0)) {
				return StepConstant.create(1);
			}
		}

		return power(a, b);
	}

	public static StepExpression nonTrivialPower(StepExpression a, double b) {
		return nonTrivialPower(a, StepConstant.create(b));
	}

	/**
	 * return a*b, except if: a == 1 -> b, a == -1 -> -b and vice versa
	 */
	public static StepExpression nonTrivialProduct(StepExpression a, StepExpression b) {
		if (a != null && b != null) {
			if (isEqual(a, 1)) {
				return b;
			} else if (isEqual(a, -1)) {
				return minus(b);
			} else if (isEqual(b, 1)) {
				return a;
			} else if (isEqual(b, -1)) {
				return minus(a);
			}
		}

		return multiply(a, b);
	}

	public static StepExpression nonTrivialProduct(double a, StepExpression b) {
		return nonTrivialProduct(StepConstant.create(a), b);
	}

	/**
	 * Tries to negate the expression. Basically, it removes the starting minus, if
	 * there is one.
	 * @return -this
	 */
	public StepExpression negate() {
		if (isOperation(Operation.MINUS)) {
			return ((StepOperation) this).getOperand(0);
		}
		if (isOperation(Operation.MULTIPLY) && ((StepOperation) this).getOperand(0).isNegative()) {
			StepExpression so = null;
			if (!isEqual(((StepOperation) this).getOperand(0), -1)) {
				so = ((StepOperation) this).getOperand(0).negate();
			}
			for (int i = 1; i < ((StepOperation) this).noOfOperands(); i++) {
				so = multiply(so, ((StepOperation) this).getOperand(i));
			}
			return so;
		}
		if (isOperation(Operation.DIVIDE)) {
			if (((StepOperation) this).getOperand(0).isNegative()) {
				return divide(((StepOperation) this).getOperand(0).negate(), ((StepOperation) this).getOperand(1));
			}
			return minus(this);
		}

		return minus(this);
	}

	public boolean isNegative() {
		return isOperation(Operation.MINUS) ||
				isOperation(Operation.MULTIPLY) && ((StepOperation) this).getOperand(0).isNegative();
	}

	public static Operation getInverse(Operation op) {
		switch (op) {
		case SIN:
			return Operation.ARCSIN;
		case COS:
			return Operation.ARCCOS;
		case TAN:
			return Operation.ARCTAN;
		default:
			return Operation.NO_OPERATION;
		}
	}

}
