/* ******************************************************************** */
/*                                                                      */
/*  LoadFileFromDiskWorker                                              */
/*                                                                      */
/* Load a file from disk to the process.                                */
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

    /**
     * Worker type
     */
    private static final String WORKERTYPE_FILES_LOAD_FROM_DISK = "c-files-load-from-disk";


    private static final String BPMNERROR_FOLDER_NOT_EXIST_ERROR = "FOLDER_NOT_EXIST_ERROR";
    private static final String BPMNERROR_LOAD_FILE_ERROR = "LOAD_FILE_ERROR";
    private static final String BPMNERROR_MOVE_FILE_ERROR = "MOVE_FILE_ERROR";
    private static final String POLICY_V_DELETE = "DELETE";
    private static final String POLICY_V_ARCHIVE = "ARCHIVE";
    private static final String POLICY_V_UNCHANGE = "UNCHANGE";
    private static final String INPUT_FOLDER = "folder";
    private static final String INPUT_FILTER_FILE = "filterFile";
    private static final String INPUT_FILE_NAME = "fileName";
    private static final String INPUT_POLICY = "policy";
    private static final String INPUT_STORAGEDEFINITION = "storageDefinition";
    private static final String INPUT_STORAGEDEFINITION_COMPLEMENT = "storageDefinitionComplement";
    private static final String INPUT_ARCHIVE_FOLDER = "archiveFolder";
    private static final String OUTPUT_FILE_LOADED = "fileLoaded";
    private static final String OUTPUT_FILE_NAME = "fileNameLoaded";
    private static final String OUTPUT_FILE_MIMETYPE = "fileMimeType";
    private final Logger logger = LoggerFactory.getLogger(LoadFileFromDiskWorker.class.getName());

    public LoadFileFromDiskWorker() {
        super(WORKERTYPE_FILES_LOAD_FROM_DISK,
                Arrays.asList(
                        AbstractWorker.WorkerParameter.getInstance(INPUT_FOLDER, "Folder", String.class, AbstractWorker.Level.REQUIRED, "Specify the folder where the file will be loaded. Must be visible from the server."),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_FILE_NAME, "File name", String.class, AbstractWorker.Level.OPTIONAL, "Specify a file name, else the first file in the folder will be loaded"),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_FILTER_FILE, "Filter file", String.class, AbstractWorker.Level.OPTIONAL, "If you didn't specify a fileName, a filter to select only part of files present in the folder"),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_POLICY, "Policy", String.class, AbstractWorker.Level.OPTIONAL,
                                        "Policy to manipulate the file after loading. With " + POLICY_V_ARCHIVE + ", the folder archive must be specify")
                                .addChoice("DELETE", POLICY_V_DELETE)
                                .addChoice("ARCHIVE", POLICY_V_ARCHIVE)
                                .addChoice("UNCHANGE", POLICY_V_UNCHANGE)
                        ,
                        AbstractWorker.WorkerParameter.getInstance(INPUT_ARCHIVE_FOLDER, "Archive folder", String.class, AbstractWorker.Level.OPTIONAL, "With the policy " + POLICY_V_ARCHIVE + ". File is moved in this folder.")
                                .addCondition(INPUT_POLICY, Arrays.asList(POLICY_V_ARCHIVE)),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_STORAGEDEFINITION, "Storage definition", String.class, FileVariableFactory.FileVariableStorage.JSON.toString(), Level.OPTIONAL,
                                        "How to saved the FileVariable. "
                                                + FileVariableFactory.FileVariableStorage.JSON + " to save in the engine (size is linited), "
                                                + FileVariableFactory.FileVariableStorage.TEMPFOLDER + " to use the temporary folder of THIS machine"
                                                + FileVariableFactory.FileVariableStorage.FOLDER + " to specify a folder to save it (to be accessible by multiple machine if you ruin it in a cluster"
                                )
                                .addChoice("JSON", FileVariableFactory.FileVariableStorage.JSON.toString())
                                .addChoice("TEMPFOLDER", FileVariableFactory.FileVariableStorage.TEMPFOLDER.toString())
                                .addChoice("FOLDER", FileVariableFactory.FileVariableStorage.FOLDER.toString())
                        ,
                        AbstractWorker.WorkerParameter.getInstance(INPUT_STORAGEDEFINITION_COMPLEMENT, "Storage defintion Complement", String.class, AbstractWorker.Level.OPTIONAL, "Complement to the Storage definition, if needed. " + FileVariableFactory.FileVariableStorage.FOLDER + ": please provide the folder to save the file")
                                .addCondition(INPUT_STORAGEDEFINITION, Arrays.asList(FileVariableFactory.FileVariableStorage.FOLDER.toString()))
                ),


                Arrays.asList(
                        AbstractWorker.WorkerParameter.getInstance(OUTPUT_FILE_LOADED, "File loaded", Object.class, Level.REQUIRED, "Content of the file, according the storage definition"),
                        AbstractWorker.WorkerParameter.getInstance(OUTPUT_FILE_NAME, "File name", String.class, Level.REQUIRED, "Name of the file"),
                        AbstractWorker.WorkerParameter.getInstance(OUTPUT_FILE_MIMETYPE, "File Mime type", String.class, AbstractWorker.Level.REQUIRED, "MimeType of the loaded file")),
                Arrays.asList(
                        AbstractWorker.BpmnError.getInstance(BPMNERROR_FOLDER_NOT_EXIST_ERROR, "Folder does not exist, or not visible from the server"),
                        AbstractWorker.BpmnError.getInstance(BPMNERROR_LOAD_FILE_ERROR, "Error during the load"),
                        AbstractWorker.BpmnError.getInstance(BPMNERROR_MOVE_FILE_ERROR, "Error when the file is moved to the archive directory"),
                        AbstractWorker.BpmnError.getInstance(FileVariableFactory.BPMNERROR_INCORRECT_STORAGEDEFINITION, "Storage definition is incorrect"))
        );
    }

    @Override

    @ZeebeWorker(type = WORKERTYPE_FILES_LOAD_FROM_DISK, autoComplete = true)
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

        // with a template, the storage defitinion is just the droptdown value, so add the complement if present
        String storageDefinitionComplement = getInputStringValue(INPUT_STORAGEDEFINITION_COMPLEMENT, null, activatedJob);
        if (storageDefinitionComplement != null && !storageDefinitionComplement.trim().isEmpty())
            storageDefinition = storageDefinition + ":" + storageDefinitionComplement;

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
