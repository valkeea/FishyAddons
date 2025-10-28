package me.valkeea.fishyaddons.event;

public abstract class BaseEvent {
    private boolean consumed = false;
    private Object result = null;

    public boolean isConsumed() {
        return consumed;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    @SuppressWarnings("unchecked")
    public <T> T getResult() {
        return (T) result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
