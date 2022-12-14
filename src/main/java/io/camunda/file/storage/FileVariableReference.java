/* ******************************************************************** */
/*                                                                      */
/*  FileVariableReference                                               */
/*                                                                      */
/*  This object carry the reference to a file variable, not the file    */
/*  itself. This is used as a process variable.                         */
/*  The content may be retrieved via the FileVariableFactory            */
/*                                                                      */
/* ******************************************************************** */
package io.camunda.file.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A FileContainer must be self-description. So, it contains the way to find the file itself (JSON, FOLDER...) and the information to access the content
 */
public class FileVariableReference {
  private static final Logger logger = LoggerFactory.getLogger(FileVariableReference.class.getName());

  /**
   * Storage definition where the file is. It is a String to simplify the encoding.
   */
  public String storageDefinition;
  /**
   * content to retrieve the file in the storageDefinition (key to access it)
   */
  public Object content;

  /**
   * Transform the reference from JSON
   *
   * @param fileReferenceJson file Reference in JSON
   * @return a FileVariableReference
   * @throws Exception when an error arrive
   */
  public static FileVariableReference fromJson(String fileReferenceJson) throws Exception {
    try {
      return new ObjectMapper().readValue(fileReferenceJson, FileVariableReference.class);
    } catch (JsonProcessingException e) {
      logger.error("FileStorage.FileVariableReference.fromJson: exception " + e + " During un serialize fileVariable");
      throw e;
    }
  }

  /**
   * Transform the fileReference to JSON
   *
   * @return the Json
   * @throws JsonProcessingException in any error
   */
  public String toJson() throws JsonProcessingException {
    try {
      return new ObjectMapper().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      logger.error("FileStorage.FileVariableReference.toJson: exception " + e + " During serialize fileVariable");
      throw e;
    }
  }

  public String getStorageDefinition() {
    return storageDefinition;
  }

  public Object getContent() {
    return content;
  }

  /**
   * Must be static to not make any trouble in the serialization/deserialization
   * @param fieldReference field reference to get the identification
   * @return an indentification, to log it for example
   */
  public static String getIdentification(FileVariableReference fieldReference) {
      StringBuilder result = new StringBuilder();
      result.append(fieldReference.storageDefinition);
      result.append( ": ");
      if (fieldReference.content==null)
          result.append("null");
      else
          result.append((fieldReference.content.toString() + "                  ").substring(0, 50));
      return result.toString();
  }
}
