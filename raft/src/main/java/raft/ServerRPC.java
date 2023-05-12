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
    public final record AppendEntries(int term, int leaderId, int prevLogIndex,
                                      int prevLogTerm, List<List<Integer>> entries,
                                      int leaderCommit,
                                      ActorRef<ServerRPC> sender )implements ServerRPC {}
    public final record AppendEntriesResult(int term, boolean success,
                                            ActorRef<ServerRPC> sender) implements  ServerRPC {}
    public final record RequestVote(int term, int candidateId, int lastLogIndex,
                                    int lastLogTerm,
                                    ActorRef<ServerRPC> sender ) implements  ServerRPC {}
    public final record RequestVoteResult(int term, boolean voteGranted,
                                          ActorRef<ServerRPC> sender) implements  ServerRPC {}

    public final record End() implements ServerRPC {}
}
