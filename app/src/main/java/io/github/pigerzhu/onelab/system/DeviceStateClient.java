package io.github.pigerzhu.onelab.system;

public final class DeviceStateClient {
    public static final int STATE_UNKNOWN = -1;
    public static final int STATE_OUTER_DEFAULT = 5;

    private static final String COMMAND = "cmd device_state ";

    public boolean supportsOuterDefault() {
        String states = Shell.runSuForOutput(COMMAND + "print-states-simple");
        if (states == null) {
            return false;
        }
        for (String value : states.split(",")) {
            if (String.valueOf(STATE_OUTER_DEFAULT).equals(value.trim())) {
                return true;
            }
        }
        return false;
    }

    public int currentState() {
        String value = Shell.runSuForOutput(COMMAND + "print-state");
        if (value == null) {
            return STATE_UNKNOWN;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return STATE_UNKNOWN;
        }
    }

    public int setOuterDefault(boolean enabled) {
        String operation = enabled ? "state " + STATE_OUTER_DEFAULT : "state reset";
        if (!Shell.runSu(COMMAND + operation)) {
            return STATE_UNKNOWN;
        }
        int state = waitForState(enabled);
        if (!enabled && state != STATE_UNKNOWN && state != STATE_OUTER_DEFAULT) {
            // Resetting swaps the physical panels back, but the current task can remain on
            // logical display 1. Starting its existing activity on display 0 reparents the task.
            Shell.runSu("am start --display 0 -n io.github.pigerzhu.onelab/.MainActivity");
        }
        return state;
    }

    private int waitForState(boolean outerExpected) {
        int state = STATE_UNKNOWN;
        for (int attempt = 0; attempt < 10; attempt++) {
            state = currentState();
            if ((outerExpected && state == STATE_OUTER_DEFAULT)
                    || (!outerExpected && state != STATE_UNKNOWN
                    && state != STATE_OUTER_DEFAULT)) {
                return state;
            }
            try {
                Thread.sleep(200L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return STATE_UNKNOWN;
            }
        }
        return state;
    }
}
