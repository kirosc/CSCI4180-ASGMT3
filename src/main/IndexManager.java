package main;

import static java.lang.System.exit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import main.Handler.StorageHandler;

public class IndexManager {

  private static final String INDEX_FILE = "mydedup.index";
  private static final String directory = "data";


  // Chunk fingerprint => Reference counter
  public HashMap<String, Integer> chunkCount;

  // Chunk fingerprint => Chunk size
  public HashMap<String, Integer> chunkSizes;

  // File pathname => List of chunks' fingerprint
  public HashMap<String, List<String>> fileRecipe;

  public StorageHandler handler;

  public IndexManager(StorageHandler handler) {
    this.handler = handler;

    InputStream stream = handler.read(INDEX_FILE);

    if (stream == null) {
      chunkCount = new HashMap<>();
      chunkSizes = new HashMap<>();
      fileRecipe = new HashMap<>();
    } else {
      try {
        ObjectInputStream objectStream = new ObjectInputStream(stream);
        chunkCount = (HashMap<String, Integer>) objectStream.readObject();
        chunkSizes = (HashMap<String, Integer>) objectStream.readObject();
        fileRecipe = (HashMap<String, List<String>>) objectStream.readObject();
        objectStream.close();
      } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public void save() {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ObjectOutputStream stream;
    try {
      stream = new ObjectOutputStream(outputStream);
      stream.writeObject(chunkCount);
      stream.writeObject(chunkSizes);
      stream.writeObject(fileRecipe);
      stream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    handler.write(INDEX_FILE, inputStream);
  }

  public synchronized void uploadChunk(String fingerprint, InputStream stream) {
    if (chunkCount.containsKey(fingerprint)) {
      chunkCount.put(fingerprint, chunkCount.get(fingerprint) + 1);
    } else {
      chunkCount.put(fingerprint, 1);

      // Is a zero chunk
      if (isZeroChunk(fingerprint)) {
        int size = getZeroChunkSize(fingerprint);
        chunkSizes.put(fingerprint, size);
        return;
      }

      // Normal chunk
      try {
        chunkSizes.put(fingerprint, stream.available());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      Path path = Paths.get(directory, fingerprint);
      handler.write(path, stream);
    }
  }

  public InputStream downloadChunk(String fingerprint) {
    if (isZeroChunk(fingerprint)) {
      int size = getZeroChunkSize(fingerprint);
      return new ByteArrayInputStream(new byte[size]);
    }

    // Normal chunk
    Path path = Paths.get(directory, fingerprint);
    return handler.read(path);
  }

  public synchronized void removeChunk(String fingerprint) {
    int referenceCount = chunkCount.get(fingerprint);
    if (referenceCount-- > 1) {
      chunkCount.put(fingerprint, referenceCount);
      System.out.println(referenceCount + " " + fingerprint);
    } else {
      chunkCount.remove(fingerprint);
      chunkSizes.remove(fingerprint);

      if (!isZeroChunk(fingerprint)) {
        Path path = Paths.get(directory, fingerprint);
        handler.delete(path);
      }
    }
  }

  /**
   * Check if a chunk is a zero chunk given the fingerprint. Note: MD5 hex representation:
   * [a-fA-F0-9]
   */
  private boolean isZeroChunk(String fingerprint) {
    return fingerprint.startsWith("Z");
  }

  private int getZeroChunkSize(String fingerprint) {
    return Integer.parseInt(fingerprint.substring(1));
  }

  public void checkIfFileAlreadyExists(String filename) {
    if (fileRecipe.containsKey(filename)) {
      System.out.println("File " + filename + " already exists");
      exit(1);
    }
  }

  public void checkIfFileExists(String filename) {
    if (!fileRecipe.containsKey(filename)) {
      System.out.println("File " + filename + " does not exist");
      exit(1);
    }
  }

  public void printStat() {
    int chunkTotalSize = chunkCount.keySet().stream()
        .map(fingerprint -> chunkSizes.get(fingerprint))
        .mapToInt(Integer::intValue).sum();

    List<String> uniqueChunks = chunkCount.keySet().stream()
        .filter(fingerprint -> !isZeroChunk(fingerprint))
        .collect(Collectors.toList());

    int uniqueChunkTotalSize = uniqueChunks.stream()
        .map(fingerprint -> chunkSizes.get(fingerprint))
        .mapToInt(Integer::intValue).sum();

    DecimalFormat df = new DecimalFormat("0.00");
    df.setRoundingMode(RoundingMode.HALF_UP);
    String ratio = chunkTotalSize == 0 ? df.format(0)
        : df.format((double) chunkTotalSize / uniqueChunkTotalSize);

    System.out.println("Total number of files that have been stored: " + fileRecipe.size());
    System.out.println("Total number of pre-deduplicated chunks in storage: " + chunkCount.size());
    System.out.println("Total number of unique chunks in storage: " + uniqueChunks.size());
    System.out
        .println("Total number of bytes of pre-deduplicated chunks in storage: " + chunkTotalSize);
    System.out
        .println("Total number of bytes of unique chunks in storage: " + uniqueChunkTotalSize);
    System.out.println("Deduplication ratio: " + ratio);
  }
}
