package main.Handler;

import java.io.InputStream;
import java.nio.file.Path;

public interface StorageHandler {
  InputStream read(String path);
  InputStream read(Path path);
  void write(String path, InputStream stream);
  void write(Path path, InputStream stream);
  void delete(String path);
  void delete(Path path);
}

