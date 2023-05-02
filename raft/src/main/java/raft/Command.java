package raft;

public final record Command<T> (T value) { }
