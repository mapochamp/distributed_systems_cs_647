package main;

public class ClockInt implements Clock<Integer> {
    private int timeStamp;
    @Override
    public Integer increment() {
        timeStamp++;
        return timeStamp;
    }

    @Override
    public Integer messageReceived(Integer messageTimeStamp) {
        timeStamp = Math.max(messageTimeStamp + 1, timeStamp);
        return timeStamp;
    }
}