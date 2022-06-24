/* ******************************************************************** */
/*                                                                      */
/*  FileVariable                                                        */
/*                                                                      */
/*  File variable contains the file. Attention, file is in memory then  */
/* ******************************************************************** */
package org.camunda.cherry.definition.filevariable;

import java.io.File;
import java.net.URLConnection;

/* Definition of a FileVariable */
public class FileVariable {
    public String name;
    public String mimeType;
    public byte[] value;


    /**
     * Return the suffix of the file, based on the name or on the mimeType
     *
     * @return the suffix
     */
    public static String getSuffix(String fileName) {
        if (fileName != null) {
            int lastDot = fileName.lastIndexOf(".");
            if (lastDot != -1)
                return fileName.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * return the Mimetype from the name.
     *
     * @return the mime type
     */
    public static String getMimeTypeFromName(String fileName) {
        File file = new File(fileName);
        return URLConnection.guessContentTypeFromName(file.getName());

    }
}
