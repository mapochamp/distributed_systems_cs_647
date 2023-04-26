package main;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.ActorRef;

public class Orchestrator extends AbstractBehavior<String> {
    public static Behavior<String> create() {
        // TODO: make this so that we loop and spawn stuff
        return Behaviors.setup(context -> {
            var resourceAcquirer = context.spawn(ResourceAcquirer.create(), "echo");
            var mutex = context.spawn(Mutex.create(), "proxy");
            return new Orchestrator(context, resourceAcquirer, mutex);
        });
    }

    // TODO:make this a list of ActorRef's
    private ActorRef<ResourceMessage> resourceAcquirer;
    private ActorRef<MutexMessage> mutex;

    // TODO: make this take a list of Actor Ref's instead of a single one as an arg?
    private Orchestrator(ActorContext context, ActorRef<ResourceMessage> resourceAcquirer,
                         ActorRef<MutexMessage> mutex) {
        super(context);
        this.resourceAcquirer = resourceAcquirer;
        this.mutex = mutex;
    }
    @Override
    public Receive<String> createReceive() {
        return newReceiveBuilder()
                .onMessage(String.class, this::dispatch)
                .build();
    }

    public Behavior<String> dispatch(String txt) {
        getContext().getLog().info("[Orchestrator] received "+txt);
        switch (txt) {
            // The Scala version uses a different type here, and essentially uses Behavior<Object>.
            case "shutdown":
                resourceAcquirer.tell(new ResourceMessage.End());
                mutex.tell(new MutexMessage.End());
                return Behaviors.stopped();
            default:
                resourceAcquirer.tell(new Mutex.Lock(resourceAcquirer));
        }
        return this;
    }
}
