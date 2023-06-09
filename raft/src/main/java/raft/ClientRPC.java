package raft;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.TimerScheduler;


public interface ClientRPC {
    public final record temp() implements ClientRPC {}
    public final record End() implements ClientRPC {}
    public final record Timeout() implements ClientRPC {}
    public final record Init() implements ClientRPC {}
    public final record RequestAck() implements ClientRPC {}
    public final record RequestReject(ActorRef<ServerRPC> leader, int entry) implements ClientRPC {}
}
