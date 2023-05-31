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



public class StressTestClient extends AbstractBehavior<ClientRPC> {
    public static Behavior<ClientRPC> create(int id, List<ActorRef<ServerRPC>> serverList) {
        return Behaviors.setup(context ->
                Behaviors.withTimers(timers -> {
                    Random random = new Random();
                    int seconds = random.nextInt(15) + 30;
                    Duration after = Duration.ofSeconds(seconds);
                    return new StressTestClient(context, timers,after, id, serverList);
                })
        );
    }

    private StressTestClient(ActorContext ctxt, TimerScheduler<ClientRPC> timers,
                   Duration after, int id, List<ActorRef<ServerRPC>> serverList) {
        super(ctxt);
        this.serverList = serverList;
        this.timers = timers;
        this.after = after;
        this.id = id;
        this.serverId = id - 1;
        this.lastEntry = 0;
        this.oldServer = id - 1;
        this.usingClosestServer = false;
    }

    int id;
    int lastEntry;
    int serverId;
    int oldServer;
    boolean usingClosestServer;
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
                restartTimer();
                break;
            case ClientRPC.PingServerResponse p:
                restartTimer();
                break;
            case ClientRPC.RequestAck r:
                getContext().getLog().info(String.format("[Client %d] request committed", id));
                // we got an ack from the leader that something was committed
                restartTimer();
                break;
            case ClientRPC.RequestReject r:
                // we didn't send it to the leader so send it to the leader
                getContext().getLog().info(String.format("[Client %d] request rejected", id));
                if(r.leader() != null && r.success()) {
                    getContext().getLog().info(String.format("[Client %d] request resend", id));
                    sendRequest(r.leader(), r.entry());
                    restartTimer();
                } // else there are no tickets left
                break;
            case ClientRPC.UnstableReadRequestResult r:
                getContext().getLog().info(String.format("[Client %d] unstable read: %d", id, r.state()));
                // buy tickets
                buyTickets(r.state());
                restartTimer();
                break;
            case ClientRPC.StableReadRequestResult r:
                if (r.success()) {
                    getContext().getLog().info(String.format("[Client %d] stable read: %d", id, r.state()));
                    buyTickets(r.state());
                } else {
                    getContext().getLog().info(String.format("[Client %d] request resend", id));
                    if(r.leader() != null) {
                        r.leader().tell(new ServerRPC.ClientReadStableRequest(getContext().getSelf()));
                    }
                }
                restartTimer();
                break;
            case ClientRPC.UnstableReadRequest r:
                var serverToReadFrom = serverList.get(serverId);
                serverToReadFrom.tell(new ServerRPC.ClientReadUnstableRequest(getContext().getSelf()));
                restartTimer();
                break;
            case ClientRPC.StableReadRequest r:
                // ask our server for a stable read and get redirected if our server isn't the leader
                var serverStableRead = serverList.get(serverId);
                serverStableRead.tell(new ServerRPC.ClientReadStableRequest(getContext().getSelf()));
                restartTimer();
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
        int seconds = random.nextInt(15)+30;
        after = Duration.ofSeconds(seconds);
        timers.startSingleTimer(TIMER_KEY, new ClientRPC.Timeout(), after);
    }

    private void buyTickets(int ticketsLeft) {
        if(ticketsLeft == 0) {
            return;
        }
        Random random = new Random();
        int randomTicketcount = random.nextInt(ticketsLeft)+1;
        getContext().getLog().info(String.format("[Client %d] sending request to buy %d tix to server %d",
                id, randomTicketcount, serverId));
        serverList.get(serverId).tell(new ServerRPC.ClientWriteRequest(getContext().getSelf(),
                randomTicketcount));
    }

    private void sendRequest(ActorRef<ServerRPC> server, int entry) {
        server.tell(new ServerRPC.ClientWriteRequest(getContext().getSelf(), entry));
    }

}
