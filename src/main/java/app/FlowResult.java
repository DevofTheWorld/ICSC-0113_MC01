package app;
//
public record FlowResult(boolean error, boolean completed, String message, String nextPrompt) {

    public static FlowResult error(String message) {
        return new FlowResult(true, false, message, null);
    }

    public static FlowResult next(String nextPrompt) {
        return new FlowResult(false, false, null, nextPrompt);
    }

    public static FlowResult completed(String nextPrompt) {
        return new FlowResult(false, true, null, nextPrompt);
    }
}
