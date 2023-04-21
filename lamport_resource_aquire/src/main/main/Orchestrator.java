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
            var echo = context.spawn(EchoServer.create(), "echo");
            var proxy = context.spawn(Proxy.create(echo), "proxy");
            return new Orchestrator(context, echo, proxy);
        });
    }

    // TODO:make this a list of ActorRef's
    private ActorRef<EchoRequest> resourceAcquirer;

    // TODO: make this take a list of Actor Ref's instead of a single one as an arg
    private Orchestrator(ActorContext context, ActorRef<EchoRequest> resourceAcquirer) {
        super(context);
        this.resourceAcquirer = resourceAcquirer;
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
            case "repeat":
                proxy.tell(new ProxyMessage.Repeat());
                break;
            // The Scala version uses a different type here, and essentially uses Behavior<Object>.
            case "shutdown":
                proxy.tell(new ProxyMessage.Shutdown());
                echo.tell(new EchoRequest.End());
                return Behaviors.stopped();
            default:
                proxy.tell(new ProxyMessage.Request(txt));
        }
        return this;
    }
}
