package main;

import static java.lang.System.exit;
import static main.Utils.Utils.modQ;
import static main.Utils.Utils.powerAndModQ;
import static main.Utils.Utils.readFile;
import static main.Utils.Utils.setQ;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import main.Handler.LocalHandler;
import main.Handler.StorageHandler;

public class MyDedup {

  static int FILE_SIZE, MIN_CHUNK_SIZE, MAX_CHUNK_SIZE;

  static StorageHandler handler;

  public static void main(String[] args) {
    if (args.length == 0) {
      printUsage();
    }

    String storageMethod = args[args.length - 1];

    if (storageMethod.equals("local")) {
      handler = new LocalHandler();
    } else if (storageMethod.equals("azure")) {
      // TODO: Azure
      handler = new LocalHandler();
    }

    String mode = args[0];

    if (mode.equals("upload")) {
      upload(args);
    } else if (mode.equals("download")) {
      System.out.println("download");
    } else if (mode.equals("delete")) {
      System.out.println("delete");
    } else {
      printUsage();
    }


  }

  public static void upload(String[] args) {
    checkArgsLength(args, 7);
    int minChunkSize = Integer.parseInt(args[1]); // m
    int avgChunkSize = Integer.parseInt(args[2]); // q
    int maxChunkSize = Integer.parseInt(args[3]);
    int d = Integer.parseInt(args[4]);
    Path pathname = Paths.get(args[5]);

    if (
        Math.log(minChunkSize) / Math.log(2) % 1 != 0 ||
            Math.log(avgChunkSize) / Math.log(2) % 1 != 0 ||
            Math.log(maxChunkSize) / Math.log(2) % 1 != 0
    ) {
      System.out.println("chunk size needs to be a power of 2");
      exit(1);
    }

    byte[] fileBytes = readFile(pathname);
    ArrayList<Chunk> chunks;
    IndexManager indexManager;

    MIN_CHUNK_SIZE = minChunkSize;
    MAX_CHUNK_SIZE = maxChunkSize;
    FILE_SIZE = fileBytes.length;

    indexManager = new IndexManager(handler);

    chunks = generateChunks(fileBytes, MIN_CHUNK_SIZE, d, avgChunkSize);

    // Calculate MD5 and upload chunks
    chunks.stream().parallel().forEach(chunk -> {
      if (chunk.isZeroChunk) {
        chunk.fingerprint = "Z" + chunk.size;
      } else {
        calculateHash(fileBytes, chunk);
      }

      InputStream stream = new ByteArrayInputStream(fileBytes, chunk.offset, chunk.size);
      indexManager.uploadChunk(chunk.fingerprint, stream);
    });

    // Update file receipt
    List<String> chunkList =
        chunks.stream().map(chunk -> chunk.fingerprint)
            .collect(Collectors.toList());

    indexManager.fileRecipe.put(pathname.toString(), chunkList);

    indexManager.printStat();

    indexManager.save();
  }

  /**
   * Generate a list of chunks using Rabin fingerprinting
   */
  public static ArrayList<Chunk> generateChunks(byte[] t, int m, int d, int q) {
    ArrayList<Chunk> chunks = new ArrayList<>();
    int rfp = 0;

    // min_chunk_size > file size
    if (m > FILE_SIZE) {
      chunks.add(new Chunk(0, FILE_SIZE));
      return chunks;
    }

    Chunk chunk = new Chunk(0, m);
    while (true) {
      // Reach EOF
      if (chunk.offset + chunk.size >= FILE_SIZE) {
        chunk.size = FILE_SIZE - chunk.offset;
        chunks.add(chunk);
        break;
      }

      // Chunk size reaches max_chunk_size
      if (chunk.size >= MAX_CHUNK_SIZE) {
        chunk.size = MAX_CHUNK_SIZE;
        chunk = populateChunk(chunks, chunk);
        continue;
      }

      // Found interesting RFP, start a new chunk
      rfp = calculateRFP(chunk, t, rfp, m, d, q);
      if ((rfp & 0xFF) == 0) {
        // Handle zero run
        if (isZeroChunk(chunk, t)) {
          chunk.isZeroChunk = true;
          searchForZeroRun(chunk, t);
        }
        chunk = populateChunk(chunks, chunk);
        continue;
      }

      chunk.size++;
    }

    return chunks;
  }

  /**
   * Check if the current chunk is a zero chunk
   */
  public static boolean isZeroChunk(Chunk chunk, byte[] t) {
    for (int i = chunk.offset; i < chunk.offset + chunk.size; i++) {
      if (t[i] != 0) {
        return false;
      }
    }

    return true;
  }

  public static void searchForZeroRun(Chunk chunk, byte[] t) {
    int i = chunk.offset + chunk.size; // Start index

    while (t[i] == 0) {
      chunk.size++;
      i++;
    }
  }

  /**
   * Push {@code chunk} to the list and return a new chunk
   */
  public static Chunk populateChunk(ArrayList<Chunk> chunks, Chunk chunk) {
    chunks.add(chunk);
    return new Chunk(chunk.offset + chunk.size, MAX_CHUNK_SIZE);
  }

  public static int calculateRFP(Chunk chunk, byte[] t, int rfp, int m, int d, int q) {
    setQ(q);

    int dmMinus1ModQ = powerAndModQ(d, m - 1);
    // s starts from 0, i starts from 1
    // t_i = t[i - 1]
    // s = 0 (anchor point)
    if (chunk.offset == 0 && chunk.size == m) {
      rfp = 0;
      for (int i = 1; i <= m; i++) {
        rfp += modQ(modQ(t[i - 1]) * powerAndModQ(d, m - i));
      }
      rfp = modQ(rfp);
    } else {
      // rfp = p_s-1
      byte t_s = t[chunk.offset + chunk.size - m - 1];
      byte t_s_plus_m = t[chunk.offset + chunk.size - 1];
      int t_s_mod_q = modQ(t_s);
      int t_s_plus_m_mod_q = modQ(t_s_plus_m);

      // No need to perform modQ(rfp) as rfq < q
      rfp = modQ(rfp - modQ(dmMinus1ModQ * t_s_mod_q));
      rfp = modQ(modQ(d) * rfp);
      rfp = modQ(rfp + t_s_plus_m_mod_q);
    }

    return rfp;
  }

  public static void calculateHash(byte[] bytes, Chunk chunk) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(bytes, chunk.offset, chunk.size);
      chunk.fingerprint = String.format("%032x", new BigInteger(1, md.digest()));
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      exit(1);
    }
  }

  public static void download(String[] args) {
    checkArgsLength(args, 4);
    String fileToDownload = args[1];
    String localFileName = args[2];
  }

  public static void delete(String[] args) {
    checkArgsLength(args, 3);
    String fileToDelete = args[1];
  }

  public static void checkArgsLength(String[] args, int length) {
    if (args.length != length) {
      printUsage();
      exit(1);
    }
  }

  public static void printUsage() {
    System.out.println(
        "java java.MyDedup upload <min_chunk> <avg_chunk> <max_chunk> <d> <file_to_upload> <local|azure>");
    System.out
        .println("java java.MyDedup download <file_to_download> <local_file_name> <local|azure>");
    System.out.println("java java.MyDedup delete <file_to_delete> <local|azure>");
    exit(1);
  }
}
