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

public class ResourceAcquirer extends AbstractBehavior<ResourceAcquirer.Command> {
    inteface Command{};
    // Message definitions
    public static class receiveListOfActors implements Command {
    }

    public static class resourceAck implements Command {
        public final int messageTimeStamp;

        public resourceAck(int messageTimeStamp) {
            this.messageTimeStamp = messageTimeStamp;
        }
    }

    public static class resourceRequest implements Command {
    }

    // TODO: replace command with Message class or interface
    // TODO: merge the callbaks into a single switch like in the example

    private ClockInt localClock = new ClockInt();

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new ResourceAcquirer(context));
    }
    // Default Constructor
    private ResourceAcquirer(ActorContext<Command> context) {
        super(context);
    }

    // State of Actor
    @Override
    public Receiv<Command> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals(resourceAck.INSTANCE, this::onResourceAck)
                .onMessageEquals(resourceRequest.INSTANCE, this::onResourceReq)
                .build();
    }

    private Behavior<Command> onResourceAck() {
        return this;
    }

    private Behavior<Command> onResourceReq() {
        return this;
    }
}