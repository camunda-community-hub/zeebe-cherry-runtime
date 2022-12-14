/* ******************************************************************** */
/*                                                                      */
/*  FileRepoFactory                                                 */
/*                                                                      */
/*  File can't be saved in C8. So, different implementation to store    */
/*  files are possible, and the factory give access to the different    */
/*  formats                                                             */
/*                                                                      */
/*  - FileVariable : the file (content included)                        */
/*  - FileVariableReference: reference to the file, to save as a        */
/*        process variable                                              */
/*  - StorageDefinition: information to access the storage.             */
/*  - FileVariableFactory : This class is the main API                  */
/*       createFileVariable(StorageDefinition) : create an empty file   */
/*       saveFileVariable( FileVariable ): save the file in the storage */
/*          the method return a FileVariableReference                   */
/*       loadFileVariable( FileVariableReference): load the file        */
/* ******************************************************************** */
package io.camunda.file.storage;

import java.util.Random;

public class FileRepoFactory {

  private final Random rand = new Random();

  public static FileRepoFactory getInstance() {
    return new FileRepoFactory();
  }

  /**
   * Create an empty File Variable.
   * StorageDefinition is not given: the JSON storage definition is used by default then
   *
   * @return a fileVariable, JSON type is the default
   */
  public FileVariable createFileVariable() {
    try {
      return new FileVariable(StorageDefinition.getFromString(StorageDefinition.StorageDefinitionType.JSON.toString()));
    }catch (Exception e) {
      // It should never have happened: JSON does not generate an exception
      return null;
    }
  }

  public FileVariable createFileVariable(StorageDefinition storageDefinition) {
    return new FileVariable(storageDefinition);
  }

  /**
   * get the FileVariable object from the different information
   * StorageDefinition is a string like
   * "JSON" : the value is a JSON information, to be unSerialize
   * "FOLDER:<path>", and the value is a file name in this directory.
   *
   * @param fileVariableReference information to access the file
   * @return a fileVariable
   * @throws Exception can't load the fileVariable
   */
  public FileVariable loadFileVariable(FileVariableReference fileVariableReference) throws Exception {
    if (fileVariableReference == null || fileVariableReference.content == null)
      return null;

    StorageDefinition storageDefinition = StorageDefinition.getFromString(fileVariableReference.storageDefinition);
    Storage storage = getStorage(storageDefinition);
    return storage.fromStorage(fileVariableReference);
  }

  /**
   * SetFileVariable
   *
   * @param fileVariable file Variable to save
   * @return the FileContainer (depends on the storageDefinition code)
   * @throws Exception if an error arrive
   */
  public FileVariableReference saveFileVariable(FileVariable fileVariable) throws Exception {
    if (fileVariable == null)
      return null;

    FileVariableReference fileVariableReference = new FileVariableReference();
    fileVariableReference.storageDefinition = fileVariable.getStorageDefinition().encodeToString();

    Storage storage = getStorage(fileVariable.getStorageDefinition());
    fileVariableReference = storage.toStorage(fileVariable, null);
    return fileVariableReference;
  }

  /**
   * Purge the fileVariable
   *
   * @param fileVariableReference reference to the file to purge
   * @return true if the file is correctly purge
   * @throws Exception if the purge failed
   */
  public boolean purgeFileVariable(FileVariableReference fileVariableReference) throws Exception {
    if (fileVariableReference == null)
      return true;

    StorageDefinition storageDefinition = StorageDefinition.getFromString(fileVariableReference.storageDefinition);
    Storage storage = getStorage(storageDefinition);
    return storage.purgeStorage(fileVariableReference);
  }

  /**
   * Generate an uniq Identifier for class who search for one
   *
   * @return a uniq ID
   */
  public String generateUniqId() {
    // get an uniq identifier
    return "_" + System.currentTimeMillis() + "_" + rand.nextInt(10000);
  }

  protected String getLoggerHeaderMessage(Class<?> clazz) {
    return "FileStorage." + clazz.getName() + ": ";

  }

  private Storage getStorage(StorageDefinition storageDefinition) throws Exception {
    return switch (storageDefinition.type) {
      case JSON -> new StorageJSON(storageDefinition, this);
      case FOLDER -> new StorageFolder(storageDefinition, this);
      case CMIS -> new StorageCMIS(storageDefinition, this);
      case TEMPFOLDER -> new StorageTempFolder(storageDefinition, this);
    };
   }
}
