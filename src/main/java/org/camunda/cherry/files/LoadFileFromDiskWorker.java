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
import io.camunda.zeebe.spring.client.exception.ZeebeBpmnError;
import org.camunda.cherry.definition.AbstractWorker;
import org.camunda.cherry.definition.BpmnError;
import org.camunda.cherry.definition.RunnerParameter;
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
	private static final String WORKERLOGO = "data:image/svg+xml,%3C?xml version='1.0' encoding='UTF-8' standalone='no'?%3E%3Csvg   xmlns:dc='http://purl.org/dc/elements/1.1/'   xmlns:cc='http://creativecommons.org/ns%23'   xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns%23'   xmlns:svg='http://www.w3.org/2000/svg'   xmlns='http://www.w3.org/2000/svg'   version='1.1'   id='Capa_1'   x='0px'   y='0px'   viewBox='0 0 18 18'   xml:space='preserve'   width='18'   height='18'%3E%3Cmetadata   id='metadata55'%3E%3Crdf:RDF%3E%3Ccc:Work       rdf:about=''%3E%3Cdc:format%3Eimage/svg+xml%3C/dc:format%3E%3Cdc:type         rdf:resource='http://purl.org/dc/dcmitype/StillImage' /%3E%3Cdc:title%3E%3C/dc:title%3E%3C/cc:Work%3E%3C/rdf:RDF%3E%3C/metadata%3E%3Cdefs   id='defs53' /%3E%3Cg   id='g18'   transform='scale(0.3)'%3E %3Cpath   d='M 30,0 C 13.458,0 0,13.458 0,30 0,46.542 13.458,60 30,60 46.542,60 60,46.542 60,30 60,13.458 46.542,0 30,0 Z m 0,58 C 14.561,58 2,45.439 2,30 2,14.561 14.561,2 30,2 45.439,2 58,14.561 58,30 58,45.439 45.439,58 30,58 Z'   id='path2' /%3E %3Cpath   d='M 23.165,8.459 C 23.702,8.329 24.033,7.789 23.904,7.253 23.775,6.716 23.234,6.387 22.698,6.514 18.763,7.46 15.176,9.469 12.322,12.323 9.468,15.177 7.46,18.764 6.514,22.698 c -0.129,0.536 0.202,1.076 0.739,1.206 0.078,0.019 0.157,0.027 0.234,0.027 0.451,0 0.861,-0.308 0.972,-0.767 0.859,-3.575 2.685,-6.836 5.277,-9.429 2.592,-2.593 5.854,-4.417 9.429,-5.276 z'   id='path4' /%3E %3Cpath   d='m 52.747,36.096 c -0.538,-0.129 -1.077,0.201 -1.206,0.739 -0.859,3.575 -2.685,6.836 -5.277,9.429 -2.592,2.593 -5.854,4.418 -9.43,5.277 -0.537,0.13 -0.868,0.67 -0.739,1.206 0.11,0.459 0.521,0.767 0.972,0.767 0.077,0 0.156,-0.009 0.234,-0.027 3.936,-0.946 7.523,-2.955 10.377,-5.809 2.854,-2.854 4.862,-6.441 5.809,-10.376 0.128,-0.536 -0.203,-1.076 -0.74,-1.206 z'   id='path6' /%3E %3Cpath   d='m 24.452,13.286 c 0.538,-0.125 0.873,-0.663 0.747,-1.2 -0.125,-0.538 -0.665,-0.878 -1.2,-0.747 -3.09,0.72 -5.904,2.282 -8.141,4.52 -2.237,2.236 -3.8,5.051 -4.52,8.141 -0.126,0.537 0.209,1.075 0.747,1.2 0.076,0.019 0.152,0.026 0.228,0.026 0.454,0 0.865,-0.312 0.973,-0.773 0.635,-2.725 2.014,-5.207 3.986,-7.18 1.972,-1.973 4.456,-3.352 7.18,-3.987 z'   id='path8' /%3E %3Cpath   d='m 48.661,36.001 c 0.126,-0.537 -0.209,-1.075 -0.747,-1.2 -0.538,-0.133 -1.075,0.209 -1.2,0.747 -0.635,2.725 -2.014,5.207 -3.986,7.18 -1.972,1.973 -4.455,3.352 -7.18,3.986 -0.538,0.125 -0.873,0.663 -0.747,1.2 0.107,0.462 0.519,0.773 0.973,0.773 0.075,0 0.151,-0.008 0.228,-0.026 3.09,-0.72 5.904,-2.282 8.141,-4.52 2.236,-2.236 3.798,-5.05 4.518,-8.14 z'   id='path10' /%3E %3Cpath   d='m 26.495,16.925 c -0.119,-0.541 -0.653,-0.879 -1.19,-0.763 -4.557,0.997 -8.146,4.586 -9.143,9.143 -0.118,0.539 0.224,1.072 0.763,1.19 0.072,0.016 0.144,0.023 0.215,0.023 0.46,0 0.873,-0.318 0.976,-0.786 0.831,-3.796 3.821,-6.786 7.617,-7.617 0.538,-0.118 0.88,-0.651 0.762,-1.19 z'   id='path12' /%3E %3Cpath   d='m 43.838,34.695 c 0.118,-0.539 -0.224,-1.072 -0.763,-1.19 -0.54,-0.118 -1.072,0.222 -1.19,0.763 -0.831,3.796 -3.821,6.786 -7.617,7.617 -0.539,0.118 -0.881,0.651 -0.763,1.19 0.103,0.468 0.516,0.786 0.976,0.786 0.071,0 0.143,-0.008 0.215,-0.023 4.556,-0.997 8.145,-4.586 9.142,-9.143 z'   id='path14' /%3E %3Cpath   d='m 38.08,30 c 0,-4.455 -3.625,-8.08 -8.08,-8.08 -4.455,0 -8.08,3.625 -8.08,8.08 0,4.455 3.625,8.08 8.08,8.08 4.455,0 8.08,-3.625 8.08,-8.08 z M 30,36.08 c -3.353,0 -6.08,-2.728 -6.08,-6.08 0,-3.352 2.728,-6.08 6.08,-6.08 3.352,0 6.08,2.728 6.08,6.08 0,3.352 -2.727,6.08 -6.08,6.08 z'   id='path16' /%3E%3C/g%3E%3Cg   id='g20'%3E%3C/g%3E%3Cg   id='g22'%3E%3C/g%3E%3Cg   id='g24'%3E%3C/g%3E%3Cg   id='g26'%3E%3C/g%3E%3Cg   id='g28'%3E%3C/g%3E%3Cg   id='g30'%3E%3C/g%3E%3Cg   id='g32'%3E%3C/g%3E%3Cg   id='g34'%3E%3C/g%3E%3Cg   id='g36'%3E%3C/g%3E%3Cg   id='g38'%3E%3C/g%3E%3Cg   id='g40'%3E%3C/g%3E%3Cg   id='g42'%3E%3C/g%3E%3Cg   id='g44'%3E%3C/g%3E%3Cg   id='g46'%3E%3C/g%3E%3Cg   id='g48'%3E%3C/g%3E%3Cg   id='g85'   transform='matrix(1.1259408,0,0,1.1259408,33.760593,8.6647721)'%3E%3Cpath     d='m -15.969199,-1.3973349 c -0.04428,0.56592004 -0.49572,0.83754004 -0.58752,0.88722004 -0.4887,0.26676 -1.31112,0.17388 -1.70856,-0.38556 -0.21924,-0.30780004 -0.3294,-0.79758004 -0.1161,-1.13670004 l 0.0027,-0.0054 c 0.07668,-0.12474 0.25434,-0.33534 0.58212,-0.34236 h 0.01188 c 0.11718,0 0.25218,0.03564 0.37152,0.06696 0.0837,0.02214 0.15606,0.04104 0.21006,0.04482 0.03402,0.00216 0.08262,-0.00648 0.14418,-0.01728 0.18414,-0.0324 0.4914,-0.0864 0.76032,0.10368 0.36612,0.2597403 0.33156,0.76356 0.3294,0.78462 z'     id='path57'     style='fill:%23aa0000;fill-opacity:1;stroke-width:0.3' /%3E%3Cpath     d='m -18.207499,-2.3007546 c -0.1053,0.07452 -0.17496,0.16686 -0.21708,0.2349 l -0.0027,0.00432 c -0.10908,0.17388 -0.13932,0.37584 -0.1188,0.57348 -0.0022,0.00108 -0.0043,0.00216 -0.0059,0.00378 -0.40014,0.3267 -0.85212,0.24246 -1.02762,0.19116 l -0.01026,-0.0027 c -0.5589,-0.13932 -1.11078,-0.74304 -1.00926,-1.38618 0.05616,-0.35424 0.324,-0.7614 0.72792,-0.85428 0.06966,-0.0162 0.43362,-0.0837 0.7263,0.16146 0.08964,0.0756 0.15606,0.1857601 0.21438,0.28296 0.04158,0.0702 0.07776,0.13014 0.11718,0.16956 0.02538,0.02538 0.06966,0.05022 0.12582,0.08208 0.1647,0.09234 0.4131,0.2322001 0.4779,0.53352 5.4e-4,0.00216 0.0011,0.00432 0.0022,0.00594 z'     id='path59'     style='fill:%23aa0000;fill-opacity:1;stroke-width:0.3' /%3E%3Cpath     d='m -17.110219,-2.3309949 c -0.03186,0.0054 -0.05994,0.00918 -0.07992,0.00918 -0.0032,0 -0.0065,-5.4e-4 -0.0092,-5.4e-4 -0.01512,-0.00108 -0.03294,-0.00324 -0.05184,-0.00702 0.16578,-0.42336 0.23436,-1.10052 -0.34182,-1.95804 -0.0011,-0.00216 -0.0027,-0.00378 -0.0049,-0.0054 0.03348,-0.13284 0.04482,-0.22572 0.04752,-0.2484 0.0016,-0.01512 -0.0092,-0.02808 -0.02376,-0.0297 -0.01512,-0.00216 -0.02808,0.00918 -0.0297,0.02376 -0.0065,0.05778 -0.07128,0.5788801 -0.4347,1.0416602 -0.22734,0.28944 -0.58536,0.5049 -0.70902,0.56646 -0.01134,-0.00756 -0.02106,-0.01512 -0.02754,-0.0216 -0.01782,-0.01782 -0.0351,-0.04104 -0.05292,-0.06858 0.01026,-0.0054 0.02106,-0.01134 0.0324,-0.01728 0.12906,-0.06912 0.34506,-0.1841401 0.60858,-0.4600801 0.31158,-0.32616 0.37098,-0.82566 0.2835,-0.96174 -0.04212,-0.06588 -0.10962,-0.07236 -0.16362,-0.07722 -0.07074,-0.00594 -0.1134,-0.01026 -0.12096,-0.10638 -0.007,-0.0837 0.0324,-0.15768 0.1107,-0.20736 0.1053,-0.0675 0.29376,-0.0891 0.47628,0.01458 0.11394,0.06426 0.11286,0.17388 0.11232,0.27972 -5.4e-4,0.07074 -0.0011,0.1377 0.03402,0.18576 l 0.01512,0.02052 c 0.0864,0.1171801 0.28836,0.3904201 0.40932,0.86724 0.12906,0.5102999 0.007,0.9752399 -0.07992,1.1604599 z'     id='path61'     style='fill:%23502d16;fill-opacity:1;stroke-width:0.3' /%3E%3Cpath     d='m -18.698899,-4.5455348 c -0.0043,0.00216 -0.0086,0.00324 -0.01296,0.00324 -0.0097,0 -0.0189,-0.0054 -0.02376,-0.01404 -5.4e-4,-5.4e-4 -0.03834,-0.06804 -0.10638,-0.13014 -0.01134,-0.00972 -0.01188,-0.027 -0.0022,-0.0378 0.01026,-0.01134 0.027,-0.01188 0.03834,-0.00216 0.07506,0.06804 0.11556,0.14094 0.11718,0.14418 0.0076,0.01296 0.0027,0.02916 -0.01026,0.03672 z'     id='path63'     style='stroke-width:0.3' /%3E%3Cpath     d='m -18.644359,-4.0309149 c -0.05616,0.06642 -0.08532,0.0783 -0.1296,0.09612 l -0.01404,0.00594 c -0.0032,0.00162 -0.007,0.00216 -0.01026,0.00216 -0.0108,0 -0.02052,-0.00648 -0.02484,-0.01674 -0.0059,-0.0135 5.4e-4,-0.0297 0.01458,-0.0351 l 0.01404,-0.00594 c 0.0405,-0.01674 0.06102,-0.02538 0.10908,-0.081 0.0097,-0.01188 0.027,-0.01296 0.03834,-0.00324 0.01134,0.00972 0.01242,0.02646 0.0027,0.0378 z'     id='path65'     style='stroke-width:0.3' /%3E%3Cpath     d='m -17.948839,-4.4391548 c -0.01404,-0.02214 -0.0324,-0.03402 -0.05292,-0.0405 -0.07182,0.09666 -0.32454,0.243 -0.57834,0.243 -0.0675,0 -0.13554,-0.01026 -0.1998,-0.03456 -0.1728,-0.06588 -0.29106,-0.15768 -0.40554,-0.24624 -0.09072,-0.07074 -0.17658,-0.13716 -0.28134,-0.18252 -0.01404,-0.00648 -0.01998,-0.02214 -0.01404,-0.03564 0.0059,-0.01404 0.0216,-0.01998 0.03564,-0.01404 0.1107,0.0486 0.19926,0.11664 0.29268,0.18954 0.11124,0.0864 0.2268,0.1755 0.3915,0.23814 0.16794,0.06372 0.3564,0.02214 0.4995,-0.04212 -0.06156,-0.19008 -0.45954,-0.76842 -1.11564,-0.6210001 -0.10152,0.02268 -0.19008,0.04374 -0.26784,0.0621 -0.3294,0.07722 -0.47034,0.11016 -0.61668,0.07128 0.0135,0.06804 0.04914,0.12582 0.10584,0.17226 0.1134,0.09126 0.2781,0.11394 0.3564,0.11502 -0.03996,-0.04536 -0.08262,-0.07452 -0.135,-0.09288 -0.01404,-0.00486 -0.0216,-0.01998 -0.01674,-0.03402 0.0049,-0.01458 0.02052,-0.0216 0.03456,-0.01674 0.1701,0.05994 0.2484,0.21222 0.37476,0.5227201 0.1323,0.32508 0.59508,0.62802 0.97038,0.5157 0.38232,-0.11448 0.37584,-0.51732 0.3753,-0.52164 0,-0.0108 0.0065,-0.02106 0.0162,-0.02538 0.02538,-0.01134 0.15552,-0.07074 0.24732,-0.15714 0.0016,-0.00162 0.0032,-0.0027 0.0049,-0.0027 v -5.4e-4 c -0.0043,-0.02646 -0.01188,-0.04752 -0.02106,-0.0621 m -0.89532,-0.2851201 c 0.01026,-0.01134 0.027,-0.01188 0.03834,-0.00216 0.07506,0.06804 0.11556,0.14094 0.11718,0.14418 0.0076,0.01296 0.0027,0.02916 -0.01026,0.03672 -0.0043,0.00216 -0.0086,0.00324 -0.01296,0.00324 -0.0097,0 -0.0189,-0.0054 -0.02376,-0.01404 -5.4e-4,-5.4e-4 -0.03834,-0.06804 -0.10638,-0.13014 -0.01134,-0.00972 -0.01188,-0.027 -0.0022,-0.0378 m -0.45468,0.36774 c -0.0011,0 -0.01674,0.0027 -0.04752,0.0027 -0.0243,0 -0.05724,-0.00162 -0.09936,-0.00702 -0.01512,-0.00162 -0.02538,-0.01512 -0.02322,-0.03024 0.0016,-0.01458 0.01512,-0.02484 0.03024,-0.02322 0.08748,0.01134 0.13014,0.00486 0.13068,0.00486 0.01458,-0.0027 0.02862,0.00702 0.03078,0.02214 0.0027,0.01458 -0.007,0.02808 -0.0216,0.03078 m 0.65448,0.32562 c -0.05616,0.06642 -0.08532,0.0783 -0.1296,0.09612 l -0.01404,0.00594 c -0.0032,0.00162 -0.007,0.00216 -0.01026,0.00216 -0.0108,0 -0.02052,-0.00648 -0.02484,-0.01674 -0.0059,-0.0135 5.4e-4,-0.0297 0.01458,-0.0351 l 0.01404,-0.00594 c 0.0405,-0.01674 0.06102,-0.02538 0.10908,-0.081 0.0097,-0.01188 0.027,-0.01296 0.03834,-0.00324 0.01134,0.00972 0.01242,0.02646 0.0027,0.0378 z'     id='path67'     style='fill:%23008000;fill-opacity:1;stroke-width:0.3' /%3E%3Cpath     d='m -19.277239,-4.3873149 c 0.0027,0.01458 -0.007,0.02808 -0.0216,0.03078 -0.0011,0 -0.01674,0.0027 -0.04752,0.0027 -0.0243,0 -0.05724,-0.00162 -0.09936,-0.00702 -0.01512,-0.00162 -0.02538,-0.01512 -0.02322,-0.03024 0.0016,-0.01458 0.01512,-0.02484 0.03024,-0.02322 0.08748,0.01134 0.13014,0.00486 0.13068,0.00486 0.01458,-0.0027 0.02862,0.00702 0.03078,0.02214 z'     id='path69'     style='stroke-width:0.3' /%3E%3C/g%3E%3C/svg%3E";

	
    private final Logger logger = LoggerFactory.getLogger(LoadFileFromDiskWorker.class.getName());

    public LoadFileFromDiskWorker() {
        super(WORKERTYPE_FILES_LOAD_FROM_DISK,
                Arrays.asList(
                        RunnerParameter.getInstance(INPUT_FOLDER, "Folder", String.class, RunnerParameter.Level.REQUIRED, "Specify the folder where the file will be loaded. Must be visible from the server."),
                        RunnerParameter.getInstance(INPUT_FILE_NAME, "File name", String.class, RunnerParameter.Level.OPTIONAL, "Specify a file name, else the first file in the folder will be loaded"),
                        RunnerParameter.getInstance(INPUT_FILTER_FILE, "Filter file", String.class, RunnerParameter.Level.OPTIONAL, "If you didn't specify a fileName, a filter to select only part of files present in the folder"),
                        RunnerParameter.getInstance(INPUT_POLICY, "Policy", String.class, RunnerParameter.Level.OPTIONAL,
                                        "Policy to manipulate the file after loading. With " + POLICY_V_ARCHIVE + ", the folder archive must be specify")
                                .addChoice("DELETE", POLICY_V_DELETE)
                                .addChoice("ARCHIVE", POLICY_V_ARCHIVE)
                                .addChoice("UNCHANGE", POLICY_V_UNCHANGE)
                        ,
                        RunnerParameter.getInstance(INPUT_ARCHIVE_FOLDER, "Archive folder", String.class, RunnerParameter.Level.OPTIONAL, "With the policy " + POLICY_V_ARCHIVE + ". File is moved in this folder.")
                                .addCondition(INPUT_POLICY, Arrays.asList(POLICY_V_ARCHIVE)),
                        RunnerParameter.getInstance(INPUT_STORAGEDEFINITION, "Storage definition", String.class, FileVariableFactory.FileVariableStorage.JSON.toString(),
                                        RunnerParameter.Level.OPTIONAL,
                                        "How to saved the FileVariable. "
                                                + FileVariableFactory.FileVariableStorage.JSON + " to save in the engine (size is linited), "
                                                + FileVariableFactory.FileVariableStorage.TEMPFOLDER + " to use the temporary folder of THIS machine"
                                                + FileVariableFactory.FileVariableStorage.FOLDER + " to specify a folder to save it (to be accessible by multiple machine if you ruin it in a cluster"
                                )
                                .addChoice("JSON", FileVariableFactory.FileVariableStorage.JSON.toString())
                                .addChoice("TEMPFOLDER", FileVariableFactory.FileVariableStorage.TEMPFOLDER.toString())
                                .addChoice("FOLDER", FileVariableFactory.FileVariableStorage.FOLDER.toString())
                        ,
                        RunnerParameter.getInstance(INPUT_STORAGEDEFINITION_COMPLEMENT, "Storage defintion Complement", String.class, RunnerParameter.Level.OPTIONAL, "Complement to the Storage definition, if needed. " + FileVariableFactory.FileVariableStorage.FOLDER + ": please provide the folder to save the file")
                                .addCondition(INPUT_STORAGEDEFINITION, Arrays.asList(FileVariableFactory.FileVariableStorage.FOLDER.toString()))
                ),


                Arrays.asList(
                        RunnerParameter.getInstance(OUTPUT_FILE_LOADED, "File loaded", Object.class, RunnerParameter.Level.REQUIRED, "Content of the file, according the storage definition"),
                        RunnerParameter.getInstance(OUTPUT_FILE_NAME, "File name", String.class, RunnerParameter.Level.REQUIRED, "Name of the file"),
                        RunnerParameter.getInstance(OUTPUT_FILE_MIMETYPE, "File Mime type", String.class, RunnerParameter.Level.REQUIRED, "MimeType of the loaded file")),
                Arrays.asList(
                        BpmnError.getInstance(BPMNERROR_FOLDER_NOT_EXIST_ERROR, "Folder does not exist, or not visible from the server"),
                        BpmnError.getInstance(BPMNERROR_LOAD_FILE_ERROR, "Error during the load"),
                        BpmnError.getInstance(BPMNERROR_MOVE_FILE_ERROR, "Error when the file is moved to the archive directory"),
                        BpmnError.getInstance(FileVariableFactory.BPMNERROR_INCORRECT_STORAGEDEFINITION, "Storage definition is incorrect"))
        );
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
                    .toList();
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
	
	public String getLogo() {
		return WORKERLOGO;
	}
}
