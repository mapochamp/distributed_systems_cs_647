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
    public static Behavior<String> create(int numServers, int numClients) {
        return Behaviors.setup(context -> {
            List<ActorRef<ServerRPC>> ServerList = new ArrayList<ActorRef<ServerRPC>>();
            List<ActorRef<ServerRPC>> ClientList = new ArrayList<ActorRef<ServerRPC>>();
            for(int i=0; i < numServers; i++) {
                ServerList.add(context.spawn(Server.create(i), String.format("Server%d", i)));
            }
            //for(int i=0; i < numClients; i++) {
            //    ServerList.add(context.spawn(Client.create(i), String.format("Client%d", i)));
            //}
            return new Orchestrator(context, ServerList, ClientList);
        });
    }

    private List<ActorRef<ServerRPC>> ServerList;
    private boolean initialized;

    private Orchestrator(ActorContext context,
                         List<ActorRef<ServerRPC>> ServerList) {
        super(context);
        this.ServerList = ServerList;
        this.initialized = false;
    }
    @Override
    public Receive<String> createReceive() {
        return newReceiveBuilder()
                .onMessage(String.class, this::dispatch)
                .build();
    }

    public Behavior<String> dispatch(String txt) {
        stgetContext().getLog().info("[Orchestrator] received "+txt);
        switch (txt) {
            // TODO: case for setting timeout interval upperbound
            // TODO: case for setting heart beat interval
            // The Scala version uses a different type here, and essentially uses Behavior<Object>.
            case "shutdown":
                for(ActorRef<ServerRPC> actorRef : ServerList) {
                    actorRef.tell(new ServerRPC.End());
                }
                //resourceAcquirer3.tell(new ServerRPC.End());
                mutex.tell(new MutexMessage.End());
                return Behaviors.stopped();
            default:
                if(!initialized) {
                    var resourceAcquirer1 = ServerList.get(0);
                    for (ActorRef<ServerRPC> actorRef : ServerList) {
                        List<ActorRef<ServerRPC>> copy = new ArrayList<ActorRef<ServerRPC>>(ServerList);
                        copy.remove(actorRef);
                        actorRef.tell(new ServerRPC.InitActorRefList(copy));
                    }

                    resourceAcquirer1.tell(new ServerRPC.InitKickOff());
                    initialized = true;
                } else {
                    for(ActorRef<ServerRPC> actorRef : ServerList) {
                        actorRef.tell(new ServerRPC.End());
                    }
                    //resourceAcquirer3.tell(new ServerRPC.End());
                    mutex.tell(new MutexMessage.End());
                    return Behaviors.stopped();
                }
        }
        return this;
    }
}
