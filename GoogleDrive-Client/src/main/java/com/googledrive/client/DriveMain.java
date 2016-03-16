package com.googledrive.client;


import com.google.api.services.drive.model.File;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DriveMain {

  private static final String FOLDER = "Reports";
  private static final String UPLOAD_FILE = "Report";


  public static void main(String[] args) {

    /**
     * Run the program in a single thread that will be executed in background
     */
    Executor executor = Executors.newSingleThreadExecutor();
    executor.execute(new Runnable() {
      @Override
      public void run() {
        try {
          DriveWrapper drive = new DriveWrapper();

          File folder;
          if ((folder = drive.getFolder(FOLDER)) == null) {
            folder = drive.createFolder(FOLDER);
          }

          StringBuilder sb = new StringBuilder();
          drive.buildReport(sb);

          java.io.File report = new java.io.File(UPLOAD_FILE);
          Files.write(sb, report, Charsets.UTF_8);

          drive.uploadFile(UPLOAD_FILE, report, folder.getId(), true);

          System.out.println("Success!");
        } catch (IOException e) {
          System.out.println("Error: " + e.getMessage());
        }
      }
    });

  }
}
