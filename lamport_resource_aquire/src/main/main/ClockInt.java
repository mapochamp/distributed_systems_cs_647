package main;

public final class ClockInt implements Clock<Integer> {
    private int timeStamp;

    public ClockInt() { this.timeStamp = 0; }
    @Override
    public Integer increment() {
        timeStamp++;
        return timeStamp;
    }

    @Override
    public int messageReceived(Integer messageTimeStamp) {
        timeStamp = Math.max(messageTimeStamp + 1, timeStamp);
        return timeStamp;
    }

    public int getCurrentTimestamp() {
        return timeStamp;
    }
}