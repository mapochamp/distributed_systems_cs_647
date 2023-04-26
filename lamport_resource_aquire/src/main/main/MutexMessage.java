package main;

public interface MutexMessage {
    public final record Lock(ActorRef<ResourceMessage> sender) implements MutexMessage {}
    public final record Release(ActorRef<ResourceMessage> sender) implements MutexMessage {}
    public final record End() implements MutexMessage {}
}
