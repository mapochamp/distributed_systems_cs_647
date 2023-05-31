package raft;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.SupervisorStrategy;

import java.util.ArrayList;
import java.util.List;
import java.time.Duration;
import java.util.Random;


public class Orchestrator extends AbstractBehavior<String> {
    public static Behavior<String> create(int numServers, int numClients) {
        return Behaviors.setup(context -> {
            List<ActorRef<ServerRPC>> ServerList = new ArrayList<ActorRef<ServerRPC>>();
            List<ActorRef<ClientRPC>> ClientList = new ArrayList<ActorRef<ClientRPC>>();
            for(int i=1; i < numServers+1; i++) {
                Behavior<ServerRPC> supervisedServer = Behaviors.supervise(Server.create(i))
                                .onFailure(SupervisorStrategy.restart());
                ServerList.add(context.spawn(supervisedServer, String.format("Server%d", i)));
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

    public Behavior<String> dispatch(String txt) {
        getContext().getLog().info("[Orchestrator] received "+txt);
        switch (txt) {
            // The Scala version uses a different type here, and essentially uses Behavior<Object>.
            case "shutdown":
                for(ActorRef<ServerRPC> actorRef : ServerList) {
                    actorRef.tell(new ServerRPC.End());
                }
                for(ActorRef<ClientRPC> actorRef : ClientList) {
                    actorRef.tell(new ClientRPC.End());
                }
                return Behaviors.stopped();
            case "kill":
                Random random = new Random();
                int id = random.nextInt(ServerList.size() - 1);
                getContext().getLog().info(String.format("Killing server %d", id));
                var server = ServerList.get(id);
                server.tell(new ServerRPC.Kill());
                //id = random.nextInt(ClientList.size() - 1);
                //var client = ClientList.get(id);
                //client.tell(new ClientRPC.Timeout());
                break;
            case "unstable":
                Random random1 = new Random();
                int idx = random1.nextInt(ClientList.size()-1);
                var requester = ClientList.get(idx);
                requester.tell(new ClientRPC.UnstableReadRequest());
                break;
            case "stable":
                Random random2 = new Random();
                int index = random2.nextInt(ClientList.size()-1);
                var stableClient = ClientList.get(index);
                stableClient.tell(new ClientRPC.StableReadRequest());
                break;
            default:
                if(!initialized)  {
                    getContext().getLog().info("Initializing all servers");
                    for(ActorRef<ServerRPC> actorRef : ServerList) {
                        List<ActorRef<ServerRPC>> copy = new ArrayList<ActorRef<ServerRPC>>(ServerList);
                        copy.remove(actorRef);
                        actorRef.tell(new ServerRPC.Init(copy));
                    }
                    getContext().getLog().info("Initializing all clients");
                    for(ActorRef<ClientRPC> actorRef : ClientList) {
                        actorRef.tell(new ClientRPC.Init());
                    }
                    initialized = true;
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
