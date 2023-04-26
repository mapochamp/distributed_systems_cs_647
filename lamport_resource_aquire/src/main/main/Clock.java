package main;

/*
    Question list:
    (Please consider that I have terminal C/C++ brain)
    1. How do these java file structures work?
    2. How do I get rid of these errors of the ide not thinking I have the java packages
    the list of ref's to all the actors?
    3. When trying to aquire or release a resource, should i try to do that in a behavior? and do it on a message? or what
    4. do we update clock when sending all request, release, and ack messages?
    5. Request queue data strcuture: map? list? you can't actually serach a queue can you?
 */

public interface Clock<T> {
    T increment();

    T messageReceived(T messageTimeStamp);
}
