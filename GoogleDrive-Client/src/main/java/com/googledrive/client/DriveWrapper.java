package com.googledrive.client;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.Create;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.base.Strings;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DriveWrapper {

  /** Application name */
  private static final String APPLICATION_NAME = "GDriveClient";
  private static final String DATE_FORMAT = "yyyy-MM-dd_HH:mm:ss";

  /** Directory to store user credentials for this application */
  private static final java.io.File DATA_STORE_DIR =
      new java.io.File(System.getProperty("user.home"), ".credentials/drive.json");

  private static final String SECRET = "/client_id.json";

  /** Global instance of the FileDataStoreFactory */
  private static FileDataStoreFactory data_store_factory;
  /** Global instance of the HTTP transport */
  private static HttpTransport http_transport;
  /** Global instance of the JSON factory */
  private JsonFactory json_factory = JacksonFactory.getDefaultInstance();

  private Drive service;

  public DriveWrapper() {
    try {
      http_transport = GoogleNetHttpTransport.newTrustedTransport();
      data_store_factory = new FileDataStoreFactory(DATA_STORE_DIR);

      // Build a new authorized API client service.
      service = getDriveService();
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Creates an authorized Credential object.
   */
  public Credential authorize() throws IOException {
    // Load client secrets.
    InputStream in = DriveWrapper.class.getResourceAsStream(SECRET);
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(json_factory, new InputStreamReader(in));

    /// set up authorization code flow
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(http_transport,
        json_factory, clientSecrets, Collections.singleton(DriveScopes.DRIVE))
            .setDataStoreFactory(data_store_factory).build();

    Credential credential =
        new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    System.out.println("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
    return credential;
  }

  /**
   * Build and return an authorized Drive client service.
   */
  public Drive getDriveService() throws IOException {
    Credential credential = authorize();
    return new Drive.Builder(http_transport, json_factory, credential)
        .setApplicationName(APPLICATION_NAME).build();
  }


  /**
   * Upload a file
   */
  public File uploadFile(String name, java.io.File report, String parent, boolean useDirectUpload)
      throws IOException {
    File fileMetadata = new File();
    fileMetadata.setName(getDate() + "_" + name);

    List<String> parents = new ArrayList<String>();
    parents.add(parent);

    fileMetadata.setParents(parents);
    FileContent mediaContent = new FileContent("text/html", report);

    Create insert = service.files().create(fileMetadata, mediaContent);
    MediaHttpUploader uploader = insert.getMediaHttpUploader();
    uploader.setDirectUploadEnabled(useDirectUpload);
    uploader.setProgressListener(new FileUploadProgressListener());
    return insert.execute();
  }

  /**
   * Create folder
   */
  public File createFolder(String name) throws IOException {
    File folder = new File();
    folder.setName(name);
    folder.setMimeType("application/vnd.google-apps.folder");

    Create insert = service.files().create(folder);
    MediaHttpUploader uploader = insert.getMediaHttpUploader();
    return insert.execute();
  }

  /**
   * Get folder, used to check if folder exist already
   */
  public File getFolder(String name) throws IOException {
    Drive.Files.List request;
    request = service.files().list();
    String query = "mimeType='application/vnd.google-apps.folder' AND trashed=false AND name='"
        + name + "' AND 'root' in parents";

    request = request.setQ(query);
    FileList files = request.execute();
    if (files.getFiles().size() == 0) // if the size is zero, then the folder doesn't exist
      return null;
    else
      // since google drive allows to have multiple folders with the same title (name)
      // we select the first file in the list to return
      return files.getFiles().get(0);
  }


  private static String getDate() {
    DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
    Date date = new Date();
    return dateFormat.format(date);
  }

  /**
   * Build report
   */
  public void buildReport(StringBuilder sb) throws IOException {
    buildReport(sb, "root", 0);
  }

  /**
   * Recursive method that append the name of the files in a StringBuilder. It's formatted by level
   * of depth so that the files inside a folder will appear indented
   */
  public void buildReport(StringBuilder sb, String parent, int level) throws IOException {
    com.google.api.services.drive.Drive.Files.List request =
        service.files().list().setQ("'" + parent + "' in parents and trashed=false");

    do {
      try {
        FileList files = request.execute();

        List<File> result = files.getFiles();
        request.setPageToken(files.getNextPageToken());

        if (result != null && result.size() > 0) {
          for (File file : result) {
            sb.append(Strings.repeat("\t", level) + file.getName() + "\n");
            buildReport(sb, file.getId(), level + 1);
          }
        }

      } catch (IOException e) {
        System.out.println("An error occurred: " + e);
        request.setPageToken(null);
      }
    } while (request.getPageToken() != null && request.getPageToken().length() > 0);

  }
}
