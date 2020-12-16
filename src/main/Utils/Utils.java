package main.Utils;

import static java.lang.System.exit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class Utils {
  static int q;

  public static File getOrCreateIndexFile() {
    File index = new File("mydedup.index");
    try {
      index.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
      exit(1);
    }
    return index;
  }

  public static byte[] readFile(Path path) {
    byte[] b = {};
    try {
      b = Files.readAllBytes(path);
    } catch (NoSuchFileException e) {
      System.out.println("File " + path + "does not exist");
      exit(1);
    } catch (IOException e) {
      e.printStackTrace();
      exit(1);
    }
    return b;
  }

  public static int modQ(int dividend) {
    return dividend & (q - 1);
  }

  public static int powerAndModQ(int d, int power) {
    return modQ((int) Math.pow(d, power));
  }

  public static void setQ(int q) {
    Utils.q = q;
  }
}
