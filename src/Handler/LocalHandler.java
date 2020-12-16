package Handler;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LocalHandler implements StorageHandler {

  @Override
  public InputStream read(String path) {
    return read(Paths.get(path));
  }

  @Override
  public InputStream read(Path path) {
    try {
      return Files.newInputStream(path);
    } catch (NoSuchFileException e) {
      return null;
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return null;
    }
  }

  @Override
  public void write(String path, InputStream stream) {
    write(Paths.get(path), stream);
  }

  public void write(Path path, InputStream stream) {
    try {
      if (path.getParent() != null) {
        Files.createDirectories(path);
      }
      Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void delete(String path) {
    delete(Paths.get(path));
  }

  @Override
  public void delete(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
