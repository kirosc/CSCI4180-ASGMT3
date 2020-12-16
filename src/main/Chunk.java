package main;

public class Chunk {
  int offset; // First index of the chunk
  int size; // Last index = offset + size - 1
  boolean isZeroChunk = false;
  String fingerprint;

  Chunk(int offset, int size) {
    this.offset = offset;
    this.size = size;
  }
}
