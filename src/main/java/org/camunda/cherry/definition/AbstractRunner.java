/* ******************************************************************** */
/*                                                                      */
/*  AbstractRunner                                                    */
/*                                                                      */
/*  The Runner is the basis for Workers and Connector to operate in   */
/* the Cherry Framework. A Runner defined                            */
/*   - a type                                                           */
/*   - list of Input/Output/Error                                       */
/*   - optionally, description, logo                              */
/* The execution depends on the class: Worker or Connector              */
/*                                                                      */
/* ******************************************************************** */
package org.camunda.cherry.definition;

import com.google.gson.Gson;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.exception.ZeebeBpmnError;
import org.camunda.cherry.definition.filevariable.FileVariable;
import org.camunda.cherry.definition.filevariable.FileVariableFactory;
import org.camunda.cherry.definition.filevariable.FileVariableReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class AbstractRunner {


    public final static String BPMNERROR_ACCESS_FILEVARIABLE = "ACCESS_FILEVARIABLE";
    public final static String BPMNERROR_SAVE_FILEVARIABLE = "SAVE_FILEVARIABLE";
	private static String logo = "data:image/svg+xml,%3C?xml version='1.0' encoding='UTF-8' standalone='no'?%3E%3Csvg   xmlns:dc='http://purl.org/dc/elements/1.1/'   xmlns:cc='http://creativecommons.org/ns%23'   xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns%23'   xmlns:svg='http://www.w3.org/2000/svg'   xmlns='http://www.w3.org/2000/svg'   width='18'   height='18'   viewBox='0 0 16.875 16.875'   id='svg15795'   version='1.1'%3E  %3Cdefs     id='defs15797'%3E    %3CradialGradient       r='255.59'       gradientTransform='matrix(-0.12272793,0,0,0.12272793,-1807.3634,-1063.9497)'       cx='-464.92001'       cy='873.19'       gradientUnits='userSpaceOnUse'       id='radialGradient3026-9-1'%3E      %3Cstop         offset='.5'         style='stop-color:%23ffd046;stop-opacity:1'         id='stop48-0-2' /%3E      %3Cstop         offset='1'         style='stop-color:%23f0a957;stop-opacity:1'         id='stop50-8-9' /%3E    %3C/radialGradient%3E  %3C/defs%3E  %3Cmetadata     id='metadata15800'%3E    %3Crdf:RDF%3E      %3Ccc:Work         rdf:about=''%3E        %3Cdc:format%3Eimage/svg+xml%3C/dc:format%3E        %3Cdc:type           rdf:resource='http://purl.org/dc/dcmitype/StillImage' /%3E        %3Cdc:title%3E%3C/dc:title%3E      %3C/cc:Work%3E    %3C/rdf:RDF%3E  %3C/metadata%3E  %3Cg     id='layer1'     transform='translate(-333.12983,-288.31523)'%3E    %3Cg       transform='matrix(0.36709704,0,0,0.36709704,986.33222,642.92532)'       style='display:inline'       id='g15745'%3E      %3Cpath         d='m -1764.6075,-935.9985 c 0,0 3.8003,2.04759 9.6285,4.07616 3.5202,1.22728 9.7726,-0.84277 13.0276,-1.70481 3.26,-0.86511 8.8927,-2.65449 7.3298,-4.21743 -1.5631,-1.56257 -6.0571,-4.89537 -6.0571,-4.89537 0,0 4.2789,7.06582 -23.9295,6.74145 z'         style='fill:%23ffffff;stroke:%23000000;stroke-width:1.89946;stroke-linejoin:round;stroke-opacity:1'         id='path9-1-4' /%3E      %3Cpath         id='path13-3-8'         style='fill:%23ffffff;stroke:%23000000;stroke-width:1.89946;stroke-linejoin:round;stroke-opacity:1'         d='m -1738.5952,-941.13712 c 0,2.98008 -8.9157,5.39684 -19.915,5.39684 -10.9985,0 -19.9126,-2.41639 -19.9126,-5.39684 2.2937,-1.66297 1.9624,-3.06329 1.9624,-3.06329 h 35.9961 c 0,0 -0.3197,1.39983 1.8691,3.06255 z' /%3E      %3Cpath         d='m -1774.2711,-954.86792 c 0.4453,-1.01594 2.1705,-4.52486 5.6775,-6.995 6.3577,-4.4835 14.2414,-3.62821 18.8768,-0.93691 3.1324,1.82227 5.2903,4.17054 7.0001,7.61821 0.4172,0.84216 1.1951,2.63915 1.7282,5.20993 0.4613,2.22972 0.5058,3.63581 0.58,5.97108 0,1.95125 -3.7069,3.71117 -10.0877,4.56818 -8.93,1.19586 -19.7224,0.15709 -24.1026,-2.3198 -1.2534,-0.70839 -1.8451,-1.46218 -1.8426,-2.19634 0.092,-3.01727 0.204,-6.43549 2.1707,-10.91935 z'         style='fill:%23ffffff;stroke-linejoin:round'         id='path19-3-0' /%3E      %3Cpath         d='m -1774.2711,-954.86792 c 0.4453,-1.01594 2.1705,-4.52486 5.6775,-6.995 6.3577,-4.4835 14.2414,-3.62821 18.8768,-0.93691 3.1324,1.82227 5.2903,4.17054 7.0001,7.61821 0.4172,0.84216 1.1951,2.63915 1.7282,5.20993 0.4613,2.22972 0.5058,3.63581 0.58,5.97108 0,1.95125 -3.7069,3.71117 -10.0877,4.56818 -8.93,1.19586 -19.7224,0.15709 -24.1026,-2.3198 -1.2534,-0.70839 -1.8451,-1.46218 -1.8426,-2.19634 0.092,-3.01727 0.204,-6.43549 2.1707,-10.91935 z'         style='fill:%2374fc00;fill-opacity:1;stroke:%23000000;stroke-width:1.89946;stroke-linejoin:round;stroke-opacity:1'         id='path23-1-6' /%3E      %3Cpath         d='m -1764.7572,-936.08809 c 0,0 3.8014,2.04415 9.6284,4.07591 3.5189,1.22483 9.7706,-0.84277 13.0263,-1.70788 3.2599,-0.86192 8.8934,-2.65141 7.3303,-4.21754 -1.5629,-1.56233 -6.0547,-4.89329 -6.0547,-4.89329 0,0 4.277,7.06618 -23.9307,6.7428 z'         style='fill:%23ffbd34;fill-opacity:1;stroke:%23ffbd34;stroke-width:0.633166;stroke-linejoin:round;stroke-opacity:1'         id='path27-6-0' /%3E      %3Cpath         id='path31-3-3'         style='fill:%23ffbd34;fill-opacity:1;stroke:%23ffbd34;stroke-width:0.633166;stroke-linejoin:round;stroke-opacity:1'         d='m -1738.7453,-941.22671 c 0,2.98032 -8.913,5.39499 -19.9138,5.39499 -10.998,0 -19.9138,-2.41405 -19.9138,-5.39499 2.2952,-1.66272 1.961,-3.06243 1.961,-3.06243 h 35.9986 c 0,0 -0.214,1.63369 1.9731,3.29629 z' /%3E      %3Cpath         d='m -1774.422,-954.95874 c 0.4452,-1.01521 2.1713,-4.52265 5.6774,-6.99513 6.357,-4.48337 14.2401,-3.6271 18.8767,-0.93616 3.1344,1.82238 5.2911,4.17041 6.9995,7.61526 0.418,0.84216 1.1977,2.63915 1.7282,5.21128 0.46,2.22825 0.505,3.63557 0.5804,5.96789 0,1.95137 -3.7087,3.71424 -10.0905,4.5683 -8.9296,1.19893 -19.7211,0.15697 -24.1,-2.31993 -1.2535,-0.70838 -1.8464,-1.45948 -1.8437,-2.19633 0.096,-3.01248 0.2077,-6.43119 2.1721,-10.91518 z'         style='fill:%23ffd520;stroke-linejoin:round'         id='path37-0-6' /%3E      %3Cpath         d='m -1774.422,-954.95874 c 0.4452,-1.01521 2.1713,-4.52265 5.6774,-6.99513 6.357,-4.48337 14.2401,-3.6271 18.8767,-0.93616 3.1344,1.82238 5.2911,4.17041 6.9995,7.61526 0.418,0.84216 1.1977,2.63915 1.7282,5.21128 0.46,2.22825 0.505,3.63557 0.5804,5.96789 0,1.95137 -3.7087,3.71424 -10.0905,4.5683 -8.9296,1.19893 -19.7211,0.15697 -24.1,-2.31993 -1.2535,-0.70838 -1.8464,-1.45948 -1.8437,-2.19633 0.096,-3.01248 0.2077,-6.43119 2.1721,-10.91518 z'         style='fill:none;stroke:%23000000;stroke-width:0.633166;stroke-linejoin:round'         id='path41-6-5' /%3E      %3Cpath         d='m -1774.422,-954.95874 c 0.4452,-1.01521 2.1713,-4.52265 5.6774,-6.99513 6.357,-4.48337 14.2401,-3.6271 18.8767,-0.93616 3.1344,1.82238 5.2911,4.17041 6.9995,7.61526 0.418,0.84216 1.1977,2.63915 1.7282,5.21128 0.46,2.22825 0.505,3.63557 0.5804,5.96789 0,1.95137 -3.7087,3.71424 -10.0905,4.5683 -8.9296,1.19893 -19.7211,0.15697 -24.1,-2.31993 -1.2535,-0.70838 -1.8464,-1.45948 -1.8437,-2.19633 0.096,-3.01248 0.2077,-6.43119 2.1721,-10.91518 z'         style='fill:url(%23radialGradient3026-9-1);stroke:%23fac44b;stroke-linejoin:round;stroke-opacity:1'         id='path52-0-7' /%3E      %3Cpath         d='m -1767.915,-962.62629 c 13.3737,-8.528 26.9474,8.98687 26.5227,20.23415 l 1.2441,0.50196 c -7e-4,-12.25929 -13.2853,-30.32975 -27.7672,-20.73611'         style='fill:%23cccccc;stroke-linejoin:round'         id='path60-2-9' /%3E      %3Cpolygon         transform='matrix(-0.12272793,0,0,0.12272793,-1731.9594,-974.55471)'         id='polygon62-6-3'         style='fill:%23cccccc;stroke-linejoin:round'         points='76.865,262.06 61.197,281.15 66.729,266.15 ' /%3E      %3Cpath         d='m -1745.3502,-940.13566 c 0,-11.09583 -6.6212,-28.38329 -18.6117,-24.45845 12.8582,-4.35218 23.8104,11.56833 23.8104,22.70098 l 0.6789,1.84129 -5.2013,1.75427 -0.6765,-1.83772 z'         style='fill:%23ffd467;fill-opacity:1;stroke:none;stroke-width:0.633166;stroke-linecap:round;stroke-linejoin:round'         id='path64-7-7' /%3E      %3Cpath         d='m -1745.349,-940.13566 0.6782,1.84141 -3.0497,-2.13755 c 0.4259,-11.24802 -6.82,-30.72126 -20.1949,-22.19658 14.4819,-9.59106 22.566,10.23183 22.566,22.49358 z'         style='fill:%23cccccc;stroke-linejoin:round'         id='path70-9-2' /%3E      %3Cpath         d='m -1745.349,-940.13566 0.6782,1.84141 -3.0497,-2.13755 c 0.4259,-11.24802 -6.82,-30.72126 -20.1949,-22.19658 14.4819,-9.59106 22.566,10.23183 22.566,22.49358 z'         style='fill:%23ffbd34;fill-opacity:1;stroke:none;stroke-width:0.633166;stroke-linecap:round;stroke-linejoin:round'         id='path74-7-4' /%3E      %3Cpolygon         transform='matrix(-0.12272793,0,0,0.12272793,-1731.9594,-974.55471)'         id='polygon78-8-4'         style='fill:%23ffbd34;fill-opacity:1;stroke:%23ffbd34;stroke-width:5.1591;stroke-linecap:round;stroke-linejoin:round;stroke-opacity:1'         points='267.24,277.31 253.03,251.64 254.68,222.57 265.49,233.41 ' /%3E      %3Cpolygon         style='stroke-linejoin:round'         transform='matrix(-0.12272793,0,0,0.12272793,-1731.9594,-974.55471)'         id='polygon80-2-4'         points='329.73,266.98 320.7,241.3 322.32,217.4 327.99,223.07 ' /%3E      %3Cpath         id='path82-9-3'         style='fill:%23ffd467;fill-opacity:1;stroke:%23ffd467;stroke-width:0.633166;stroke-linecap:round;stroke-linejoin:round;stroke-opacity:1'         d='m -1764.7572,-940.52103 c 0,0 -7.3005,-0.90536 -7.6667,-1.27121 -0.3672,-0.36291 0.2126,-5.38788 0.2126,-5.38788 l 7.6696,1.26802 z' /%3E      %3Cpolygon         transform='matrix(-0.12272793,0,0,0.12272793,-1731.9594,-974.55471)'         id='polygon84-9-0'         style='fill:%23f4b253;fill-opacity:1;stroke:%23f4b253;stroke-width:5.1591;stroke-linecap:round;stroke-linejoin:round;stroke-opacity:1'         points='254.68,222.57 265.49,233.41 327.99,223.07 317.16,212.24 ' /%3E    %3C/g%3E    %3Cg       id='g48'       transform='matrix(0.29730774,0,0,0.29730774,336.52636,289.85483)'%3E      %3Cpath         d='m 16.7328,13.29858 c -0.1476,1.8864 -1.6524,2.7918 -1.9584,2.9574 -1.629,0.8892 -4.3704,0.5796 -5.6952001,-1.2852 -0.7308,-1.026 -1.098,-2.6586 -0.387,-3.789 l 0.009,-0.018 c 0.2556,-0.4158 0.8478,-1.1178 1.9404001,-1.1412 h 0.0396 c 0.3906,0 0.8406,0.1188 1.2384,0.2232 0.279,0.0738 0.5202,0.1368 0.7002,0.1494 0.1134,0.0072 0.2754,-0.0216 0.4806,-0.0576 0.6138,-0.108 1.638,-0.288 2.5344,0.3456 1.2204,0.865801 1.1052,2.5452 1.098,2.6154 z'         id='path2'         style='fill:%23aa0000;fill-opacity:1' /%3E      %3Cpath         d='m 9.2718,10.287181 c -0.351,0.2484 -0.5832,0.5562 -0.7236,0.783 l -0.009,0.0144 c -0.3636001,0.5796 -0.4644,1.2528 -0.3960001,1.9116 -0.0072,0.0036 -0.0144,0.0072 -0.0198,0.0126 -1.3338001,1.089 -2.8404,0.8082 -3.4254003,0.6372 l -0.0342,-0.009 c -1.8630002,-0.4644 -3.70259996,-2.4768 -3.3642,-4.6206 0.1872,-1.1808001 1.08,-2.538 2.4264,-2.8476 0.2322,-0.054 1.4454,-0.279 2.421,0.5382 0.2988,0.252 0.5202,0.6192001 0.7146,0.9432 0.1386,0.234 0.2592,0.4338 0.3906001,0.5652 0.0846,0.0846 0.2322,0.1674 0.4194,0.2736 0.549,0.3078001 1.3770001,0.7740001 1.5930002,1.7784 0.0018,0.0072 0.0036,0.0144 0.0072,0.0198 z'         id='path4'         style='fill:%23aa0000;fill-opacity:1' /%3E      %3Cpath         d='m 12.9294,10.18638 c -0.1062,0.018 -0.1998,0.0306 -0.2664,0.0306 -0.0108,0 -0.0216,-0.0018 -0.0306,-0.0018 -0.0504,-0.0036 -0.1098,-0.0108 -0.1728,-0.0234 0.5526,-1.4112 0.7812,-3.6684 -1.1394,-6.5268 -0.0036,-0.0072 -0.009,-0.0126 -0.0162,-0.018 0.1116,-0.4428 0.1494,-0.7524 0.1584,-0.828 0.0054,-0.0504 -0.0306,-0.0936 -0.0792,-0.099 -0.0504,-0.0072 -0.0936,0.0306 -0.099,0.0792 -0.0216,0.1926 -0.2376,1.9296001 -1.4490001,3.4722004 -0.7578001,0.9648001 -1.9512001,1.6830001 -2.3634002,1.8882 -0.0378,-0.0252 -0.0702,-0.0504 -0.0918,-0.072 -0.0594,-0.0594 -0.117,-0.1368 -0.1764,-0.2286 0.0342,-0.018 0.0702,-0.0378 0.108,-0.0576 0.4302,-0.2304 1.1502,-0.6138001 2.0286002,-1.5336001 1.0386001,-1.0872 1.2366001,-2.7522001 0.9450001,-3.2058 -0.1404,-0.2196 -0.3654001,-0.2412 -0.5454001,-0.2574 -0.2358,-0.0198 -0.378,-0.0342 -0.4032,-0.3546 -0.0234,-0.279 0.108,-0.5256 0.369,-0.6912 0.3510001,-0.225 0.9792001,-0.297 1.5876001,0.0486 0.3798,0.2142 0.3762,0.5796 0.3744,0.9324 -0.0018,0.2358 -0.0036,0.459 0.1134,0.6192 l 0.0504,0.0684 c 0.288,0.3906001 0.9612,1.3014001 1.3644,2.8908 C 13.626,8.01918 13.2192,9.56898 12.9294,10.18638 Z'         id='path6'         style='fill:%23502d16;fill-opacity:1' /%3E      %3Cpath         d='m 7.6338,2.8045802 c -0.0144,0.0072 -0.0288,0.0108 -0.0432,0.0108 -0.0324,0 -0.063,-0.018 -0.0792,-0.0468 -0.0018,-0.0018 -0.1278,-0.2268 -0.3546,-0.4338 -0.0378,-0.0324 -0.0396,-0.09 -0.0072,-0.126 0.0342,-0.0378 0.09,-0.0396 0.1278,-0.0072 0.2502,0.2268 0.3852,0.4698 0.3906,0.4806 0.025201,0.0432 0.009,0.0972 -0.0342,0.1224 z'         id='path8' /%3E      %3Cpath         d='m 7.8156,4.51998 c -0.1872,0.2214 -0.2844,0.261 -0.432,0.3204 l -0.0468,0.0198 c -0.0108,0.0054 -0.0234,0.0072 -0.0342,0.0072 -0.036,0 -0.0684,-0.0216 -0.0828,-0.0558 -0.0198,-0.045 0.0018,-0.099 0.0486,-0.117 l 0.0468,-0.0198 c 0.135,-0.0558 0.2034,-0.0846 0.3636,-0.27 0.0324,-0.0396 0.09,-0.0432 0.1278,-0.0108 0.0378,0.0324 0.0414,0.0882 0.009,0.126 z'         id='path10' /%3E      %3Cpath         d='m 10.134,3.1591802 c -0.0468,-0.0738 -0.108,-0.1134 -0.1764,-0.135 -0.2394,0.3222 -1.0818001,0.81 -1.9278,0.81 -0.225,0 -0.4518,-0.0342 -0.666,-0.1152 -0.576,-0.2196 -0.9702,-0.5256 -1.3518001,-0.8208 -0.3024,-0.2358 -0.5886,-0.4572 -0.9378,-0.6084001 -0.0468,-0.0216 -0.0666,-0.0738 -0.0468,-0.1188 0.0198,-0.0468 0.072,-0.0666 0.1188,-0.0468 0.369,0.162 0.6642,0.3888001 0.9756,0.6318 0.3708,0.2880001 0.756,0.5850001 1.3050001,0.7938 0.5598,0.2124 1.1880001,0.0738 1.6650001,-0.1404 -0.2052,-0.6336 -1.5318002,-2.56139997 -3.7188,-2.0700002 -0.3384,0.0756 -0.6336,0.1458 -0.8928,0.207 -1.098,0.2574 -1.5678,0.3672001 -2.0556002,0.2376 0.045,0.2268001 0.1638,0.4194 0.3528,0.5742001 0.378,0.3042 0.927,0.3798 1.1880001,0.3834 C 3.8322,2.59038 3.69,2.49318 3.5154,2.43198 3.4686,2.41578 3.4434,2.36538 3.4596,2.31858 c 0.0162,-0.0486 0.0684,-0.072 0.1152,-0.0558 0.567,0.1998 0.828,0.7074 1.2492001,1.7424002 0.441,1.0836 1.9836001,2.0934 3.2346,1.7190001 1.2744,-0.3816 1.2528001,-1.7244 1.251,-1.7388 0,-0.036 0.0216,-0.0702 0.054,-0.0846 0.0846,-0.0378 0.5184,-0.2358 0.8243999,-0.5238 0.0054,-0.0054 0.0108,-0.009 0.0162,-0.009 v -0.0018 C 10.1898,3.27798 10.1646,3.20778 10.134,3.1591802 M 7.1496005,2.20878 c 0.0342,-0.0378 0.09,-0.0396 0.1278,-0.0072 0.2502,0.2268 0.3852,0.4698 0.3906,0.4806 0.0252,0.0432 0.009,0.0972 -0.0342,0.1224 -0.0144,0.0072 -0.0288,0.0108 -0.0432,0.0108 -0.0324,0 -0.063,-0.018 -0.0792,-0.0468 -0.0018,-0.0018 -0.1278,-0.2268 -0.3546,-0.4338 -0.037801,-0.0324 -0.0396,-0.09 -0.0072,-0.126 m -1.5156002,1.2258 c -0.0036,0 -0.0558,0.009 -0.1584,0.009 -0.081,0 -0.1908,-0.0054 -0.3312,-0.0234 -0.0504,-0.0054 -0.0846,-0.0504 -0.0774,-0.1008 0.0054,-0.0486 0.0504,-0.0828 0.1008,-0.0774 0.2916,0.0378 0.4338,0.0162 0.4356,0.0162 0.0486,-0.009 0.0954,0.0234 0.1026,0.0738 0.009,0.0486 -0.0234,0.0936 -0.072,0.1026 M 7.8156,4.51998 c -0.1872,0.2214 -0.2844,0.261 -0.432,0.3204 l -0.0468,0.0198 c -0.0108,0.0054 -0.0234,0.0072 -0.0342,0.0072 -0.036,0 -0.0684,-0.0216 -0.0828,-0.0558 -0.0198,-0.045 0.0018,-0.099 0.0486,-0.117 l 0.0468,-0.0198 c 0.135,-0.0558 0.2034,-0.0846 0.3636,-0.27 0.0324,-0.0396 0.09,-0.0432 0.1278,-0.0108 0.0378,0.0324 0.0414,0.0882 0.009,0.126 z'         id='path12'         style='fill:%23008000;fill-opacity:1' /%3E      %3Cpath         d='m 5.7060003,3.33198 c 0.009,0.0486 -0.0234,0.0936 -0.072,0.1026 -0.0036,0 -0.0558,0.009 -0.1584,0.009 -0.081,0 -0.1908,-0.0054 -0.3312,-0.0234 -0.0504,-0.0054 -0.0846,-0.0504 -0.0774,-0.1008 0.0054,-0.0486 0.0504,-0.0828 0.1008,-0.0774 0.2916,0.0378 0.4338,0.0162 0.4356,0.0162 0.0486,-0.009 0.0954,0.0234 0.1026,0.0738 z'         id='path14' /%3E    %3C/g%3E  %3C/g%3E%3C/svg%3E";

    private final String type;
    private final List<BpmnError> listBpmnErrors;
    Logger loggerAbstract = LoggerFactory.getLogger(AbstractRunner.class.getName());
    /**
     * For the Connector class, this information is calculated after the constructor
     */
    private List<RunnerParameter> listInput;
    /**
     * For the Connector class, this information is calculated after the constructor
     */
    private List<RunnerParameter> listOutput;
    private String name;
    private String description;
    /**
     * Give log please
     */
    private boolean isLogWorker = true;

    /**
     * Constructor
     *
     * @param type           name of the worker
     * @param listInput      list of Input parameters for the worker
     * @param listOutput     list of Output parameters for the worker
     * @param listBpmnErrors list of potential BPMN Error the worker can generate
     */

    protected AbstractRunner(String type,
                             List<RunnerParameter> listInput,
                             List<RunnerParameter> listOutput,
                             List<BpmnError> listBpmnErrors) {

        this.type = type;
        this.listInput = listInput;
        this.listOutput = listOutput;
        this.listBpmnErrors = listBpmnErrors;
    }


    /* -------------------------------------------------------- */
    /*                                                          */
    /*  getInput/setOutput                                      */
    /*                                                          */
    /* method to get variable value                             */
    /* -------------------------------------------------------- */

    /**
     * Return a value as Double
     *
     * @param parameterName name of the parameter
     * @param defaultValue  default value, if the variable does not exist or any error arrived (can't parse the value)
     * @param activatedJob  job passed to the worker
     * @return a Double value
     */
    public Double getInputDoubleValue(String parameterName, Double defaultValue, final ActivatedJob activatedJob) {
        if (!containsKeyInJob(parameterName, activatedJob))
            return (Double) getDefaultValue(parameterName, defaultValue);
        Object value = getValueFromJob(parameterName, activatedJob);
        if (value == null)
            return null;
        if (value instanceof Double valueDouble)
            return valueDouble;
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * return a value as a Map
     *
     * @param parameterName name of the parameter
     * @param defaultValue  default value, if the variable does not exist or any error arrived (can't parse the value)
     * @param activatedJob  job passed to the worker
     * @return a Map value
     */
    public Map getInputMapValue(String parameterName, Map defaultValue, final ActivatedJob activatedJob) {
        if (!containsKeyInJob(parameterName, activatedJob))
            return (Map) getDefaultValue(parameterName, defaultValue);

        Object value = getValueFromJob(parameterName, activatedJob);
        if (value == null)
            return null;
        if (value instanceof Map valueMap)
            return valueMap;

        return defaultValue;
    }

    /**
     * Return a value as Long
     *
     * @param parameterName name of the variable
     * @param defaultValue  default value, if the variable does not exist or any error arrived (can't parse the value)
     * @param activatedJob  job passed to the worker
     * @return a Double value
     */
    public Long getInputLongValue(String parameterName, Long defaultValue, final ActivatedJob activatedJob) {
        if (!containsKeyInJob(parameterName, activatedJob))
            return (Long) getDefaultValue(parameterName, defaultValue);
        Object value = getValueFromJob(parameterName, activatedJob);

        if (value == null)
            return null;
        if (value instanceof Long valueLong)
            return valueLong;
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Return a value as Duration. The value may be a Duration object, or a time in ms (LONG) or a ISO 8601 String representing the duration
     * https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-
     * https://fr.wikipedia.org/wiki/ISO_8601
     *
     * @param parameterName name of the variable
     * @param defaultValue  default value, if the variable does not exist or any error arrived (can't parse the value)
     * @param activatedJob  job passed to the worker
     * @return a Double value
     */
    public Duration getInputDurationValue(String parameterName, Duration defaultValue, final ActivatedJob activatedJob) {
        if (!containsKeyInJob(parameterName, activatedJob))
            return (Duration) getDefaultValue(parameterName, defaultValue);
        Object value = getValueFromJob(parameterName, activatedJob);
        if (value == null)
            return null;
        if (value instanceof Duration valueDuration)
            return valueDuration;
        if (value instanceof Long valueLong)
            return Duration.ofMillis(valueLong);
        try {
            return Duration.parse(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * get the FileVariable.
     * The file variable may be store in multiple storage. The format is given in the parameterStorageDefinition. This is a String which pilot
     * how to load the file. The value can be saved a JSON, or saved in a specific directory (then the value is an ID)
     *
     * @param parameterName name where the value is stored
     * @param activatedJob  job passed to the worker
     * @return a FileVariable
     */
    public FileVariable getFileVariableValue(String parameterName, final ActivatedJob activatedJob) throws ZeebeBpmnError {
        if (!containsKeyInJob(parameterName, activatedJob))
            return null;
        Object fileVariableReferenceValue = getValueFromJob(parameterName, activatedJob);
        try {
            FileVariableReference fileVariableReference = FileVariableReference.fromJson(fileVariableReferenceValue.toString());

            if (fileVariableReference == null)
                return null;

            return FileVariableFactory.getInstance().getFileVariable(fileVariableReference);
        } catch (Exception e) {
            throw new ZeebeBpmnError(BPMNERROR_ACCESS_FILEVARIABLE, "Worker [" + getName() + "] error during access fileVariableReference[" + fileVariableReferenceValue + "] :" + e);
        }
    }

    /**
     * return a variable value
     *
     * @param parameterName name of the input value
     * @param defaultValue  if the input does not exist, this is the default value.
     * @param activatedJob  job passed to the worker
     * @return the value as String
     */
    public Object getValue(String parameterName, Object defaultValue, ActivatedJob activatedJob) {
        if (!containsKeyInJob(parameterName, activatedJob))
            return getDefaultValue(parameterName, defaultValue);
        try {
            return getValueFromJob(parameterName, activatedJob);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Return the defaultValue for a parameter. If the defaultValue is provided by the software, it has the priority.
     * Else the default value is the one given in the parameter.
     *
     * @param parameterName name of parameter
     * @param defaultValue  default value given by the software
     * @return the default value
     */
    private Object getDefaultValue(String parameterName, Object defaultValue) {
        // if the software give a default value, it has the priority
        if (defaultValue != null)
            return defaultValue;
        List<RunnerParameter> inputFilter = listInput.stream().filter(t -> t.name.equals(parameterName)).toList();
        if (!inputFilter.isEmpty())
            return inputFilter.get(0).defaultValue;
        // definitively, no default value
        return null;
    }

    /**
     * Set the value. Worker must use this method, then the class can verify the output contract is respected
     *
     * @param parameterName name of the variable
     * @param value         value of the variable
     */
    public void setValue(String parameterName, Object value, AbstractWorker.ContextExecution contextExecution) {
        contextExecution.outVariablesValue.put(parameterName, value);
    }

    /**
     * Set a fileVariable value
     *
     * @param parameterName     name to save the fileValue
     * @param storageDefinition parameter which pilot the way to retrieve the value
     * @param fileVariableValue fileVariable to save
     */
    public void setFileVariableValue(String parameterName, String storageDefinition, FileVariable fileVariableValue, AbstractWorker.ContextExecution contextExecution) {
        try {
            FileVariableReference fileVariableReference = FileVariableFactory.getInstance().setFileVariable(storageDefinition, fileVariableValue);
            contextExecution.outVariablesValue.put(parameterName, fileVariableReference.toJson());
        } catch (Exception e) {
            logError("parameterName[" + parameterName + "] Error during setFileVariable read: " + e);
            throw new ZeebeBpmnError(BPMNERROR_SAVE_FILEVARIABLE, "Worker [" + getName() + "] error during access storageDefinition[" + storageDefinition + "] :" + e);
        }
    }

    /**
     * Log an error
     *
     * @param message message to log
     */
    public void logError(String message) {
        loggerAbstract.error("CherryWorker[" + getName() + "]: " + message);
    }

    /**
     * Check the contract
     * Each connector must and return a contract for what it needs for the execution
     *
     * @throws RuntimeException if the input is incorrect, contract not respected
     */
    protected void checkInput(final ActivatedJob job) throws RuntimeException {
        List<String> listErrors = new ArrayList<>();
        for (RunnerParameter parameter : getListInput()) {
            // if a parameter is a star, then this is not really a name
            if ("*".equals(parameter.name))
                continue;

            // value is in Variables if the designer map Input and Output manually
            // or may be in the custom headers if the designer use a template
            Object value = getValueFromJob(parameter.name, job);

            // check type
            if (value != null && incorrectClassParameter(value, parameter.clazz)) {
                listErrors.add("Param[" + parameter.name + "] expect class[" + parameter.clazz.getName() + "] received[" + value.getClass() + "];");
            }


            // check REQUIRED parameters
            if ((value == null || value.toString().trim().length() == 0) && parameter.level == RunnerParameter.Level.REQUIRED) {
                listErrors.add("Param[" + parameter.name + "] is missing");
            }
        }
        if (!listErrors.isEmpty()) {
            logError("CherryConnector[" + getType() + "] Errors:" + String.join(",", listErrors));
            throw new ZeebeBpmnError("INPUT_CONTRACT_ERROR", "Worker [" + getType() + "] InputContract Exception:" + String.join(",", listErrors));
        }
    }

    /**
     * Check the contract at output
     * The connector must use setVariable to set any value. Then, we can verify that all expected information are provided
     *
     * @param contextExecution keep the context of this execution
     * @throws RuntimeException when the contract is not respected
     */
    protected void checkOutput(AbstractWorker.ContextExecution contextExecution) throws RuntimeException {
        List<String> listErrors = new ArrayList<>();

        for (RunnerParameter parameter : getListOutput()) {

            // no check on the * parameter
            if ("*".equals(parameter.name))
                continue;

            if (parameter.level == RunnerParameter.Level.REQUIRED && !contextExecution.outVariablesValue.containsKey(parameter.name)) {
                listErrors.add("Param[" + parameter.name + "] is missing");
            }
            // if the value is given, it must be the correct value
            if (contextExecution.outVariablesValue.containsKey(parameter.name)) {
                Object value = contextExecution.outVariablesValue.get(parameter.name);

                if (incorrectClassParameter(value == null ? null : value.getClass().getName(), parameter.clazz))
                    listErrors.add("Param[" + parameter.name + "] expect class[" + parameter.clazz.getName()
                            + "] received[" + contextExecution.outVariablesValue.get(parameter.name).getClass() + "];");

            }
        }
        Set<String> outputName = getListOutput().stream().map(t -> t.name).collect(Collectors.toSet());
        // second pass: verify that the connector does not provide an unexpected value
        // if a outputParameter is "*" then the connector allows itself to produce anything
        long containsStar = getListOutput().stream().filter(t -> "*".equals(t.name)).count();
        if (containsStar == 0) {
            List<String> listExtraVariables = contextExecution.outVariablesValue.keySet()
                    .stream()
                    .filter(variable -> !outputName.contains(variable))
                    .toList();
            if (!listExtraVariables.isEmpty())
                listErrors.add("Output not defined in the contract[" + String.join(",", listExtraVariables) + "]");
        }


        if (!listErrors.isEmpty()) {
            logError("Errors:" + String.join(",", listErrors));
            throw new ZeebeBpmnError("OUTPUT_CONTRACT_ERROR", "Worker[" + getType() + "] OutputContract Exception:" + String.join(",", listErrors));
        }
    }



    /* -------------------------------------------------------- */
    /*                                                          */
    /*  Runner parameters                                       */
    /*                                                          */
    /* Runner must declare the input/output parameters          */
    /* -------------------------------------------------------- */
    // If inputs and/or outputs are mapped as literals in the bpmn process diagram, the types are ambiguous. For example,
    // the value of `90` will be interpreted as an Integer, but we also need a way to interpret as a Long.

    // This idea was inspired by: https://stackoverflow.com/questions/40402756/check-if-a-string-is-parsable-as-another-java-type
    static Map<Class<?>, Predicate<String>> canParsePredicates = new HashMap<>();

    static {
        canParsePredicates.put(java.lang.Integer.class, s -> {
            try {
                Integer.parseInt(s);
                return true;
            } catch (Exception e) {
                return false;
            }
        });
        canParsePredicates.put(java.lang.Long.class, s -> {
            try {
                Long.parseLong(s);
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    static Boolean canParse(Class<?> clazz, Object value) {
        if (value != null) {
            return canParsePredicates.get(clazz).test(value.toString());
        } else {
            return false;
        }
    }

    /**
     * Check the object versus the expected parameter
     *
     * @param value        object value to check
     * @param isInstanceOf expected class
     * @return false if the value is on the class, else true
     */
    boolean incorrectClassParameter(Object value, Class<?> isInstanceOf) {
        if (value == null)
            return false;
        try {
            if (Class.forName(isInstanceOf.getName()).isInstance(value)) {
                return false;
            } else {
                return !canParse(isInstanceOf, value.toString());
            }
        } catch (Exception e) {
            // do nothing, we return true
        }
        return true;
    }


    private boolean containsKeyInJob(String parameterName, final ActivatedJob activatedJob) {
        return (activatedJob.getVariablesAsMap().containsKey(parameterName)
                || activatedJob.getCustomHeaders().containsKey(parameterName));
    }

    /**
     * Value is in Variables if the designer map Input and Output manually,
     * or may be in the custom headers if the designer use a template
     *
     * @param parameterName parameter to get the value
     * @param activatedJob  activated job
     * @return
     */
    protected Object getValueFromJob(String parameterName, final ActivatedJob activatedJob) {
        if (activatedJob.getVariablesAsMap().containsKey(parameterName))
            return activatedJob.getVariablesAsMap().get(parameterName);
        return activatedJob.getCustomHeaders().get(parameterName);
    }

    /**
     * Retrieve a variable, and return the string representation. If the variable is not a String, then a toString() is returned. If the value does not exist, then defaultValue is returned
     * The method can return null if the variable exists, but it is a null value.
     *
     * @param parameterName name of the variable to load
     * @param defaultValue  if the input does not exist, this is the default value.
     * @param activatedJob  job passed to the worker
     * @return the value as String
     */
    public String getInputStringValue(String parameterName, String defaultValue, final ActivatedJob activatedJob) {
        if (!containsKeyInJob(parameterName, activatedJob))
            return (String) getDefaultValue(parameterName, defaultValue);
        Object value = getValueFromJob(parameterName, activatedJob);
        return value == null ? null : value.toString();
    }



    /* -------------------------------------------------------- */
    /*                                                          */
    /*  Getter/Setter                                           */
    /*                                                          */
    /* -------------------------------------------------------- */

    /* isLog
     * return if the  worker will log
     */
    public boolean isLog() {
        return isLogWorker;
    }

    public void setLog(boolean logWorker) {
        isLogWorker = logWorker;
    }


    public String getType() {
        return type;
    }

    public List<RunnerParameter> getListInput() {
        return listInput;
    }

    public void setListInput(List<RunnerParameter> listInput) {
        this.listInput = listInput;
    }

    public List<RunnerParameter> getListOutput() {
        return listOutput;
    }

    public void setListOutput(List<RunnerParameter> listOutput) {
        this.listOutput = listOutput;
    }

    public List<BpmnError> getListBpmnErrors() {
        return listBpmnErrors;
    }

    /**
     * Return the list of variable to fetch if this is possible, else null.
     * To calculate the list:
     * - the listInput must not be null (it may be empty)
     * - any input must be a STAR. A star in the input means the worker/connector ask to retrieve any information
     *
     * @return null if this is not possible to fetch variable, else the list of variable to fetch
     */
    public List<String> getListFetchVariables() {
        if (listInput == null)
            return null;
        boolean isStarPresent = listInput.stream().anyMatch(t -> t.name.equals("*"));
        if (isStarPresent)
            return null;
        return listInput.stream().map(RunnerParameter::getName).toList();
    }



    /* -------------------------------------------------------- */
    /*                                                          */
    /*  Additional optional information                        */
    /*                                                          */
    /* -------------------------------------------------------- */

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return the identification for the worker: it is the name or, if this not exist, the type
     *
     * @return the identification
     */
    public String getIdentification() {
        return name == null ? type : name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Image must be a string like
     * "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='18' height='18.0' viewBox='0 0 18 18.0' %3E%3Cg id='XMLID_238_'%3E %3Cpath id='XMLID_239_' d='m 14.708846 10.342394 c -1.122852 0.0,-2.528071 0.195852,-2.987768 0.264774 C 9.818362 8.6202,9.277026 7.4907875,9.155265 7.189665 C 9.320285 6.765678,9.894426 5.155026,9.976007 3.0864196 C 10.016246 2.0507226,9.797459 1.2768387,9.325568 ....   -0.00373,0.03951 0.00969,0.0425 0.030567 z'/%3E%3C/svg%3E";
     *
     * @return
     */
    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    /* -------------------------------------------------------- */
    /*                                                          */
    /*  Definition information                                  */
    /*                                                          */
    /* -------------------------------------------------------- */

    /**
     * produce a JSON string containing the definition for the template
     * https://docs.camunda.io/docs/components/modeler/desktop-modeler/element-templates/defining-templates/
     *
     * @return
     */
    public String getTemplate() {
        Map<String, Object> templateContent = new HashMap<>();
        templateContent.put("$schema", "https://unpkg.com/@camunda/zeebe-element-templates-json-schema/resources/schema.json");
        templateContent.put("name", getIdentification());
        templateContent.put("id", getClass().getName());
        templateContent.put("description", getDescription());
        templateContent.put("documentationRef", "https://docs.camunda.io/docs/components/modeler/web-modeler/connectors/available-connectors/template/");
        if (getLogo() != null)
            templateContent.put("icon", Map.of("contents", getLogo()));
        templateContent.put("category", Map.of("id", "connectors", "name", "connectors"));
        templateContent.put("appliesTo", List.of("bpmn:Task"));
        templateContent.put("elementType", Map.of("value", "bpmn:ServiceTask"));
        // no groups at this moment
        List<Map<String, Object>> listProperties = new ArrayList<>();
        templateContent.put("properties", listProperties);
        listProperties.add(
                Map.of("type", "Hidden",
                        "value", getType(),
                        "binding", Map.of("type", "zeebe:taskDefinition:type")));

        listProperties.addAll(listInput.stream().map(p -> getParameterTemplate(p, true)).toList());
        listProperties.addAll(listOutput.stream().map(p -> getParameterTemplate(p, false)).toList());

        // transform the result in JSON
        Gson gson = new Gson();

        return gson.toJson(templateContent);
    }


    /**
     * Get the template from a runnerParameter
     *
     * @param runnerParameter runner parameter to get the description
     * @param isInput         true if this is an input parameter
     * @return a template description
     */
    private Map<String, Object> getParameterTemplate(RunnerParameter runnerParameter, boolean isInput) {
        Map<String, Object> onePropertie = new HashMap<>();
        onePropertie.put("id", runnerParameter.name);
        onePropertie.put("label", runnerParameter.label);
        // don't have the group at this moment
        onePropertie.put("description", runnerParameter.explanation);
        String typeParameter = "String";
        // String, Text, Boolean, Dropdown or Hidden)
        if (Boolean.class.equals(runnerParameter.clazz))
            typeParameter = "Boolean";
        else if (runnerParameter.hasChoice()) {
            typeParameter = "Dropdown";
            // add choices
            List<Map<String, String>> listChoices = new ArrayList<>();
            for (RunnerParameter.WorkerParameterChoice oneChoice : runnerParameter.workerParameterChoiceList) {
                listChoices.add(Map.of("name", oneChoice.displayName,
                        "value", oneChoice.code));
            }
            onePropertie.put("choices", listChoices);
        }
        onePropertie.put("type", typeParameter);
        if (isInput) {
            onePropertie.put("binding",
                    Map.of("type", "zeebe:input",
                            "name", runnerParameter.name));
        } else {
            onePropertie.put("binding",
                    Map.of("type", "zeebe:output",
                            "source", "= "+runnerParameter.name));

        }

        Map<String, Object> constraints = new HashMap<>();
        if (runnerParameter.level == RunnerParameter.Level.REQUIRED)
            constraints.put("notEmpty", Boolean.TRUE);

        if (!constraints.isEmpty())
            onePropertie.put("constraints", constraints);
        return onePropertie;
    }
}
