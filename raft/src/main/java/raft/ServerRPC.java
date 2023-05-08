package raft;

import java.util.*;
public sealed interface ServerRPC {
    public final record AppendEntries(int term, int leaderId, int prevLogIndex,
                                      int prevLogTerm, List<Command<Integer>> entries,
                                      int leaderCommit,
                                      ActorRef<ServerRPC> sender )implements ServerRPC {}
    public final record AppendEntriesResult(int term, boolean success,
                                            ActorRef<ServerRPC> sender) {}
    public final record RequestVote(int term, int candidateId, int lastLogIndex,
                                    int lastLogTerm,
                                    ActorRef<ServerRPC> sender ) {}
    public final record RequestVoteResult(int term, boolean voteGranted,
                                          ActorRef<ServerRPC> sender) {}
}
