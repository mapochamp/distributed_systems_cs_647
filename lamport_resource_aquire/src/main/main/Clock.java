package main;

public interface Clock<T> {
    T increment();

    T messageReceived(T messageTimeStamp);
}
