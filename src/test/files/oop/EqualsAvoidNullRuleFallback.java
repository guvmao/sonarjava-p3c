package oop;

public class EqualsAvoidNullRuleFallback {

    public void check(String input) {
        boolean result = StringConstants.STRING_ONE.equals(input); // Compliant
        if (result) {
            System.out.println("Input matches the expected value.");
        }
    }

    public void check2(String input) {
        boolean result = UnknownConstants.STRING_TWO.equals(input); // Noncompliant {{【UnknownConstants.STRING_TWO】应该作为equals的参数，而不是调用方}}
        if (result) {
            System.out.println("Input matches the expected value.");
        }
    }
}
