package main;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.ActorRef;

//TODO: this will send Mutex messages to ack resourceAquirers to let them know if they actually
// got a mutex or not. think of it like a semaphore or a reader writer lock where resourceAquireier
// won't actually know if they got it unless this acks back
public class Mutex extends AbstractBehavior<MutexMessage> {
    private static final int MAX_SLEEP_TIME_MS = 1000;

    public void simulateLock() {
        Random random = new Random();
        int sleepTime = random.nextInt(MAX_SLEEP_TIME_MS);

        TimeUnit.MILLISECONDS.sleep(sleepTime);
    }

    public static Behavior<MutexMessage> create() {
        return Behaviors.setup(context -> {
            return new Mutex(context);
        });
    }

    private Mutex(ActorContext ctxt) {
        super(ctxt);
    }
    @Override
    public Receive<MutexMessage> createReceive() {
        // This method is only called once for initial setup
        return newReceiveBuilder()
                // We could register multiple onMessage handlers, for each subclass of EchoRequest, if we wanted to.
                // By using a single handler for the general message type, it makes it easier to switch handling of all message types simultaneously (in a later project)
                .onMessage(MutexMessage.class, this::dispatch)
                .build();
    }

    public Behavior<MutexMessage> dispatch(MutexMessage msg) {
        // This style of switch statement is technically a preview feature in many versions of Java, so you'll need to compile with --enable-preview
        switch (msg) {
            case MutexMessage.Lock l:
                // TODO: need timestamp to ack?
                getContext().getLog().info("[Mutex] Locking Lock");
                l.sender().tell(new ResourceMessage.Ack());
                break;
            case MutexMessage.Release r:
                getContext().getLog().info("[Mutex] Releasing Lock");
                r.sender().tell(new ResourceMessage.Ack());
                break;
            case MutexMessage.End e:
                getContext().getLog().info("[Mutex] shutting down");
                return Behaviors.stopped();
        }
        // Keep the same message handling behavior
        return this;
    }

}
