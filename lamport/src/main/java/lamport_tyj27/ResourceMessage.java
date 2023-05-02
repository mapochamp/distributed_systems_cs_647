package lamport_tyj27;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.ActorRef;

import java.util.*;

public interface ResourceMessage {
    public final record Ack(int timeStamp, ActorRef<ResourceMessage> sender, int senderId) implements ResourceMessage {}
    public final record Request(int timeStamp, ActorRef<ResourceMessage> sender, int senderId) implements ResourceMessage {}
    public final record Release(int timeStamp, ActorRef<ResourceMessage> sender, int senderId) implements ResourceMessage {}
    public final record LockRequestAck(ActorRef<MutexMessage> sender) implements ResourceMessage {}
    public final record LockRequestReject(ActorRef<MutexMessage> sender) implements ResourceMessage {}
    public final record LockReleaseAck(ActorRef<MutexMessage> sender) implements ResourceMessage {}
    public final record LockReleaseReject(ActorRef<MutexMessage> sender) implements ResourceMessage {}
    public final record Init(ActorRef<ResourceMessage> sender) implements ResourceMessage {}
    public final record InitKickOff() implements ResourceMessage {}
    public final record InitActorRefList(List<ActorRef<ResourceMessage>> actorRefList) implements ResourceMessage {}
    public final record End() implements ResourceMessage {}
}
