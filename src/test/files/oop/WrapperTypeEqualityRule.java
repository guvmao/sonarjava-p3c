public class WrapperTypeEqualityRuleExample {
    public void check() {
        Integer a = 235;
        Integer b = 235;
        if (a == b) { // Noncompliant {{【a】应该作为equals的参数，而不是调用方}}
            return;
        }
        if (a.equals(b)) { // Compliant
            return;
        }
        if (a != 0) { // Compliant - 0 is in cache range [-128, 127]
            return;
        }
        if (a == 127) { // Compliant - 127 is in cache range
            return;
        }
        if (a == 128) { // Noncompliant {{【a】应该作为equals的参数，而不是调用方}}
            return;
        }
        if (a == -129) { // Noncompliant {{【a】应该作为equals的参数，而不是调用方}}
            return;
        }
    }
}