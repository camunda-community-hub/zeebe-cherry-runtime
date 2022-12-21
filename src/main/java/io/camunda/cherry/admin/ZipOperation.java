package io.camunda.cherry.admin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipOperation {

  ByteArrayOutputStream zipContent;
  ZipOutputStream zipOut;
String fileName;

  public ZipOperation(String zipFileName) {
    zipContent = new ByteArrayOutputStream();
    zipOut = new ZipOutputStream(zipContent);
    fileName = zipFileName;
  }

  /**
   * Add a file in the Zip
   * @param fileName file name
   * @param content content to add
   * @throws IOException in case of error
   */
  public void addZipContent(String fileName, String content) throws IOException {
    ZipEntry zipEntry = new ZipEntry(fileName);
    zipOut.putNextEntry(zipEntry);
    byte[] contentByte = content.getBytes(StandardCharsets.UTF_8);

    zipOut.write(contentByte, 0, contentByte.length);
  }

  public void close() throws IOException {
    zipOut.close();
  }

  public InputStream getInputStream() {
    return new ByteArrayInputStream(zipContent.toByteArray());
  }

  public byte[] getBytes() {
    return zipContent.toByteArray();
  }

  public String getFileName() {
    return fileName;
  }
}
