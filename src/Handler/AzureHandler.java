package Handler;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AzureHandler implements StorageHandler {

  private static final String directory = "data";

  // TODO: Uncomment for submission
//  static {
//    System.setProperty("https.proxyHost", "proxy.cse.cuhk.edu.hk");
//    System.setProperty("https.proxyPort", "8000");
//    System.setProperty("http.proxyHost", "proxy.cse.cuhk.edu.hk");
//    System.setProperty("http.proxyPort", "8000");
//  }

  public static final String storageConnectionString =
      "DefaultEndpointsProtocol=https;"
          + "AccountName=csci4180group50;"
          + "AccountKey=;"
          + "EndpointSuffix=core.windows.net";

  public CloudBlobContainer container;

  public AzureHandler() {
    try {
      CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
      CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
      container = blobClient.getContainerReference("dedup");
      container.createIfNotExists();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public InputStream read(String path) {
    Path mPath = Paths.get(directory, path);
    try {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      CloudBlockBlob blob = container.getBlockBlobReference(mPath.toString());
      blob.download(stream);
      stream.close();
      return new ByteArrayInputStream(stream.toByteArray());
    } catch (StorageException e) {
      return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
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
      CloudBlockBlob blob = container.getBlockBlobReference(mPath.toString());
      blob.upload(stream, stream.available());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void write(Path path, InputStream stream) {
    write(path.toString(), stream);
  }

  @Override
  public void delete(String path) {
    Path mPath = Paths.get(directory, path);
    try {
      CloudBlockBlob blob = container.getBlockBlobReference(mPath.toString());
      blob.deleteIfExists();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void delete(Path path) {
    delete(path.toString());
  }
}
