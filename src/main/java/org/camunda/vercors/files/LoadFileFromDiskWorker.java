/* ******************************************************************** */
/*                                                                      */
/*  LoadFileFromDiskWorker                                              */
/*                                                                      */
/* Load a file from disk to the process.                                */
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
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LoadFileFromDiskWorker extends AbstractWorker {

    public static final String BPMNERROR_FOLDER_NOT_EXIST_ERROR = "FOLDER_NOT_EXIST_ERROR";
    public static final String BPMNERROR_LOAD_FILE_ERROR = "LOAD_FILE_ERROR";
    public static final String BPMNERROR_MOVE_FILE_ERROR = "MOVE_FILE_ERROR";
    public static final String POLICY_V_DELETE = "DELETE";
    public static final String POLICY_V_ARCHIVE = "ARCHIVE";
    public static final String POLICY_V_UNCHANGE = "UNCHANGE";
    private final static String INPUT_FOLDER = "folder";
    private final static String INPUT_FILTER_FILE = "filterFile";
    private final static String INPUT_FILE_NAME = "fileName";
    private final static String INPUT_POLICY = "policy";
    private final static String INPUT_STORAGEDEFINITION = "storageDefinition";
    private final static String INPUT_ARCHIVE_FOLDER = "archiveFolder";
    private final static String OUTPUT_FILE_LOADED = "fileLoaded";
    private final static String OUTPUT_FILE_NAME = "fileNameLoaded";
    private final static String OUTPUT_FILE_MIMETYPE = "fileMimeType";
    Logger logger = LoggerFactory.getLogger(LoadFileFromDiskWorker.class.getName());

    public LoadFileFromDiskWorker() {
        super("v-files-load-from-disk",
                Arrays.asList(
                        AbstractWorker.WorkerParameter.getInstance(INPUT_FOLDER, String.class, AbstractWorker.Level.REQUIRED, "Give the folder where the file will be loaded"),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_FILE_NAME, String.class, AbstractWorker.Level.OPTIONAL, "Specify a file name, else the first file in the folder will be loaded"),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_FILTER_FILE, String.class, AbstractWorker.Level.OPTIONAL, "If you didn't specify a fileName, a filter to select only part of files present in the directory"),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_POLICY, String.class, AbstractWorker.Level.OPTIONAL,
                                "Policy to manipulate the file after loading. Policy are " + POLICY_V_DELETE + ", " + POLICY_V_ARCHIVE + " (then specify the folder archive), " + POLICY_V_UNCHANGE),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_STORAGEDEFINITION, String.class, FileVariableFactory.FileVariableStorage.JSON.toString(), Level.OPTIONAL, "How to saved the FileVariable"),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_ARCHIVE_FOLDER, String.class, AbstractWorker.Level.OPTIONAL, "Folder used with policy " + POLICY_V_ARCHIVE)),

                Arrays.asList(
                        AbstractWorker.WorkerParameter.getInstance(OUTPUT_FILE_LOADED, Object.class, Level.REQUIRED, "Name of fileVariable where the file is saved"),
                        AbstractWorker.WorkerParameter.getInstance(OUTPUT_FILE_NAME, String.class, Level.REQUIRED, "File Name"),
                        AbstractWorker.WorkerParameter.getInstance(OUTPUT_FILE_MIMETYPE, String.class, AbstractWorker.Level.REQUIRED, "MimeType of the loaded file")),
                Arrays.asList(BPMNERROR_FOLDER_NOT_EXIST_ERROR, BPMNERROR_LOAD_FILE_ERROR, BPMNERROR_MOVE_FILE_ERROR)
        );
    }

    @Override
    @ZeebeWorker(type = "v-files-load-from-disk", autoComplete = true)
    public void handleWorkerExecution(final JobClient jobClient, final ActivatedJob activatedJob) {
        super.handleWorkerExecution(jobClient, activatedJob);
    }


    @Override
    public void execute(final JobClient client, final ActivatedJob activatedJob, ContextExecution contextExecution) {
        String folderName = getInputStringValue(INPUT_FOLDER, null, activatedJob);
        String fileName = getInputStringValue(INPUT_FILE_NAME, null, activatedJob);
        String filterFile = getInputStringValue(INPUT_FILTER_FILE, null, activatedJob);
        String policy = getInputStringValue(INPUT_POLICY, null, activatedJob);
        String storageDefinition = getInputStringValue(INPUT_STORAGEDEFINITION, null, activatedJob);
        String archiveFolder = getInputStringValue(INPUT_ARCHIVE_FOLDER, null, activatedJob);

        FileVariable fileVariable = null;

        File folder = new File(folderName);
        if (!(folder.exists() && folder.isDirectory())) {
            logger.error(getName() + ": folder[" + folder.getAbsolutePath() + "] does not exist ");
            throw new ZeebeBpmnError(BPMNERROR_FOLDER_NOT_EXIST_ERROR, "Worker [" + getName() + "] folder[" + folder.getAbsolutePath() + "] does not exist");
        }
        List<File> listFilesFiltered;
        File fileToProcess = null;
        if (folder.listFiles() == null) {
            listFilesFiltered = Collections.emptyList();
        } else {
            listFilesFiltered = Arrays.stream(folder.listFiles())
                    .filter(t -> {
                        if (fileName != null)
                            return t.getName().equals(fileName);
                        if (filterFile == null || filterFile.isEmpty())
                            return true;
                        return t.getName().matches(filterFile);
                    })
                    .collect(Collectors.toList());
        }
        if (listFilesFiltered.isEmpty()) {
            logger.info(getName() + ": folder [" + folder.getAbsolutePath() + "] does not have any matching file "
                    + (fileName != null ? "fileName[" + fileName + "]" : "FilterFile[" + filterFile + "]"));
        } else {
            // load the first file only
            fileToProcess = listFilesFiltered.get(0);
            fileVariable = new FileVariable();
            fileVariable.value = new byte[(int) fileToProcess.length()];
            fileVariable.name = fileToProcess.getName();
            fileVariable.mimeType = FileVariable.getMimeTypeFromName(fileVariable.name);

            try (FileInputStream fis = new FileInputStream(fileToProcess)) {
                fis.read(fileVariable.value);
            } catch (Exception e) {
                logger.error(getName() + ": cannot read file[" + fileToProcess.getAbsolutePath() + "] : " + e);
                throw new ZeebeBpmnError(BPMNERROR_LOAD_FILE_ERROR, "Worker [" + getName() + "]  cannot read file[" + fileToProcess.getAbsolutePath() + "] : " + e);
            }
        }

        // output
        if (fileVariable != null) {
            setFileVariableValue(OUTPUT_FILE_LOADED, storageDefinition, fileVariable, contextExecution);
            setValue(OUTPUT_FILE_NAME, fileVariable.name, contextExecution);
            setValue(OUTPUT_FILE_MIMETYPE, fileVariable.mimeType, contextExecution);
        } else {
            setValue(OUTPUT_FILE_LOADED, null, contextExecution);
            setValue(OUTPUT_FILE_NAME, null, contextExecution);
            setValue(OUTPUT_FILE_MIMETYPE, null, contextExecution);

        }

        if (fileToProcess != null) {
            // according to the policy, move the file
            if (POLICY_V_UNCHANGE.equals(policy)) {
                // Nothing to do here
            } else if (POLICY_V_DELETE.equals(policy)) {
                fileToProcess.delete();
            } else if (POLICY_V_ARCHIVE.equals(policy)) {
                Path source = Paths.get(fileToProcess.getAbsolutePath());
                Path target = Paths.get(archiveFolder + "/" + fileToProcess.getName());

                try {
                    // rename or move a file to other path
                    // if target exists, throws FileAlreadyExistsException
                    Files.move(source, target);
                } catch (Exception e) {
                    logger.error(getName() + ": cannot apply the policy[" + policy + "] from source[" + source + "] to [" + target + "] : " + e);
                    throw new ZeebeBpmnError(BPMNERROR_MOVE_FILE_ERROR, "Worker [" + getName() + "] cannot apply the policy[" + policy + "] from source[" + source + "] to [" + target + "] : " + e);
                }
            }
        }
    }
}
