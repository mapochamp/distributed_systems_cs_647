package main;

public interface ResourceMessage {
    public final record Ack(int timeStamp, ActorRef<ResourceMessage> sender, int senderId) implements ResourceMessage {}
    public final record Request(int timeStamp, ActorRef<ResourceMessage> sender, int senderId) implements ResourceMessage {}
    public final record Release(int timeStamp, ActorRef<ResourceMessage> sender, int senderId) implements ResourceMessage {}
    public final record End() implements ResourceMessage {}
}
