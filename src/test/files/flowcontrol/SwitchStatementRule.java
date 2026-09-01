package flowcontrol;

class SwitchStatementRule {
    public int check(int number) {
        switch (number) { // Compliant
            case 1:
                number = number + 10;
                break;
            case 2:
                number = number + 100;
                break;
            default:
        }
        return number;
    }

    public int check2(int number) {
        switch (number) { // Noncompliant {{switch块缺少default语句}}
            case 1:
                number = number + 10;
                break;
            case 2:
                number = number + 100;
                break;
        }
        return number;
    }

    public int check3(int number) {
        switch (number) {
            case 1:
                number = number + 10;
                break;
            case 2: // Noncompliant {{switch中每个case需要通过break/return等来终止}}
                number = number + 100;
            default:
        }
        return number;
    }

    public void checkDefaultAfterMultipleCases(Status status) {
        switch (status) { // Compliant
            case IMPLEMENT_PLAN:
                deleteByProposalId();
                break;
            case APPLICANT:
            case ADMIN_CHECK:
            default:
                break;
        }
    }

    public int checkCaseBlockWithReturn(Status status) {
        switch (status) {
            case IMPLEMENT_PLAN: {
                deleteByProposalId();
                return 1;
            }
            case APPLICANT: {
                return 2;
            }
            case ADMIN_CHECK:
            default:
                return 0;
        }
    }

    public int checkNestedSwitchWithReturn(int quarterIndex, int metricIndex) {
        switch (quarterIndex) { // Compliant
            case 0:
                switch (metricIndex) { // Compliant
                    case 0:
                        return 1;
                    default:
                        return 0;
                }
            case 1:
                return 2;
            default:
                return 3;
        }
    }

    public void checkNestedSwitchWithBreak(int outerValue, int innerValue) {
        switch (outerValue) {
            case 0: // Noncompliant {{switch中每个case需要通过break/return等来终止}}
                switch (innerValue) {
                    case 0:
                        break;
                    default:
                        break;
                }
            case 1:
            default:
                break;
        }
    }

    private void deleteByProposalId() {
    }

    enum Status {
        IMPLEMENT_PLAN,
        APPLICANT,
        ADMIN_CHECK
    }
}