/* ******************************************************************** */
/*                                                                      */
/*  SaveFileFromDiskWorker                                              */
/*                                                                      */
/* Save a file from the process to the disk                             */
/* C8 does not manage a file type, so there is different implementation */
/* @see FileVariableFactory                                             */
/* ******************************************************************** */
package org.camunda.vercors.files;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import io.camunda.zeebe.spring.client.exception.ZeebeBpmnError;
import org.camunda.vercors.definition.AbstractWorker;
import org.camunda.vercors.definition.filevariable.FileVariable;
import org.camunda.vercors.definition.filevariable.FileVariableFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

@Component
public class SaveFileToDiskWorker extends AbstractWorker {

    public static final String BPMNERROR_LOAD_FILE_ERROR = "LOAD_FILE_ERROR";
    public static final String BPMNERROR_FOLDER_NOT_EXIST_ERROR = "FOLDER_NOT_EXIST_ERROR";
    public static final String BPMNERROR_WRITE_FILE_ERROR = "WRITE_FILE_ERROR";
    private final static String INPUT_FOLDER_TO_SAVE = "folder";
    private final static String INPUT_FILENAME = "fileName";
    private final static String INPUT_SOURCE_FILE = "sourceFile";
    private final static String INPUT_STORAGEDEFINITION = "storageDefinition";
    Logger logger = LoggerFactory.getLogger(SaveFileToDiskWorker.class.getName());


    public SaveFileToDiskWorker() {
        super("v-files-save-to-disk",
                Arrays.asList(
                        AbstractWorker.WorkerParameter.getInstance(INPUT_FOLDER_TO_SAVE, String.class, AbstractWorker.Level.REQUIRED, "Folder where the file will be save"),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_FILENAME, String.class, Level.OPTIONAL, "File name used to save the file. If not provided, fileVariable name is used"),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_SOURCE_FILE, Object.class, Level.REQUIRED, "FileVariable used to save"),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_STORAGEDEFINITION, String.class, FileVariableFactory.FileVariableStorage.JSON.toString(), Level.OPTIONAL, "Storage Definition use to access the file")
                ),
                Collections.emptyList(),
                Arrays.asList(BPMNERROR_LOAD_FILE_ERROR, BPMNERROR_FOLDER_NOT_EXIST_ERROR, BPMNERROR_WRITE_FILE_ERROR));
    }

    @Override
    @ZeebeWorker(type = "v-files-save-to-disk", autoComplete = true)
    public void handleWorkerExecution(final JobClient jobClient, final ActivatedJob activatedJob) {
        super.handleWorkerExecution(jobClient, activatedJob);
    }

    @Override
    public void execute(final JobClient client, final ActivatedJob activatedJob, ContextExecution contextExecution) {
        String folderToSave = getInputStringValue(INPUT_FOLDER_TO_SAVE, null, activatedJob);
        String sourceStorageDefinition = getInputStringValue(INPUT_STORAGEDEFINITION, null, activatedJob);
        FileVariable fileVariable = getFileVariableValue(INPUT_SOURCE_FILE, sourceStorageDefinition, activatedJob);

        String fileName = getInputStringValue(INPUT_FILENAME, null, activatedJob);

        if (fileVariable == null) {
            logError("File behind input[" + INPUT_SOURCE_FILE + "] does not exist ");
            throw new ZeebeBpmnError(BPMNERROR_LOAD_FILE_ERROR, "Worker [" + getName() + "] file behind input[" + INPUT_SOURCE_FILE + "] does not exist");

        }
        File folder = new File(folderToSave);
        if (!(folder.exists() && folder.isDirectory())) {
            logError("Folder[" + folder.getAbsolutePath() + "] does not exist ");
            throw new ZeebeBpmnError(BPMNERROR_FOLDER_NOT_EXIST_ERROR, "Worker [" + getName() + "] folder[" + folder.getAbsolutePath() + "] does not exist");
        }

        try {
            Path file = Paths.get(folder.getAbsolutePath() + FileSystems.getDefault().getSeparator() + (fileName == null ? fileVariable.name : fileName));
            Files.write(file, fileVariable.value);
            logInfo("Write file[" + file + "]");
        } catch (Exception e) {
            logError("Cannot save to folder[" + folderToSave + "] : " + e);
            throw new ZeebeBpmnError(BPMNERROR_WRITE_FILE_ERROR, "Worker [" + getName() + "] cannot save to folder[" + folderToSave + "] :" + e);
        }
    }
}