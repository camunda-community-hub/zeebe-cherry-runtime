/* ******************************************************************** */
/*                                                                      */
/*  FileVariableTempFolder                                              */
/*                                                                      */
/*  Save a file variable in the temporary folder of the host            */
/*  Attention, this is the temporary folder where the worker is running */
/* ******************************************************************** */
package io.camunda.file.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StorageTempFolder extends Storage {
  Logger logger = LoggerFactory.getLogger(StorageTempFolder.class.getName());

  private FileRepoFactory fileRepoFactory;

  protected StorageTempFolder(StorageDefinition storageDefinition, FileRepoFactory fileRepoFactory) {
    super(storageDefinition, fileRepoFactory);
  }

  @Override
  public String getName() {
    return "TempFolder";
  }

  public static String getStorageDefinitionString( ) {
    return StorageDefinition.StorageDefinitionType.TEMPFOLDER.toString();
  }
  /**
   * Save the file Variable structure in the temporary folder
   *
   * @param fileVariable          fileVariable to save it
   * @param fileVariableReference file variable to update (may be null)
   */
  public FileVariableReference toStorage(FileVariable fileVariable, FileVariableReference fileVariableReference)
      throws Exception {
    Path tempPath = null;
    try {
      String fileName = fileVariable.getName();
      String suffix = "";
      int lastDot = fileVariable.getName().lastIndexOf(".");
      if (lastDot != -1) {
        fileName = fileVariable.getName().substring(0, lastDot) + "_";
        suffix = fileVariable.getName().substring(lastDot + 1);
      }

      tempPath = Files.createTempFile(fileName, "." + suffix);
      Files.write(tempPath, fileVariable.getValue());

      FileVariableReference fileVariableReferenceOutput = new FileVariableReference();
      fileVariableReferenceOutput.storageDefinition = getStorageDefinition().encodeToString();
      fileVariableReferenceOutput.content = tempPath.getFileName().toString();
      return fileVariableReferenceOutput;

    } catch (Exception e) {
      logger.error(fileRepoFactory.getLoggerHeaderMessage(StorageTempFolder.class) + "Exception " + e
          + " During write fileVariable on tempFolder[" + tempPath + "]");
      throw e;
    }
  }

  /**
   * read the fileVariable
   *
   * @param fileVariableReference name of the file in the temporary directory
   * @return the fileVariable object
   * @throws Exception during the writing
   */
  public FileVariable fromStorage(FileVariableReference fileVariableReference) throws Exception {
    String tempFilePath;
    try {
      // get the temporary path
      Path tempPath = Files.createTempFile("", "");
      String separator = FileSystems.getDefault().getSeparator();
      tempFilePath = tempPath.toString().substring(0, tempPath.toString().lastIndexOf(separator));

      FileVariable fileVariable = new FileVariable(getStorageDefinition());
      fileVariable.setName(fileVariableReference.content.toString());
      fileVariable.setMimeType(FileVariable.getMimeTypeFromName(fileVariableReference.content.toString()));
      fileVariable.setValue(Files.readAllBytes(Paths.get(tempFilePath + separator + fileVariableReference.content.toString())));
      return fileVariable;

    } catch (Exception e) {
      logger.error(
          fileRepoFactory.getLoggerHeaderMessage(StorageTempFolder.class) + "Exception " + e + " During read file["
              + fileVariableReference.content.toString() + "] in temporaryPath[" + fileVariableReference.content.toString() + "]");
      throw e;
    }
  }

  /**
   * Delete the file
   *
   * @param fileVariableReference          name of the file in the temporary directory
   * @return true if the operation was successful
   */
  public boolean purgeStorage( FileVariableReference fileVariableReference) {

    String tmpDir = System.getProperty("java.io.tmpdir");
    File file = new File(tmpDir + FileSystems.getDefault().getSeparator() + fileVariableReference.getContent().toString());
    if (file.exists())
      return file.delete();
    return true;
  }

}
