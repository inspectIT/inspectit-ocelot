package rocks.inspectit.ocelot.core.logging.logback;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.event.SaxEventRecorder;
import ch.qos.logback.core.joran.spi.JoranException;
import com.bluecast.xml.JAXPSAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import java.io.IOException;

/**
 * Extension of the {@link SaxEventRecorder} that uses {@link com.bluecast.xml.JAXPSAXParserFactory}
 * for parsing the input stream. This is a small hack to always use Piccolo parser without
 * interfering with the system properties. Most of this class is copied from the
 * {@link SaxEventRecorder} class.
 *
 * @author Ivan Senic
 */
public class PiccoloSaxEventRecorder extends SaxEventRecorder {

    /**
     * Constructor.
     *
     * @param context Context
     * @see SaxEventRecorder#SaxEventRecorder(Context)
     */
    public PiccoloSaxEventRecorder(Context context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Method is same as {@link SaxEventRecorder#recordEvents(InputSource)} except that it calls the
     * {@link #getSaxParser()} method for creating the parser.
     */
    @Override
    public void recordEvents(InputSource inputSource) throws JoranException {
        SAXParser saxParser = getSaxParser();
        try {
            saxParser.parse(inputSource, this);
            return;
        } catch (IOException ie) {
            handleError("I/O error occurred while parsing xml file", ie);
        } catch (SAXException se) {
            // Exception added into StatusManager via Sax error handling. No need to add it again
            throw new JoranException("Problem parsing XML document. See previously reported errors.", se);
        } catch (Exception ex) {
            handleError("Unexpected exception while parsing XML document.", ex);
        }
        throw new IllegalStateException("This point can never be reached");
    }

    /**
     * Method copied from {@link SaxEventRecorder} to maintain same implementation as in the
     * original recorder.
     *
     * @param errMsg message
     * @param t      throwable
     * @throws JoranException Throws checked exception.
     */
    private void handleError(String errMsg, Throwable t) throws JoranException {
        addError(errMsg, t);
        throw new JoranException(errMsg, t);
    }

    /**
     * Creates the {@link SAXParser}.This method directly uses
     * {@link com.bluecast.xml.JAXPSAXParserFactory} as the parser factory. Rest of the method is
     * taken from {@link SaxEventRecorder#buildSaxParser} method.
     *
     * @return {@link SAXParser}
     * @throws JoranException If exception occurs creating the parser.
     */
    private SAXParser getSaxParser() throws JoranException {
        try {
            JAXPSAXParserFactory spf = new JAXPSAXParserFactory();
            spf.setValidating(false);
            //spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            // See LOGBACK-1465
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            spf.setNamespaceAware(true);
            return spf.newSAXParser();
        } catch (
        ParserConfigurationException pce) {
            String errMsg = "Error during SAX paser configuration. See https://logback.qos.ch/codes.html#saxParserConfiguration";
            addError(errMsg, pce);
            throw new JoranException(errMsg, pce);
        } catch (SAXException pce) {
            String errMsg = "Error during parser creation or parser configuration";
            addError(errMsg, pce);
            throw new JoranException(errMsg, pce);
        }
    }
}
