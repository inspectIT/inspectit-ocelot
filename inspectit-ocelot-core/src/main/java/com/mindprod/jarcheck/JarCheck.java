package com.mindprod.jarcheck;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.System.*;
/*
TODO: check class minor version as well.
     */

/**
 * Ensures javac -target versions of the class files in a jar are as expected.
 *
 * @author Roedy Green, Canadian Mind Products
 * @version 1.5 2014-03-23 add support for Java 1.8
 * @since 2006-01-16
 */
public final class JarCheck
{
    /**
     * how many bytes at beginning of class file we read<br> 4=ca-fe-ba-be + 2=minor + 2=major
     */
    private static final int chunkLength = 8;

    private static final int FIRST_COPYRIGHT_YEAR = 2006;

    /**
     * undisplayed copyright notice
     */
    private static final String EMBEDDED_COPYRIGHT =
            "Copyright: (c) 2006-2017 Roedy Green, Canadian Mind Products, http://mindprod.com";

    private static final String RELEASE_DATE = "2014-03-23";

    /**
     * embedded version string.
     */
    private static final String VERSION_STRING = "1.5";

    /**
     * translate class file major version number to human JVM version
     */
    private static final HashMap<Integer, String> convertMachineToHuman =
            new HashMap<>( 23 );

    /**
     * translate from human JDK version to class file major version number
     */
    private static final HashMap<String, Integer> convertHumanToMachine =
            new HashMap<>( 23 );

    /**
     * expected first 4 bytes of a class file
     */
    private static final byte[] expectedMagicNumber =
            { ( byte ) 0xca, ( byte ) 0xfe, ( byte ) 0xba, ( byte ) 0xbe };

    static
    {
        convertHumanToMachine.put( "1.0", 44 );
        convertHumanToMachine.put( "1.1", 45 );
        convertHumanToMachine.put( "1.2", 46 );
        convertHumanToMachine.put( "1.3", 47 );
        convertHumanToMachine.put( "1.4", 48 );
        convertHumanToMachine.put( "1.5", 49 );
        convertHumanToMachine.put( "1.6", 50 );
        convertHumanToMachine.put( "1.7", 51 );
        convertHumanToMachine.put( "1.8", 52 );
    }

    static
    {
        convertMachineToHuman.put( 44, "1.0" );
        convertMachineToHuman.put( 45, "1.1" );
        convertMachineToHuman.put( 46, "1.2" );
        convertMachineToHuman.put( 47, "1.3" );
        convertMachineToHuman.put( 48, "1.4" );
        convertMachineToHuman.put( 49, "1.5" );
        convertMachineToHuman.put( 50, "1.6" );
        convertMachineToHuman.put( 51, "1.7" );
        convertMachineToHuman.put( 52, "1.8" );
    }

    /**
     * check one jar to make sure all class files have compatible versions.
     *
     * @param jarFilename name of jar file whose classes are to be tested.
     * @param low         low bound for major version e.g. 44
     * @param high        high bound for major version. e.g. 50
     *
     * @return true if all is ok. False if not, with long on System.err of problems.
     */
    private static boolean checkJar( String jarFilename, int low, int high )
    {
        out.println( "\nChecking jar " + jarFilename );
        boolean success = true;
        FileInputStream fis;
        ZipInputStream zip = null;
        try
        {
            try
            {
                fis = new FileInputStream( jarFilename );
                zip = new ZipInputStream( fis );
                // loop for each jar entry
                entryLoop:
                while ( true )
                {
                    ZipEntry entry = zip.getNextEntry();
                    if ( entry == null )
                    {
                        break;
                    }
                    // relative name with slashes to separate dirnames.
                    String elementName = entry.getName();
                    if ( !elementName.endsWith( ".class" ) )
                    {
                        // ignore anything but a .final class file
                        continue;
                    }

                    if(elementName.equals("module-info.class")) {
                        // Allows jar files that are compatible to Java 8 and Java 9+ at the same time.
                        // Problem was discovered as JarCheck failed for tomcat-embed-el-9.0.71.jar.
                        // Evidences why this should work:
                        // * https://stackoverflow.com/a/47359176 (last paragraph)
                        // * According to https://tomcat.apache.org/whichversion.html Tomcat 9.x is
                        //   compatible to Java 8.
                        // * The Spring guys include tomcat-embed-el-9.0.71.jar in SpringBoot 2.7.8
                        //   and claim to be Java 8 compatible.
                        out.println("Ignoring module-info.class for "+ elementName);
                        continue ;
                    }
                    byte[] chunk = new byte[ chunkLength ];
                    int bytesRead = zip.read( chunk, 0, chunkLength );
                    zip.closeEntry();
                    if ( bytesRead != chunkLength )
                    {
                        err.println( ">> Corrupt class file: "
                                + elementName );
                        success = false;
                        continue;
                    }
                    // make sure magic number signature is as expected.
                    for ( int i = 0; i < expectedMagicNumber.length; i++ )
                    {
                        if ( chunk[ i ] != expectedMagicNumber[ i ] )
                        {
                            err.println( ">> Bad magic number in "
                                    + elementName );
                            success = false;
                            continue entryLoop;
                        }
                    }
                    /*
                     * pick out big-endian ushort major version in last two
                     * bytes of chunk
                     */
                    int major =
                            ( ( chunk[ chunkLength - 2 ] & 0xff ) << 8 ) + (
                                    chunk[ chunkLength - 1 ]
                                            & 0xff );
                    /* F I N A L L Y. All this has been leading up to this TEST */
                    if ( low <= major && major <= high )
                    {
                        out.print( "   OK " );
                        out.println( convertMachineToHuman.get( major )
                                + " ("
                                + major
                                + ") "
                                + elementName );
                        // leave success set as previously
                    }
                    else
                    {
                        err.println( ">> Wrong Version " );
                        err.println( convertMachineToHuman.get( major )
                                + " ("
                                + major
                                + ") "
                                + elementName );
                        success = false;
                    }
                }
                // end while
            }
            catch ( EOFException e )
            {
                // normal exit
            }
            zip.close();
            return success;
        }
        catch ( IOException e )
        {
            err.println( ">> Problem reading jar file." );
            return false;
        }
    }

    /**
     * Main command line jarfileName lowVersion highVersion e.g. myjar.jar 1.0 1.8
     *
     * @param args rot used
     */
    public static void main( String[] args )
    {
        try
        {
            if ( args.length != 3 )
            {
                err.println( "usage: java -ea -jar jarcheck.jar jarFileName.jar 1.1 1.7" );
                System.exit( 2 );
            }
            String jarFilename = args[ 0 ];
            int low = convertHumanToMachine.get( args[ 1 ] );
            int high = convertHumanToMachine.get( args[ 2 ] );
            boolean success = checkJar( jarFilename, low, high );
            if ( !success )
            {
                err.println("JarCheck failed for " + jarFilename+". See error messages above.");
                System.exit( 1 );
            }
        }
        catch ( NullPointerException e )
        {
            err.println( "usage: java -ea -jar jarcheck.jar jarFileName.jar 1.1 1.8" );
            System.exit( 2 );
        }
    }
}
