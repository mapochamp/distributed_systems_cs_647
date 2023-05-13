package raft;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.ArrayList;
import java.util.List;
import java.time.Duration;


public class Orchestrator extends AbstractBehavior<String> {
    public static Behavior<String> create(int numServers, int numClients) {
        return Behaviors.setup(context -> {
            List<ActorRef<ServerRPC>> ServerList = new ArrayList<ActorRef<ServerRPC>>();
            List<ActorRef<ClientRPC>> ClientList = new ArrayList<ActorRef<ClientRPC>>();
            for(int i=1; i < numServers+1; i++) {
                ServerList.add(context.spawn(Server.create(i), String.format("Server%d", i)));
            }
            for(int i=1; i < numClients+1; i++) {
                ClientList.add(context.spawn(Client.create(i, ServerList), String.format("Client%d", i)));
            }
            return new Orchestrator(context, ServerList, ClientList);
        });
    }

    private List<ActorRef<ServerRPC>> ServerList;
    private List<ActorRef<ClientRPC>> ClientList;
    private boolean initialized;

    private Orchestrator(ActorContext context,
                         List<ActorRef<ServerRPC>> ServerList,
                         List<ActorRef<ClientRPC>> ClientList) {
        super(context);
        this.ServerList = ServerList;
        this.ClientList = ClientList;
        this.initialized = false;
    }
    @Override
    public Receive<String> createReceive() {
        return newReceiveBuilder()
                .onMessage(String.class, this::dispatch)
                .build();
    }

    // TODO: restart/kill servers
    public Behavior<String> dispatch(String txt) {
        getContext().getLog().info("[Orchestrator] received "+txt);
        switch (txt) {
            // TODO: case for setting timeout interval upperbound
            // TODO: case for setting heart beat interval
            // The Scala version uses a different type here, and essentially uses Behavior<Object>.
            case "shutdown":
                for(ActorRef<ServerRPC> actorRef : ServerList) {
                    actorRef.tell(new ServerRPC.End());
                }
                for(ActorRef<ClientRPC> actorRef : ClientList) {
                    actorRef.tell(new ClientRPC.End());
                }
                return Behaviors.stopped();
            default:
                // TODO init. idk
                if(!initialized)  {
                    getContext().getLog().info("Initializing all servers");
                    for(ActorRef<ServerRPC> actorRef : ServerList) {
                        actorRef.tell(new ServerRPC.Init(ServerList));
                    }
                    getContext().getLog().info("Initializing all clients");
                    for(ActorRef<ClientRPC> actorRef : ClientList) {
                        actorRef.tell(new ClientRPC.Init());
                    }
                } else  {
                    for(ActorRef<ServerRPC> actorRef : ServerList) {
                        actorRef.tell(new ServerRPC.End());
                    }
                    for(ActorRef<ClientRPC> actorRef : ClientList) {
                        actorRef.tell(new ClientRPC.End());
                    }
                    return Behaviors.stopped();
                }
        }
        return this;
    }
}
