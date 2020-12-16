import static Utils.Utils.modQ;
import static Utils.Utils.powerAndModQ;
import static Utils.Utils.setQ;
import static java.lang.System.exit;

import java.nio.file.Path;

public class MyDedup {

  public static void main(String[] args) {
    String mode = args[0];

    if (mode.equals("upload")) {
      System.out.println("upload");
    } else if (mode.equals("download")) {
      System.out.println("download");
    } else if (mode.equals("delete")) {
      System.out.println("delete");
    } else {
      System.out.println("usage");
    }
  }

  public static void upload(String[] args) {
    checkArgsLength(args, 7);
    checkStorageLocation(args[6]);
    int minChunk = Integer.parseInt(args[1]);
    int avgChunk = Integer.parseInt(args[2]);
    int maxChunk = Integer.parseInt(args[3]);
    int d = Integer.parseInt(args[4]);
    Path pathname = Path.of(args[5]);
    boolean local = args[6].equals("local");

    if (
        Math.log(minChunk) / Math.log(2) % 1 != 0 ||
            Math.log(avgChunk) / Math.log(2) % 1 != 0 ||
            Math.log(maxChunk) / Math.log(2) % 1 != 0
    ) {
      System.out.println("chunks needs to be a power of 2");
      exit(1);
    }
  }

  public static int calculateRFP(Chunk chunk, byte[] t, int rfp, int m, int d, int q) {
    setQ(q);

    int dmMinus1ModQ = powerAndModQ(d, m - 1);
    // s starts from 0, i starts from 1
    // t_i = t[i - 1]
    // s = 0 (anchor point)
    if (chunk.offset == 0 && chunk.size == m) {
      for (int i = 1; i <= m; i++) {
        rfp += modQ( modQ(t[i - 1]) * powerAndModQ(d, m - i) );
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

  public static void download(String[] args) {
    checkArgsLength(args, 4);
    checkStorageLocation(args[3]);
    String fileToDownload = args[1];
    String localFileName = args[2];
    boolean local = args[3].equals("local");
  }

  public static void delete(String[] args) {
    checkArgsLength(args, 3);
    checkStorageLocation(args[2]);
    String fileToDelete = args[1];
    boolean local = args[2].equals("local");
  }

  public static void checkArgsLength(String[] args, int length) {
    if (args.length != length) {
      printUsage();
      exit(1);
    }
  }

  public static void checkStorageLocation(String location) {
    if (!location.equals("local") && !location.equals("azure")) {
      printUsage();
      exit(1);
    }
  }

  public static void printUsage() {
    System.out.println(
        "java MyDedup upload <min_chunk> <avg_chunk> <max_chunk> <d> <file_to_upload> <local|azure>");
    System.out.println("java MyDedup download <file_to_download> <local_file_name> <local|azure>");
    System.out.println("java MyDedup delete <file_to_delete> <local|azure>");
  }
}
