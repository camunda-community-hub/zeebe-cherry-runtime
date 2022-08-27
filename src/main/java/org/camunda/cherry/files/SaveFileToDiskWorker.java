/* ******************************************************************** */
/*                                                                      */
/*  SaveFileFromDiskWorker                                              */
/*                                                                      */
/* Save a file from the process to the disk                             */
/* C8 does not manage a file type, so there is different implementation */
/* @see FileVariableFactory                                             */
/* ******************************************************************** */
package org.camunda.cherry.files;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import io.camunda.zeebe.spring.client.exception.ZeebeBpmnError;
import org.camunda.cherry.definition.AbstractWorker;
import org.camunda.cherry.definition.filevariable.FileVariable;
import org.camunda.cherry.definition.filevariable.FileVariableFactory;
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

    /**
     * Worker type
     */
    private static final String WORKERTYPE_FILES_SAVE_TO_DISK = "c-files-save-to-disk";


    private static final String BPMNERROR_LOAD_FILE_ERROR = "LOAD_FILE_ERROR";
    private static final String BPMNERROR_FOLDER_NOT_EXIST_ERROR = "FOLDER_NOT_EXIST_ERROR";
    private static final String BPMNERROR_WRITE_FILE_ERROR = "WRITE_FILE_ERROR";
    private static final String INPUT_FOLDER_TO_SAVE = "folder";
    private static final String INPUT_FILENAME = "fileName";
    private static final String INPUT_SOURCE_FILE = "sourceFile";


    public SaveFileToDiskWorker() {
        super(WORKERTYPE_FILES_SAVE_TO_DISK,
                Arrays.asList(
                        AbstractWorker.WorkerParameter.getInstance(INPUT_FOLDER_TO_SAVE, "Folder to save", String.class, AbstractWorker.Level.REQUIRED, "Folder where the file will be save"),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_FILENAME, "File name", String.class, Level.OPTIONAL, "File name used to save the file. If not provided, fileVariable name is used"),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_SOURCE_FILE, "Source file", Object.class, Level.REQUIRED, "FileVariable used to save")
                ),
                Collections.emptyList(),
                Arrays.asList(
                        AbstractWorker.BpmnError.getInstance(BPMNERROR_LOAD_FILE_ERROR, "Load file error"),
                        AbstractWorker.BpmnError.getInstance(BPMNERROR_FOLDER_NOT_EXIST_ERROR, "Folder does not exist, or not visible from the server"),
                        AbstractWorker.BpmnError.getInstance(BPMNERROR_WRITE_FILE_ERROR, "Error during writing the file"),
                        AbstractWorker.BpmnError.getInstance(FileVariableFactory.BPMNERROR_INCORRECT_STORAGEDEFINITION, "Incorrect storage definition")));
    }

    @Override

    @ZeebeWorker(type = WORKERTYPE_FILES_SAVE_TO_DISK, autoComplete = true)
    public void handleWorkerExecution(final JobClient jobClient, final ActivatedJob activatedJob) {
        super.handleWorkerExecution(jobClient, activatedJob);
    }

    @Override
    public void execute(final JobClient client, final ActivatedJob activatedJob, ContextExecution contextExecution) {

        String folderToSave = getInputStringValue(INPUT_FOLDER_TO_SAVE, null, activatedJob);
        FileVariable fileVariable = getFileVariableValue(INPUT_SOURCE_FILE, activatedJob);

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