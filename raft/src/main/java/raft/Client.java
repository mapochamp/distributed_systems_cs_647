package raft;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import java.util.*;
import java.util.concurrent.TimeUnit;



public class Client extends AbstractBehavior<ClientRPC> {
    int id;
    public static Behavior<ClientRPC> create(int id) {
        return Behaviors.setup(context -> {
            return new Client(context, id);
        });
    }

    private Client(ActorContext ctxt, int id) {
        super(ctxt);
        this.id = id;
    }
    @Override
    public Receive<ClientRPC> createReceive() {
        // This method is only called once for initial setup
        return newReceiveBuilder()
                // We could register multiple onMessage handlers, for each subclass of EchoRequest, if we wanted to.
                // By using a single handler for the general message type, it makes it easier to switch handling of all message types simultaneously (in a later project)
                .onMessage(ClientRPC.class, this::dispatch)
                .build();
    }

    public Behavior<ClientRPC> dispatch(ClientRPC msg) {
        // This style of switch statement is technically a preview feature in many versions of Java, so you'll need to compile with --enable-preview
        switch (msg) {
            case ClientRPC.temp l:
                getContext().getLog().info("temp");
                break;
            case ClientRPC.End e:
                return Behaviors.stopped();
            default:
                return Behaviors.stopped();
        }
        // Keep the same message handling behavior
        return this;
    }

}
