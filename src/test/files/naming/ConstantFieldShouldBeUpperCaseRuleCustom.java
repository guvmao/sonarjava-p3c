class ConstantFieldShouldBeUpperCaseRuleCustom {
    private static final long serialVersionUID = 1L; // Compliant
    private static final String log = "test"; // Compliant
    private static final String maxStockCount = "value"; // Noncompliant {{常量【maxStockCount】命名应全部大写并以下划线分隔}}
    private static final String MAX_STOCK_COUNT = "value"; // Compliant
}
