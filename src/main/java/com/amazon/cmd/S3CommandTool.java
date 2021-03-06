package com.amazon.cmd;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.Grant;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.logging.Level;

import static com.amazon.cmd.S3Connector.s3Logger;

public class S3CommandTool {

    private static Properties configProperties = new Properties();

    private static String getHelp(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("USAGE INSTRUCTION:\ns3tool -command [args...] -options\n\n")
            .append("COMMANDS:\n")
            .append("\t-l:\t\tlists all buckets in the instance\n")
            .append("\t-c:\t\tcreates a new bucket, args: [BUCKET_NAME]\n")
            .append("\t-d:\t\tlists all keys with the prefix ('/' for root bucket directory)\n")
            .append("\t-g:\t\tdownloads file or directory from Amazon S3 server, args: [FILE] [S3_KEY] \n")
            .append("\t-p:\t\tuploads file or directory to Amazon S3 server, args: [FILE_NAME] [S3_KEY]\n")
            .append("\t-i:\t\tgets info on object [BUCKET_NAME] [SE_KEY]\n")
            .append("\t-s:\t\tgets bucket's permission list, args: [BUCKET_NAME]\n")
            .append("\t-o:\t\tgets object permission list, args: [BUCKET_NAME]\n")
            .append("\t-b:\t\tdefault bucket change, args: [BUCKET_NAME]\n")
            .append("\nOPTIONS:\n")
            .append("\t/f:\t\tforce disable SSL connection");
        return stringBuilder.toString();
    }

    private static void setProperties(String propertyName, String valueString){
        OutputStream outputStream = null;
        try {
            s3Logger.log(Level.INFO,"Setting properties in properties.xml");
            outputStream = new FileOutputStream(new File("properties.xml"));
            configProperties.setProperty(propertyName, valueString);
            configProperties.storeToXML(outputStream,"update "+new Date());
        } catch (IOException e) {
            s3Logger.log(Level.WARNING,e.getLocalizedMessage());
        }
    }

    private static Properties getProperties(){
        InputStream inputStream = null;
        try {
            s3Logger.log(Level.INFO,"Reading properties from properties.xml");
            inputStream = new FileInputStream(new File("properties.xml"));
            configProperties.loadFromXML(inputStream);
        } catch (IOException e) {
            s3Logger.log(Level.WARNING,e.getLocalizedMessage());
        }
        return configProperties;
    }

    public static void main(String[] args) {
/*
        String source ="wf_ocr/BP/fSS-EUR Claims OCR FBNL Tagging XML - CSF DEBIT_NOTE/" +
                "test_set/f796005cf90fd72edf00efd51e7f8a80DO_08782_20180413053028_7.pdf";
        String dest="C:\\Users\\Artur_Wojciechowski\\Desktop\\";
        String putArg = "C:\\Users\\Artur_Wojciechowski\\Desktop\\test.pdf";
*/
        args = new String[]{
                "-l",
                "/f"};

        if (args.length>0 && args[args.length-1].equals("/f"))
            System.setProperty("com.amazonaws.sdk.disableCertChecking", "true");

        System.out.println("------------------------ AMAZON S3 COMMAND LINE CLIENT: -----------------------\n");
        configProperties = getProperties();

        ConnectionConfiguration connectionConfig = new ConnectionConfiguration() {
            @Override
            public String getHostConfig() {
                return configProperties.get("host").toString();
            }

            @Override
            public String getBucketNameConfig() {
                return configProperties.get("bucket").toString();
            }

            @Override
            public String getPublicKeyConfig() {
                return configProperties.get("public").toString();
            }

            @Override
            public String getPrivateKeyConfig() { return configProperties.get("private").toString(); }
        };

        S3Connector s3Connector = S3Connector.Configure(connectionConfig).S3ConnectionBuilder();
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("S3 HOST NAME:\t\t\t"+s3Connector.getHost());
        System.out.println("S3 DEFAULT BUCKET:\t\t"+s3Connector.getBucketName());
        System.out.println("-------------------------------------------------------------------------------");
        try {
            if (args.length < 1) throw new IllegalArgumentException();
            switch (args[0]) {
                case "-l":
                    if (args.length==1 || args[1].equals("/f"))
                        s3Connector.getBuckets().forEach(cons -> System.out.println(cons.getName()));
                    else throw new IllegalArgumentException();
                    break;
                case "-h":
                    if (args.length==1)
                        System.out.println(getHelp());
                    else throw new IllegalArgumentException();
                    break;
                case "-d":
                    if (args.length==2 || (args.length==3 && args[2].equals("/f")))
                        s3Connector.getDirectory(args[1]).forEach(cons -> System.out.println(cons.getKey()));
                    else throw new IllegalArgumentException();
                    break;
                case "-c":
                    if (args.length==2 || (args.length==3 && args[2].equals("/f"))) {
                        s3Connector.createBucket(args[1]);
                    } else throw new IllegalArgumentException();
                    break;
                case "-g":
                    if (args.length==3 || (args.length==4 && args[3].equals("/f"))) {
                        s3Connector.getFile(args[1], args[2] +
                                args[1].substring(args[1].lastIndexOf("/") + 1));
                    }
                    else throw new IllegalArgumentException();
                    break;
                case "-p":
                    if (args.length==3 || (args.length==4 && args[3].equals("/f"))) {
                        s3Connector.putFile(args[1], args[2]);
                    } else throw new IllegalArgumentException();
                    break;
                case "-s":
                    if (args.length==2 || (args.length==3 && args[2].equals("/f"))) {
                        s3Connector.getBucketPermissions(args[1])
                                .forEach(cons-> System.out.format("  %s: %s\n",
                                        cons.getGrantee().getIdentifier(),
                                        cons.getPermission().toString()));
                    } else throw new IllegalArgumentException();
                    break;
                case "-o":
                    if (args.length==3 || (args.length==4 && args[3].equals("/f"))) {
                        s3Connector.getObjectPermissions(args[1],args[2])
                                .forEach(cons-> System.out.format("  %s: %s\n",
                                cons.getGrantee().getIdentifier(),
                                    cons.getPermission().toString()));
                    } else throw new IllegalArgumentException();
                    break;
                case "-i":
                    if (args.length==3 || (args.length==4 && args[3].equals("/f"))) {
                        s3Connector.getInfo(args[1],args[2]);
                    } else throw new IllegalArgumentException();
                    break;
                case "-b":
                    if (args.length==2 || args[2].equals("/f")) {
                        setProperties("bucket", args[1]);
                        s3Connector.setBucketName(configProperties.getProperty("bucket"));
                        System.out.println("BUCKET CHANGED TO: " + s3Connector.getBucketName());
                    } else throw new IllegalArgumentException();
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            System.setProperty("com.amazonaws.sdk.disableCertChecking", "false");
            System.out.println("-------------------------------------------------------------------------------");
            
        } catch (IllegalArgumentException e){
            s3Logger.log(Level.WARNING,"Invalid argument list");
            getHelp();
        } catch (AmazonServiceException ase) {
            s3Logger.log(Level.WARNING, S3ExceptionService.getOutputMessage(ase));
        } catch (AmazonClientException ace) {
            s3Logger.log(Level.WARNING, S3ExceptionService.getOutputMessage(ace));
        }
    }
}
