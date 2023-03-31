This directory contains test files for rocks.inspectit.ocelot.agentcommunication.GrpcSslConfigurationTest

It is crucial, that the file private_pcks1.key contains a private key encoded using PCKS1. JDK classes are not able to
read this encoding. Only PCKS8 is supported.

File certificate.cert exists only, because it is necessary to call the test code successfully.

In case you ever need to generate a private key encoded PCKS1, I used the following command to convert a key generated
with openssl:
openssl pkey -in <your-pCKS8-encoded-key> -traditional -out private_pcks1.key
