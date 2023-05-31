package raft;

import java.util.*;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import raft.Server.Pair;
public sealed interface ServerRPC {
    // entries are stored as list of pairs (we use list as a pair). so its like
    // [ <entry, term> , <entry, term> , ... , <entry, term> ]
    public final record AppendEntries(int term, int leaderId, int prevLogIndex,
                                      int prevLogTerm, List<Integer> entry,
                                      int leaderCommit,
                                      ActorRef<ServerRPC> sender )implements ServerRPC {}
    public final record AppendEntriesResult(int term, boolean success,
                                            ActorRef<ServerRPC> sender) implements  ServerRPC {}
    public final record RequestVote(int term, int candidateId, int lastLogIndex,
                                    int lastLogTerm, int logLength,
                                    ActorRef<ServerRPC> sender ) implements  ServerRPC {}
    public final record RequestVoteResult(int term, boolean voteGranted, int senderId,
                                          ActorRef<ServerRPC> sender) implements  ServerRPC {}

    public final record ClientWriteRequest(ActorRef<ClientRPC> sender, int entry) implements ServerRPC {}

    public final record ClientReadUnstableRequest(ActorRef<ClientRPC> sender) implements ServerRPC {}
    public final record ClientReadStableRequest(ActorRef<ClientRPC> sender) implements ServerRPC {}
    public final record Timeout() implements ServerRPC {}
    public final record Init(List<ActorRef<ServerRPC>> serverList) implements ServerRPC {}
    
    public final record End() implements ServerRPC {}
    public final record Kill() implements ServerRPC {}
    public final record PingServer(ActorRef<ClientRPC> sender) implements ServerRPC {}
}
