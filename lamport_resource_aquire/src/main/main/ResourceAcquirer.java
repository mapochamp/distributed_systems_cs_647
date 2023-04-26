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

public class ResourceAcquirer extends AbstractBehavior<ResourceMessage> {
    public static Behavior<ResourceMessage> create() {
        return Behaviors.setup(context -> {
            return new ResourceAcquirer(context);
        });
    }

    private List<ActorRef<ResourceMessage>> acquirerList;
    private ClockInt integerClock;

    private ResourceAcquirer(ActorContext ctxt) {
        super(ctxt);
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
            // TODO: figure out what to do once we request and implment the aquireing details of the mutex
            case ResourceMessage.Request r:
                getContext().getLog().info("[ResourceAcquirer] requesting echo of: "+r.msg());
                echo.tell(new EchoRequest.Echo(r.msg(), getContext().getSelf()));
                break;
            case ResourceMessage.Repeat r:
                getContext().getLog().info("[ResourceAcquirer] requesting repeat of last message");
                echo.tell(new EchoRequest.Repeat(getContext().getSelf()));
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