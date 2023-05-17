package raft;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;

import java.time.Duration;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;



public class Client extends AbstractBehavior<ClientRPC> {
    public static Behavior<ClientRPC> create(int id, List<ActorRef<ServerRPC>> serverList) {
        return Behaviors.setup(context ->
                Behaviors.withTimers(timers -> {
                    Random random = new Random();
                    int seconds = random.nextInt(5) + 1;
                    Duration after = Duration.ofSeconds(seconds);
                    return new Client(context, timers,after, id, serverList);
                })
        );
    }

    private Client(ActorContext ctxt, TimerScheduler<ClientRPC> timers,
                   Duration after, int id, List<ActorRef<ServerRPC>> serverList) {
        super(ctxt);
        this.serverList = serverList;
        this.timers = timers;
        this.after = after;
        this.id = id;
        this.lastEntry = 0;
    }

    int id;
    int lastEntry;
    private static final Object TIMER_KEY = new Object();
    private final TimerScheduler<ClientRPC> timers;
    private Duration after;
    private List<ActorRef<ServerRPC>> serverList;

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
            case ClientRPC.End e:
                return Behaviors.stopped();
            case ClientRPC.Timeout t:
                ActorRef<ServerRPC> server;
                // get random server from server list
                Random random = new Random();
                int randomIndex = random.nextInt(serverList.size());
                getContext().getLog().info(String.format("[Client %d] sending request to server %d",
                        id, randomIndex+1));
                sendRequest(serverList.get(randomIndex), lastEntry);
                lastEntry++;
                break;
            case ClientRPC.RequestAck r:
                getContext().getLog().info(String.format("[Client %d] request committed", id));
                // we got an ack from the leader that something was committed
                restartTimer();
                break;
            case ClientRPC.RequestReject r:
                // we didn't send it to the leader so send it to the leader
                getContext().getLog().info(String.format("[Client %d] request rejected", id));
                if(r.leader() == null) {
                    restartTimer();
                } else {
                    getContext().getLog().info(String.format("[Client %d] request resend", id));
                    sendRequest(r.leader(), r.entry());
                }
                break;
            case ClientRPC.Init i:
                getContext().getLog().info(String.format("[Client %d] initializing", id));
                timers.startSingleTimer(TIMER_KEY, new ClientRPC.Timeout(), after);
                break;
            default:
                return Behaviors.stopped();
        }
        // Keep the same message handling behavior
        return this;
    }
    private void restartTimer() {
        timers.cancel(TIMER_KEY);
        Random random = new Random();
        int seconds = random.nextInt(5) + 1;
        after = Duration.ofSeconds(seconds);
        timers.startSingleTimer(TIMER_KEY, new ClientRPC.Timeout(), after);
    }

    private void sendRequest(ActorRef<ServerRPC> server, int entry) {
        server.tell(new ServerRPC.ClientRequest(getContext().getSelf(), entry));
    }

}
