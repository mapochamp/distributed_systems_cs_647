package main;

/*
    Question list:
    (Please consider that I have terminal C/C++ brain)
    1. How do these java file structures work?
    2. How do I get rid of these errors of the ide not thinking I have the java packages
    3. How should I start thinking about creating a list of all the actors and passing
    the list of ref's to all the actors?
 */

public interface Clock<T> {
    T increment();

    T messageReceived(T messageTimeStamp);
}
