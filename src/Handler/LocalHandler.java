package Handler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LocalHandler implements StorageHandler {
  private static final String directory = "data";

  @Override
  public InputStream read(String path) {
    Path mPath = Paths.get(directory, path);
    try {
      return Files.newInputStream(mPath);
    } catch (NoSuchFileException e) {
      return null;
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return null;
    }
  }

  @Override
  public InputStream read(Path path) {
    return read(path.toString());
  }

  @Override
  public void write(String path, InputStream stream) {
    Path mPath = Paths.get(directory, path);
    try {
      if (mPath.getParent() != null) {
        Files.createDirectories(mPath.getParent());
      }
      Files.copy(stream, mPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void write(Path path, InputStream stream) {
    write(path.toString(), stream);
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

  @Override
  public void delete(Path path) {
    delete(path.toString());
  }
}
