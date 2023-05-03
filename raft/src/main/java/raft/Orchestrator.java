package raft;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class Orchestrator extends AbstractBehavior<String> {
    public static Behavior<String> create() {
        return Behaviors.setup(context -> {
            var echo = context.spawn(EchoServer.create(), "echo");
            var proxy = context.spawn(Proxy.create(echo), "proxy");
            return new Orchestrator(context, echo, proxy);
        });
    }

    private ActorRef<EchoRequest> echo;
    private ActorRef<ProxyMessage> proxy;

    private Orchestrator(ActorContext ctxt, ActorRef<EchoRequest> echo, ActorRef<ProxyMessage> proxy) {
        super(ctxt);
        this.echo = echo;
        this.proxy = proxy;
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