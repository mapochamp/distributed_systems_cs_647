package lamport_tyj27;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public interface MutexMessage {
    public final record Lock(ActorRef<ResourceMessage> sender) implements MutexMessage {}
    public final record Release(ActorRef<ResourceMessage> sender) implements MutexMessage {}
    public final record End() implements MutexMessage {}
}
