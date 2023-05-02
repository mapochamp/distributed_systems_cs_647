package lamport_tyj27;

/*
    Question list:
    (Please consider that I have terminal C/C++ brain)
    1. How do these java file structures work?
     -> import as an sbt and it'll handle it

    2. How do I get rid of these errors of the ide not thinking I have the java packages
     -> open as an sbt project
    3. When trying to aquire or release a resource, should i try to do that in a behavior? and do it on a message? or what
        -> message
    4. do we update clock when sending all request, release, and ack messages?
        -> yes and yes for receiving
    5. Request queue data strcuture: map? list? you can't actually serach a queue can you?
        -> just use a list
 */

public interface Clock<T> {
    T increment();

    T messageReceived(T messageTimeStamp);
    T getCurrentTimestamp();
}
