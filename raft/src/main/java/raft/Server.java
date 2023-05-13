package raft;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.TimerScheduler;

import java.time.Duration;

import java.io.IOException;
import java.util.*;


public class Server extends AbstractBehavior<ServerRPC>{

    public static Behavior<ServerRPC> create(int id) {
        return Behaviors.setup(context ->
                Behaviors.withTimers(timers -> {
                    Random random = new Random();
                    int seconds = random.nextInt(7) + 2;
                    Duration after = Duration.ofSeconds(seconds);
                    return new Server(context, timers, id,after);
                })
        );
    }


    private Server(ActorContext ctxt, TimerScheduler<ServerRPC> timers, int id, Duration after) {
        super(ctxt);
        this.timers = timers;
        this.after = after;
        this.id = id;
        this.currentTerm = 0;
        this.votedFor = 0;
        this.commitIndex = 0;
        this.lastApplied = 0;
        this.currentLeader = null;
        this.nextIndex = new ArrayList<>();
        this.matchIndex = new ArrayList<>();
        this.currentState = State.FOLLOWER;
        this.nextIndexMap = new HashMap<>();
        this.matchIndexMap = new HashMap<>();
        try {
            this.log = new FileArray(String.format("%d_server_log", this.id));
       } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private enum State {
        FOLLOWER,
        CANDIDATE,
        LEADER
    }
    private static final Object TIMER_KEY = new Object();
    private final TimerScheduler<ServerRPC> timers;
    private Duration after;
    // Presistent State
    FileArray log;
    int id;
    int currentTerm;
    int votedFor;
    List<Pair<Integer, Integer>> templog;
    List<ActorRef<ServerRPC>> serverList;

    // Volatile state
    int commitIndex;
    int lastApplied;
    int votesReceived;
    State currentState;
    ActorRef<ServerRPC> currentLeader;
    // id : nextIndex
    Map<ActorRef<ServerRPC>, Integer> nextIndexMap;
    Map<ActorRef<ServerRPC>, Integer> matchIndexMap;

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

    public Behavior<ServerRPC> dispatch(ServerRPC msg) {
        // This style of switch statement is technically a preview feature in many versions of Java, so you'll need to compile with --enable-preview
        switch (msg) {
            // TODO: WHAT DO WE DO ON A HEARTBEAT OR INITTED MESSAGE WHERE THE ENTRIES ARE EMPTY?
            case ServerRPC.AppendEntries a:
                restartTimer();
                if(a.term() > currentTerm) {
                    updateTerm(a.term());
                }
                getContext().getLog().info(String.format("[Server %d] received Append Entries Request " +
                        " term %d", id, a.term()));
                if(a.term() < currentTerm) {
                    a.sender().tell(new ServerRPC.AppendEntriesResult(currentTerm,
                            false, getContext().getSelf()));
                    break;
                    // TODO: do we need an out of bounds check here
                } else if(getLogTerm(a.prevLogIndex()) != a.prevLogTerm()) {
                    a.sender().tell(new ServerRPC.AppendEntriesResult(currentTerm,
                            false, getContext().getSelf()));
                    break;
                } else if(getLogTerm(a.prevLogIndex()) == a.prevLogTerm()) {
                    getContext().getLog().info(String.format("[Server %d] conflict" +
                            " term %d", id, a.term()));
                    int conflictIdx = conflictExists(a.entries(), a.prevLogIndex());
                    if(conflictIdx != -1) {
                        deleteConflicts(conflictIdx);
                    }
                    getContext().getLog().info(String.format("[Server %d] appending entries" +
                            " term %d", id, a.term()));
                    appendNewEntries(a.entries(), a.prevLogIndex());
                    updateCommitIndex(a.leaderCommit());
                    a.sender().tell(new ServerRPC.AppendEntriesResult(currentTerm,
                            true, getContext().getSelf()));
                    break;
                }
                break;
            case ServerRPC.AppendEntriesResult a:
                restartTimer();
                getContext().getLog().info(String.format("[Server %d] got Append entries result", id));
                // if we get a true and enough servers reply true then we increment commit index
                // if we get a false then ...
                break;
            case ServerRPC.RequestVote r:
                restartTimer();
                if(r.term() > currentTerm) {
                    updateTerm(r.term());
                }
                getContext().getLog().info(String.format("[Server %d] got Vote request", id));
                // TODO: you can only vote once per term
                // TODO: tie break on log length
                if(r.term() < currentTerm) {
                    r.sender().tell(new ServerRPC.RequestVoteResult(currentTerm, false,
                                                                    getContext().getSelf()));
                    votedFor = 0;
                    break;
                }
                if(votedFor == 0  || votedFor == r.candidateId()) {
                    if(getLogTerm(r.lastLogIndex()) <= r.lastLogTerm()) {
                        votedFor = r.candidateId();
                        r.sender().tell(new ServerRPC.RequestVoteResult(currentTerm, true,
                                getContext().getSelf()));
                        break;
                    }
                    r.sender().tell(new ServerRPC.RequestVoteResult(currentTerm, false,
                            getContext().getSelf()));
                } else {
                    r.sender().tell(new ServerRPC.RequestVoteResult(currentTerm, false,
                            getContext().getSelf()));
                }
                votedFor = 0;
                break;
            case ServerRPC.RequestVoteResult r:
                restartTimer();
                getContext().getLog().info(String.format("[Server %d] got vote result", id));
                votesReceived++;
                if(votesReceived > (serverList.size()/2)) {
                    currentState = State.LEADER;
                    List<List<Integer>> entry = new ArrayList<>();
                    // send heartbeat
                    for(var server : serverList) {
                        server.tell(new ServerRPC.AppendEntries(currentTerm, id,
                                lastApplied, getLogTerm(lastApplied), entry, commitIndex,
                                getContext().getSelf()));
                    }
                }
                break;
            case ServerRPC.ClientRequest c:
                if(currentState == State.FOLLOWER) {
                    c.sender().tell(new ClientRPC.RequestReject(currentLeader, c.entry()));
                } else {
                    // TODO: make all entries just fucking ints
                    // TODO: we have to make sure that if we're currently catching up a node then we don't send it
                    // the entry data structure is meant to be able take multiple entries at once even though
                    // we only send one at a time. this is just simulating things
                    List<List<Integer>> entry = new ArrayList<>();
                    List<Integer> temp = new ArrayList<>();
                    temp.add(c.entry(), currentTerm);
                    entry.add(temp);
                    for(var server : serverList) {
                        server.tell(new ServerRPC.AppendEntries(currentTerm, id, lastApplied,
                                getLogTerm(lastApplied) ,entry, commitIndex, getContext().getSelf()));
                    }
                }
                getContext().getLog().info(String.format("[Server %d] got client request", id));
                break;
            case ServerRPC.Timeout t:
                if (currentState == State.LEADER) {
                    // reset timer to fixed number
                    restartTimer();
                    // send heartbeat (empty append entries rpc)
                } else {
                    getContext().getLog().info(String.format("[Server %d] timed out on term %d",
                            id, currentTerm));
                    // set candidate state and try to become leader
                    currentState = State.CANDIDATE;
                    // set new term
                    currentTerm++;
                    // vote for yourself
                    votedFor = id;
                    votesReceived = 1;
                    // reset election timer
                    restartTimer();
                    //send requestVoteRPC to all other servers
                    getContext().getLog().info(String.format("[Server %d] starting to broadcasting vote", id));
                    for(var server : serverList) {
                        server.tell(new ServerRPC.RequestVote(currentTerm, id, lastApplied,
                                getLogTerm(lastApplied), getContext().getSelf()));
                    }
                    getContext().getLog().info(String.format("[Server %d] done broadcasting vote", id));
                }
                break;
            case ServerRPC.Init i:
                getContext().getLog().info(String.format("[Server %d] initializing", id));
                timers.startSingleTimer(TIMER_KEY, new ServerRPC.Timeout(), after);
                serverList = i.serverList();
                //TODO: remove ourselves from serverlist
                removeFromServerList(this.serverList, getContext().getSelf());
                for(ActorRef<ServerRPC> server : serverList) {
                    nextIndexMap.put(server, 0);
                    matchIndexMap.put(server, 0);
                }
                break;
            case default:
                return Behaviors.stopped();
        }
        // Keep the same message handling behavior
        return this;
    }

    private void removeFromServerList(List<ActorRef<ServerRPC>> serverList, ActorRef<ServerRPC> serverToRemove) {
        Iterator<ActorRef<ServerRPC>> iterator = serverList.iterator();
        while(iterator.hasNext()) {
            ActorRef<ServerRPC> entry = iterator.next();
            if(entry == serverToRemove) {
                iterator.remove();
            }
        }
    }

    private void updateTerm(int newTerm) {
        currentTerm = newTerm;
        if(currentState != State.FOLLOWER) {
            currentState = State.FOLLOWER;
        }
    }

    private void restartTimer() {
        if(currentState != State.LEADER) {
            timers.cancel(TIMER_KEY);
            Random random = new Random();
            int seconds = random.nextInt(7) + 2;
            after = Duration.ofSeconds(seconds);
            timers.startSingleTimer(TIMER_KEY, new ServerRPC.Timeout(), after);
        } else {
            after = Duration.ofSeconds(3);
            timers.startSingleTimer(TIMER_KEY, new ServerRPC.Timeout(), after);
        }
    }

    private int getLogTerm(int prevLogIndex) {
        try {
            log.readFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        var entries = log.get(prevLogIndex);

        try {
            log.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entries.get(1);
    }

    private void deleteConflicts(int index) {
        try {
            log.readFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO: check if index is equal to last index
       log.truncate(index);

        try {
            log.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO: we get index out of bounds when trying access things at the very beginning when
    // prev log index = 0 and we try to access a file with no lines.
    private int conflictExists(List<List<Integer>> entries, int prevLogIndex) {
        try {
            log.readFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        int entriesIdx = 0;
        for(int i = prevLogIndex + 1; i < log.size(); i++) {
            var localEntries = log.get(i);
            // if our term doesn't match the entry term
            if(localEntries.get(1) != entries.get(entriesIdx).get(1)) {
               return i;
            }
            entriesIdx++;
        }

        try {
            log.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void appendNewEntries(List<List<Integer>> entries, int prevLogIndex) {
        try {
            log.readFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(int i = 0; i < entries.size(); i++) {
            List<Integer> newLine = new ArrayList<>();
            newLine.add(entries.get(i).get(0));
            newLine.add(entries.get(i).get(1));
            log.add(newLine);
        }

        try {
            log.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateCommitIndex(int leaderCommit) {
        if(leaderCommit > commitIndex) {
            commitIndex = Math.min(leaderCommit, log.size() - 1);
        }
    }

    public record Pair<X, Y>(X first, Y second) {
    }
}