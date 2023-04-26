package main;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import edu.drexel.cs647.java.EchoRequest.Echo;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.ActorRef;

import java.util.AbstractMap;
import java.util.PriorityQueue;

public class ResourceAcquirer extends AbstractBehavior<ResourceMessage> {
    public static Behavior<ResourceMessage> create() {
        return Behaviors.setup(context -> {
            return new ResourceAcquirer(context);
        });
    }

    private List<ActorRef<ResourceMessage>> actorRefList;
    // actor id
    private int myId;
    private ClockInt integerClock;
    private PriorityQueue<AbstractMap.SimpleEntry<Integer, Integer>> requestQueue;

    private ResourceAcquirer(ActorContext ctxt, int myId) {
        super(ctxt);
        this.myId = myId;
    }

    public setListofActorRefs(List<ActorRef<ResourceMessage>> actorRefList) {
        this.actorRefList = actorRefList;
    }

    @Override
    public Receive<ResourceMessage> createReceive() {
        // This method is only called once for initial setup
        return newReceiveBuilder()
                // We could register multiple onMessage handlers, for each subclass of ProxyMessage, if we wanted to.
                // By using a single handler for the general message type, it makes it easier to switch handling of all message types simultaneously (in a later project)
                .onMessage(ResourceMessage.class, this::dispatch)
                .build();
    }

    public Behavior<ResourceMessage> dispatch(ResourceMessage msg) {
        switch (msg) {
            case ResourceMessage.Request r:
                getContext().getLog().info("[ResourceAcquirer] requesting lock: "+integerClock.getCurrentTimestamp());
                // TODO: how to sort on the timestamp?
                requestQueue.add(new AbstractMap.SimpleEntry<>(r.timeStamp(), r.senderId()));
                integerClock.messageReceived(r.timeStamp());
                r.sender().tell(new ResourceMessage.Ack(integerClock.getCurrentTimestamp(r.timeStamp()), getContext().getSelf(), myId));
                break;
            case ResourceMessage.Release r:
                // TODO: move this stuff to the behavior when we actually release
                /*
                getContext().getLog().info("[ResourceAcquirer] Releasing Lock");
                for(ActorRef<ResourceMessage> actorRef : actorRefList) {
                    actorRef.tell(new ResourceMessage.Release(integerClock.getCurrentTimestamp(), getContext().getSelf(), myId));
                }
                 */
                // TODO: remove all the requests from the queu of the id
                break;
            case ResourceMessage.Ack a:
                getContext().getLog().info("[ResourceAcquirer] heard back: "+a.msg());
                break;
            case ResourceMessage.End s:
                getContext().getLog().info("[ResourceAcquirer] shutting down");
                return Behaviors.stopped();
        }
        // Keep the same message handling behavior
        return this;
    }
}