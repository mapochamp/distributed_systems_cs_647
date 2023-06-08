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
import java.util.*;


public interface ClientRPC {
    public final record temp() implements ClientRPC {}
    public final record End() implements ClientRPC {}
    public final record Timeout() implements ClientRPC {}
    public final record Init() implements ClientRPC {}
    public final record RequestAck() implements ClientRPC {}
    public final record UnstableReadRequest() implements ClientRPC {}
    public final record StableReadRequest() implements ClientRPC {}
    public final record UnstableReadRequestResult(int state, boolean isEmpty) implements ClientRPC {}
    public final record StableReadRequestResult(int state, boolean isEmpty, boolean success, ActorRef<ServerRPC> leader) implements ClientRPC {}
    public final record RequestReject(ActorRef<ServerRPC> leader, int entry, boolean success) implements ClientRPC {}
    public final record PingServerResponse() implements ClientRPC {}
}
