package flowcontrol;

class IfNestingRule {
    public void compliant(int value) {
        if (value > 0) { // Compliant
            if (value > 1) { // Compliant
                if (value > 2) { // Compliant
                    value++;
                }
            }
        }
    }

    public void tooDeep(int value) {
        if (value > 0) { // Compliant
            if (value > 1) { // Compliant
                if (value > 2) { // Compliant
                    if (value > 3) { // Noncompliant {{if语句嵌套超过3层，请降低嵌套层数}}
                        if (value > 4) { // Compliant
                            value++;
                        }
                    }
                }
            }
        }
    }

    public void siblingIfs(int value) {
        if (value > 0) { // Compliant
            if (value > 1) { // Compliant
                value++;
            }
        }
        if (value > 2) { // Compliant
            if (value > 3) { // Compliant
                value++;
            }
        }
    }

    public void nestedThroughLoop(int value) {
        if (value > 0) { // Compliant
            for (int i = 0; i < value; i++) {
                if (value > 1) { // Compliant
                    while (value > 2) {
                        if (value > 3) { // Compliant
                            if (value > 4) { // Noncompliant {{if语句嵌套超过3层，请降低嵌套层数}}
                                value++;
                            }
                        }
                    }
                }
            }
        }
    }

    public void elseIfChain(int value) {
        if (value == 0) { // Compliant
            value++;
        } else if (value == 1) { // Compliant
            value++;
        } else if (value == 2) { // Compliant
            value++;
        } else if (value == 3) { // Noncompliant {{if语句嵌套超过3层，请降低嵌套层数}}
            value++;
        } else if (value == 4) { // Compliant
            value++;
        }
    }

    public void lambdaStartsAtOne(int value) {
        if (value > 0) { // Compliant
            Runnable runnable = () -> {
                if (value > 1) { // Compliant
                    if (value > 2) { // Compliant
                        if (value > 3) { // Compliant
                            value++;
                        }
                    }
                }
            };
            runnable.run();
        }
    }

    public void methodStartsAtOne(int value) {
        if (value > 0) { // Compliant
            value++;
        }
    }
}
