package jack.fluids.slow;

import java.util.List;

public class LinearEquation {
  private final List<Term> terms;
  private final double rhs;

  public LinearEquation(List<Term> terms, double rhs) {
    this.terms = terms;
    this.rhs = rhs;
  }

  public static class Term {
    private final String variable;
    private final double coef;

    public Term(String variable, double coef) {
      this.variable = variable;
      this.coef = coef;
    }
  }
}
