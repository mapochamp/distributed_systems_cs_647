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
        this.lastVotedForTerm = 0;
        this.serverList = new ArrayList<>();
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
    int lastVotedForTerm;
    List<Pair<Integer, Integer>> templog;
    List<ActorRef<ServerRPC>> serverList;

    // Volatile state
    int commitIndex;
    int lastApplied;
    int votesReceived;
    State currentState;
    ActorRef<ServerRPC> currentLeader;

    // Volatile state on leaders
    Map<ActorRef<ServerRPC>, Integer> nextIndexMap;
    Map<ActorRef<ServerRPC>, Integer> matchIndexMap;


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
            case ServerRPC.AppendEntries a:
                restartTimer();
                if(a.term() > currentTerm) {
                    updateTerm(a.term());
                }
                // reply false if term < currentTerm
                if(a.term() < currentTerm) {
                    getContext().getLog().info(String.format("[Server %d] received Append Entries Request " +
                            " term %d case 1", id, a.term()));
                    a.sender().tell(new ServerRPC.AppendEntriesResult(currentTerm,
                            false, getContext().getSelf()));
                    break;
                    // TODO: do we need an out of bounds check here
                    // reply false if log doesn't contain an entry at prevLogIndex
                    // whose term matches prevLogTerm
                } else if(a.entry().isEmpty()) { // if heartbeat don't do anything
                    getContext().getLog().info(String.format("[Server %d] received Append Entries Request " +
                            " term %d case 2", id, a.term()));
                    a.sender().tell(new ServerRPC.AppendEntriesResult(currentTerm,
                            true, getContext().getSelf()));
                    currentLeader = a.sender();
                    break;
                } else if(a.prevLogIndex() == 0) {
                    getContext().getLog().info(String.format("[Server %d] received Append Entries Request " +
                            " term %d case 3", id, a.term()));
                    // if we get an entry with prevLogIndex set to 0, we just apply it instead of sending false
                    getContext().getLog().info(String.format("[Server %d] appending entries" +
                            " term %d", id, a.term()));
                    appendNewEntries(a.entry());
                    updateCommitIndexFollower(a.leaderCommit());
                    a.sender().tell(new ServerRPC.AppendEntriesResult(currentTerm,
                            true, getContext().getSelf()));
                    break;
                } else if(getLogTerm(a.prevLogIndex()) != a.prevLogTerm()) {
                    getContext().getLog().info(String.format("[Server %d] received Append Entries Request " +
                            " term %d case 4", id, a.term()));
                    a.sender().tell(new ServerRPC.AppendEntriesResult(currentTerm,
                            false, getContext().getSelf()));
                    break;
                    // if an existing entry conflicts with a  new one (same index
                    // but different terms), delete the existing entry adn all that
                    // follow it
                } else if(getLogTerm(a.prevLogIndex()) == a.prevLogTerm()) {
                    getContext().getLog().info(String.format("[Server %d] received Append Entries Request " +
                            " term %d case 5", id, a.term()));
                    getContext().getLog().info(String.format("[Server %d] conflict" +
                            " term %d", id, a.term()));
                    int conflictIdx = conflictExists(a.entry(), a.prevLogIndex());
                    if(conflictIdx != -1) {
                        deleteConflicts(conflictIdx);
                    }
                    getContext().getLog().info(String.format("[Server %d] appending entries" +
                            " term %d", id, a.term()));
                    appendNewEntries(a.entry());
                    updateCommitIndexFollower(a.leaderCommit());
                    a.sender().tell(new ServerRPC.AppendEntriesResult(currentTerm,
                            true, getContext().getSelf()));
                    break;
                }
                break;
            case ServerRPC.AppendEntriesResult a:
                restartTimer();
                //getContext().getLog().info(String.format("[Server %d] got Append entries result", id));
                if(a.success()) {
                    // if true then update our match index
                    matchIndexMap.put(a.sender(), nextIndexMap.get(a.sender()));
                    incrementNextIndex(a.sender());
                    // if true and enough servers reply true then we increment commit index
                    updateCommitIndexLeader();
                } else {
                    // if false then update decrement our next index
                    decrementNextIndex(a.sender());
                    a.sender().tell(new ServerRPC.AppendEntries(currentTerm, id, nextIndexMap.get(a.sender()),
                            getLogTerm(nextIndexMap.get(a.sender())), getEntry(nextIndexMap.get(a.sender())),
                            commitIndex, getContext().getSelf()));
                }
                break;
            case ServerRPC.RequestVote r:
                restartTimer();
                if(r.term() > currentTerm) {
                    updateTerm(r.term());
                }
                //getContext().getLog().info(String.format("[Server %d] got Vote request", id));
                // TODO: you can only vote once per term
                // TODO: tie break on log length
                if(r.term() < currentTerm || currentState == State.CANDIDATE) {
                    r.sender().tell(new ServerRPC.RequestVoteResult(currentTerm, false,
                                                                    id, getContext().getSelf()));
                    votedFor = 0;
                    lastVotedForTerm = currentTerm;
                    break;
                }
                if((votedFor == 0  || votedFor == r.candidateId()) && lastVotedForTerm < currentTerm) {
                    if(getLogTerm(r.lastLogIndex()) <= r.lastLogTerm()) {
                        votedFor = r.candidateId();
                        /*
                        getContext().getLog().info(String.format("[Server %d] votes for %d. lastVotedForTerm = %d" +
                                " currentTerm = %d  candidateTerm = %d", id, r.candidateId(), lastVotedForTerm, currentTerm,
                                    r.term()));
                         */
                        r.sender().tell(new ServerRPC.RequestVoteResult(currentTerm, true,
                                id, getContext().getSelf()));
                        break;
                    }
                    r.sender().tell(new ServerRPC.RequestVoteResult(currentTerm, false,
                            id, getContext().getSelf()));
                } else {
                    r.sender().tell(new ServerRPC.RequestVoteResult(currentTerm, false,
                            id, getContext().getSelf()));
                }
                lastVotedForTerm = currentTerm;
                votedFor = 0;
                break;
            case ServerRPC.RequestVoteResult r:
                restartTimer();
                //getContext().getLog().info(String.format("[Server %d] got vote result from %d", id, r.senderId()));
                if(r.voteGranted()) {
                    votesReceived++;
                }
                if(votesReceived > (serverList.size()/2) && currentState != State.LEADER) {
                    getContext().getLog().info(String.format("[Server %d] IS NOW LEADER", id));
                    currentState = State.LEADER;
                    List<Integer> entry = new ArrayList<>();
                    // send heartbeat (empty entry)
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
                    // TODO: we have to make sure that if we're currently catching up a node then we don't send it
                    // the entry data structure is meant to be able take multiple entries at once even though
                    // we only send one at a time. this is just simulating things
                    List<Integer> entry = new ArrayList<>();
                    entry.add(c.entry());
                    entry.add(currentTerm);
                    // append the entry to your own log first
                    getContext().getLog().info(String.format("[Server %d] appending entries" +
                            " term %d", id, currentTerm));
                    appendNewEntries(entry);
                    for(var server : serverList) {
                        // if the match index and our last applied don't match, don't resend a new request
                        if(matchIndexMap.get(server) == lastApplied) {
                            // TODO: out of bounds error for getLogTerm
                            server.tell(new ServerRPC.AppendEntries(currentTerm, id, nextIndexMap.get(server),
                                    getLogTerm(nextIndexMap.get(server)), getEntry(nextIndexMap.get(server)),
                                    commitIndex, getContext().getSelf()));
                        }
                    }
                    lastApplied++;
                }
                getContext().getLog().info(String.format("[Server %d] got client request", id));
                break;
            case ServerRPC.Timeout t:
                if (currentState == State.LEADER) {
                    // reset timer to fixed number
                    restartTimer();
                    // TODO: send heartbeat (empty append entries rpc)
                } else {
                    votesReceived = 0; //reset votesReceived in case we were a candidate before
                    /*
                    getContext().getLog().info(String.format("[Server %d] timed out on term %d",
                            id, currentTerm));
                     */
                    // set candidate state and try to become leader
                    currentState = State.CANDIDATE;
                    getContext().getLog().info(String.format("[Server %d] IS NOW CANDIDATE", id));
                    // set new term
                    currentTerm++;
                    // vote for yourself
                    votedFor = id;
                    votesReceived = 1;
                    // reset election timer
                    restartTimer();
                    //send requestVoteRPC to all other servers
                    //getContext().getLog().info(String.format("[Server %d] starting to broadcasting vote", id));
                    for(var server : serverList) {
                        server.tell(new ServerRPC.RequestVote(currentTerm, id, lastApplied,
                                getLogTerm(lastApplied), getContext().getSelf()));
                    }
                    //getContext().getLog().info(String.format("[Server %d] done broadcasting vote", id));
                }
                break;
            case ServerRPC.Init i:
                getContext().getLog().info(String.format("[Server %d] initializing", id));
                timers.startSingleTimer(TIMER_KEY, new ServerRPC.Timeout(), after);
                serverList = i.serverList();
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

    private void updateTerm(int newTerm) {
        currentTerm = newTerm;
        if(currentState != State.FOLLOWER) {
            currentState = State.FOLLOWER;
            getContext().getLog().info(String.format("[Server %d] IS NOW FOLLOWER", id));
        }
    }

    private void incrementNextIndex(ActorRef<ServerRPC> sender) {
        int nextIndex = nextIndexMap.get(sender);
        nextIndexMap.put(sender, nextIndex+1);
    }
    private void decrementNextIndex(ActorRef<ServerRPC> sender) {
        int nextIndex = nextIndexMap.get(sender);
        nextIndexMap.put(sender, nextIndex-1);
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
        /*
        try {
            log.readFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
         */

        var entry = log.get(prevLogIndex);

        try {
            log.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(entry.isEmpty()) {
            return -1;
        }
        return entry.get(1);
    }

    private List<Integer> getEntry(int logIndex) {
        /*
        try {
            log.readFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

         */

        var entry = log.get(logIndex);

        try {
            log.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(entry.isEmpty()) {
            return new ArrayList<>();
        }
        return entry;
    }

    private void deleteConflicts(int index) {
        /*
        try {
            log.readFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

         */

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
    private int conflictExists(List<Integer> entry, int prevLogIndex) {
        /*
        try {
            log.readFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

         */

        List<Integer> localEntries = log.get(prevLogIndex + 1);
        // if our term doesn't match the entry term
        if(localEntries.isEmpty() || localEntries.get(1) != entry.get(1)) {
            return prevLogIndex+1;
        }

        try {
            log.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void appendNewEntries(List<Integer> entry) {
        /*
        try {
            log.readFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

         */

        List<Integer> newLine = new ArrayList<>();
        newLine.add(entry.get(0));
        newLine.add(entry.get(1));
        log.add(newLine);

        try {
            log.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateCommitIndexFollower(int leaderCommit) {
        if(leaderCommit > commitIndex) {
            commitIndex = Math.min(leaderCommit, log.size() - 1);
        }
    }

    private void updateCommitIndexLeader() {
        for (var i : serverList) {
            int matchIndex = matchIndexMap.get(i);
            int matchCount = 0;
            for (var j : serverList) {
                if (matchIndex == matchIndexMap.get(j)) {
                    matchCount++;
                }
            }
            // update our commit index
            if (matchCount > serverList.size() / 2) {
                commitIndex = matchIndex;
            }
        }
        // Send commit to client
        List<Integer> entry = getEntry(commitIndex);
        // TODO: update all the entries to hold the client ID with it
        // TODO: pull the client ID from the entry
        // TODO: create a map of client ID and client ActorRefs
        // TODO: respond to client that entry was committed
    }

    public record Pair<X, Y>(X first, Y second) {
    }
}