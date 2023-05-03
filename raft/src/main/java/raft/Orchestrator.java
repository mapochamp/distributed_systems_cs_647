package raft;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.ArrayList;
import java.util.List;


public class Orchestrator extends AbstractBehavior<String> {
    public static Behavior<String> create(int numActors) {
        return Behaviors.setup(context -> {
            List<ActorRef<ResourceMessage>> resourceAcquirerList = new ArrayList<ActorRef<ResourceMessage>>();
            for(int i=0; i < numActors; i++) {
                resourceAcquirerList.add(context.spawn(ResourceAcquirer.create(i, mutex), String.format("resourceAcquirer%d", i)));
            }
            return new Orchestrator(context, resourceAcquirerList, mutex);
        });
    }

    private List<ActorRef<ResourceMessage>> resourceAcquirerList;
    private ActorRef<MutexMessage> mutex;
    private boolean initialized;

    private Orchestrator(ActorContext context,
                         List<ActorRef<ResourceMessage>> resourceAcquirerList,
                         ActorRef<MutexMessage> mutex) {
        super(context);
        this.resourceAcquirerList = resourceAcquirerList;
        this.mutex = mutex;
        this.initialized = false;
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
                for(ActorRef<ResourceMessage> actorRef : resourceAcquirerList) {
                    actorRef.tell(new ResourceMessage.End());
                }
                //resourceAcquirer3.tell(new ResourceMessage.End());
                mutex.tell(new MutexMessage.End());
                return Behaviors.stopped();
            default:
                if(!initialized) {
                    var resourceAcquirer1 = resourceAcquirerList.get(0);
                    for (ActorRef<ResourceMessage> actorRef : resourceAcquirerList) {
                        List<ActorRef<ResourceMessage>> copy = new ArrayList<ActorRef<ResourceMessage>>(resourceAcquirerList);
                        copy.remove(actorRef);
                        actorRef.tell(new ResourceMessage.InitActorRefList(copy));
                    }

                    resourceAcquirer1.tell(new ResourceMessage.InitKickOff());
                    initialized = true;
                } else {
                    for(ActorRef<ResourceMessage> actorRef : resourceAcquirerList) {
                        actorRef.tell(new ResourceMessage.End());
                    }
                    //resourceAcquirer3.tell(new ResourceMessage.End());
                    mutex.tell(new MutexMessage.End());
                    return Behaviors.stopped();
                }
        }
        return this;
    }
}
