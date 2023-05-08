package raft;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.ActorRef;
import java.util.*;


public class Server extends AbstractBehavior<ServerRPC>{
    public static Behavior<ServerRPC> create(int id) {
        return Behaviors.setup(context -> {
            return new Server(context);
        });
    }

    private Server(ActorContext ctxt, int id) {
        super(ctxt);
        this.id = id;
        this.currentTerm = 0;
        this.votedFor = 0;
        this.log = new ArrayList<Pair<Command<Integer>, Integer>>();
        this.commitIndex = 0;
        this.lastApplied = 0;
        this.nextIndex = new ArrayList<>();
        this.matchIndex = new ArrayList<>();
    }

    // Presistent State
    int id;
    int currentTerm;
    int votedFor;
    List<Pair<Command<Integer>, Integer>> log;

    // Volatile state
    int commitIndex;
    int lastApplied;

    // Volatile state on leaders
    List<Integer> nextIndex;
    List<Integer> matchIndex;


    @Override
    public Receive<ServerRPC> createReceive() {
        // This method is only called once for initial setup
        return newReceiveBuilder()
                // We could register multiple onMessage handlers, for each subclass of EchoRequest, if we wanted to.
                // By using a single handler for the general message type, it makes it easier to switch handling of all message types simultaneously (in a later project)
                .onMessage(ServerRPC.class, this::dispatch)
                .build();
    }

    public Behavior<ServerRPC> dispatch(EchoRequest msg) {
        // This style of switch statement is technically a preview feature in many versions of Java, so you'll need to compile with --enable-preview
        switch (msg) {
            case ServerRPC.AppendEntries a:
                if(a.term() < currentTerm) {
                    a.sender().tell(new ServerRPC.AppendEntriesResult(currentTerm,
                            false, getContext().getSelf()));
                    break;
                    // TODO: do we need an out of bounds check here
                } else if (log.get(a.prevLogIndex()).second != a.prevLogTerm()) {
                    a.sender().tell(new ServerRPC.AppendEntriesResult(currentTerm,
                            false, getContext().getSelf()));
                    break;
                } else if(log.get(a.prevLogIndex()).second != a.prevLogIndex()) { // receiver implementation #3

                } else if() { // heart beat (empty etnries)

                }
                break;
            case ServerRPC.AppendEntriesResult a:
                break;
            case ServerRPC.RequestVote r:
                lastmsg = e.msg();
                getContext().getLog().info("[EchoServer] echoing "+e.msg());
                e.sender().tell(new ServerRPC.Ack(lastmsg));
                break;
            case ServerRPC.RequestVoteResult r:
                lastmsg = e.msg();
                getContext().getLog().info("[EchoServer] echoing "+e.msg());
                e.sender().tell(new ServerRPC.Ack(lastmsg));
                break;
            case default:
                return Behaviors.stopped();
        }
        // Keep the same message handling behavior
        return this;
    }

    public static class Pair<X, Y> {
        public final X first;
        public final Y second;

        public Pair(X first, Y second) {
            this.first = first;
            this.second = second;
        }
    }
}