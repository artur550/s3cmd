package com.amazon.cmd;public interface ConnectionConfiguration {    String hostConfig=new String();    String bucketName=new String();    String publicKey=new String();    String privateKey=new String();    String get=new String();    String getHostConfig();    String getBucketNameConfig();    String getPublicKeyConfig();    String getPrivateKeyConfig();}