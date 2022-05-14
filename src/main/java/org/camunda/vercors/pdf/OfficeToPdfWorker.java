/* ******************************************************************** */
/*                                                                      */
/*  OfficeToPdfWorker                                                   */
/*                                                                      */
/*  Get a office (MSOffice, ODT) and transform it to PDF                */
/* ******************************************************************** */
package org.camunda.vercors.pdf;

import fr.opensagres.xdocreport.converter.ConverterTypeTo;
import fr.opensagres.xdocreport.converter.Options;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import io.camunda.zeebe.spring.client.exception.ZeebeBpmnError;
import org.camunda.vercors.definition.AbstractWorker;
import org.camunda.vercors.definition.filevariable.FileVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

@Component
public class OfficeToPdfWorker extends AbstractWorker {

    public static final String BPMERROR_CONVERSION_ERROR = "CONVERSION_ERROR";
    public static final String BPMERROR_LOAD_FILE_ERROR = "LOAD_FILE_ERROR";
    private final static String INPUT_SOURCE_FILE = "sourceFile";
    private final static String INPUT_SOURCE_STORAGEDEFINITION = "sourceStorageDefinition";
    private final static String INPUT_DESTINATION_FILE_NAME = "destinationFileName";
    private final static String INPUT_DESTINATION_STORAGEDEFINITION = "destinationStorageDefinition";
    private final static String OUTPUT_DESTINATION_FILE = "destinationFile";
    Logger logger = LoggerFactory.getLogger(OfficeToPdfWorker.class.getName());

    public OfficeToPdfWorker() {
        super("v-pdf-convert-to",
                Arrays.asList(
                        AbstractWorker.WorkerParameter.getInstance(INPUT_SOURCE_FILE, Object.class, Level.REQUIRED, "FileVariable for the file to convert"),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_SOURCE_STORAGEDEFINITION, String.class, Level.REQUIRED, "Storage Definition use to access the file"),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_DESTINATION_FILE_NAME, String.class, Level.REQUIRED, "Destination file name"),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_DESTINATION_STORAGEDEFINITION, String.class, Level.REQUIRED, "Storage Definition use to describe how to save the file")
                ),
                Arrays.asList(
                        AbstractWorker.WorkerParameter.getInstance(OUTPUT_DESTINATION_FILE, Object.class, Level.REQUIRED, "FileVariable converted")
                ),
                Arrays.asList(BPMERROR_CONVERSION_ERROR, BPMERROR_LOAD_FILE_ERROR));
    }

    @ZeebeWorker(type = "v-pdf-convert-to", autoComplete = true)
    public void handleWorkerExecution(final JobClient jobClient, final ActivatedJob activatedJob) {
        super.handleWorkerExecution(jobClient, activatedJob);
    }


    public void execute(final JobClient jobClient, final ActivatedJob activatedJob) {
        String sourceStorageDefinition = getInputStringValue(INPUT_SOURCE_STORAGEDEFINITION, null, activatedJob);
        FileVariable sourceFileVariable = getFileVariableValue(INPUT_SOURCE_FILE, sourceStorageDefinition, activatedJob);

        String destinationFileName = getInputStringValue(INPUT_DESTINATION_FILE_NAME, null, activatedJob);
        String destinationStorageDefinition = getInputStringValue(INPUT_DESTINATION_STORAGEDEFINITION, null, activatedJob);

        if (sourceFileVariable == null || sourceFileVariable.value == null) {
            throw new ZeebeBpmnError(BPMERROR_LOAD_FILE_ERROR, "Worker [" + getName() + "] cannot read file[" + sourceStorageDefinition + "]");
        }

        // get the file
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(sourceFileVariable.value);

        final IXDocReport report;
        try {
            report = XDocReportRegistry.getRegistry()
                    .loadReport(byteArrayInputStream, TemplateEngineKind.Velocity);

            final IContext context = report.createContext();

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            //  protected PdfOptions getPDFOptions() {
            //        PdfOptions options = PdfOptions.create();
            //        if (encoding != null && !encoding.isEmpty()) {
            //            options = options.fontEncoding(encoding);
            //        }
            //        return options;
            // Options.getTo(ConverterTypeTo.PDF).via(ConverterTypeVia.XWPF).subOptions(getPDFOptions()),
            report.convert(context,
                    Options.getTo(ConverterTypeTo.PDF),
                    out);
            FileVariable fileVariableOut = new FileVariable();
            fileVariableOut.value = out.toByteArray();
            fileVariableOut.name = destinationFileName;
            setFileVariableValue(OUTPUT_DESTINATION_FILE, destinationStorageDefinition, fileVariableOut);
        } catch (Exception e) {
            throw new ZeebeBpmnError(BPMERROR_CONVERSION_ERROR, "Worker [" + getName() + "] cannot convert file[" + sourceFileVariable.name + "] : " + e);
        }

    }
}