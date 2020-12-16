import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

interface StorageHandler {
  public InputStream read(String path);
  public void write(String path, InputStream stream);
  public void delete(String path);

  public static class LocalHandler implements StorageHandler {

    static String directory = "data";

    @Override
    public InputStream read(String path) {
      Path mPath = Paths.get(directory, path);
      try(InputStream stream = Files.newInputStream(mPath)) {
        return stream;
      } catch(IOException ioe) {
        ioe.printStackTrace();
        return null;
      }
    }

    @Override
    public void write(String path, InputStream stream) {
      Path mPath = Paths.get(directory, path);
      File file = mPath.toFile();
      if (!file.exists()) {
        try {
          Files.copy(stream, mPath);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    @Override
    public void delete(String path) {
      Path mPath = Paths.get(directory, path);
      try {
        Files.deleteIfExists(mPath);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}

