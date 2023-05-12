package raft;

public interface ClientRPC {
    public final record temp() implements ClientRPC {}
    public final record End() implements ClientRPC {}
}
