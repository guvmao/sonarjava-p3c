package oop;

import java.util.Objects;

public class EqualsAvoidNullRuleExample {

    private static final String LOCAL_CONSTANT = "expectedValue";

    public void check(String input) {
        boolean result = "expectedValue".equals(input); // Compliant
        if (result) {
            System.out.println("Input matches the expected value.");
        }
    }

    public void check2(String input) {
        boolean result = input.equals("expectedValue"); // Noncompliant {{【input】应该作为equals的参数，而不是调用方}}
        if (result) {
            System.out.println("Input matches the expected value.");
        }
    }

    public void check3(String input) {
        boolean result = LOCAL_CONSTANT.equals(input); // Compliant
        if (result) {
            System.out.println("Input matches the expected value.");
        }
    }

    public void check4(String input) {
        boolean result = Constants.EXPECTED_VALUE.equals(input); // Compliant
        if (result) {
            System.out.println("Input matches the expected value.");
        }
    }

    public void check5(String input) {
        boolean result = StringConstants.STRING_ONE.equals(input); // Compliant
        if (result) {
            System.out.println("Input matches the expected value.");
        }
    }

    public void check6(String left, String right) {
        boolean result = Objects.equals(left, right); // Compliant
        if (result) {
            System.out.println("Input matches the expected value.");
        }
    }

    public void check7(RateQueryDTO rateQueryDTO) {
        boolean result = rateQueryDTO.getToCurrency().equals(Constants.EXPECTED_VALUE); // Noncompliant {{【rateQueryDTO.getToCurrency()】应该作为equals的参数，而不是调用方}}
        if (result) {
            System.out.println("Input matches the expected value.");
        }
    }
}

class Constants {

    static final String EXPECTED_VALUE = "expectedValue";
}

class RateQueryDTO {

    String getToCurrency() {
        return "CNY";
    }
}
