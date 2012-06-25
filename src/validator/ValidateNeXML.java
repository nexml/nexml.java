package validator;

import java.io.File;
import java.io.FileReader;

import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.InputSource;

public class ValidateNeXML {

	public static String againstXSD(final String xmlFilepath, final String xsdFilepath, final String namespace) {
		try {
			// make sure both paths specify a valid file
			// File config_fp = new File(xmlFilepath);
			// if (!config_fp.exists())
			// 	return "Unable to open file " + xmlFilepath;
			// config_fp = new File(xsdFilepath);
			// if (!config_fp.exists())
			// 	return "Unable to open file " + xsdFilepath;
			final DOMParser parser = new DOMParser();
			parser.setFeature("http://xml.org/sax/features/validation", true);
			parser.setFeature("http://apache.org/xml/features/validation/schema", true);
			if (namespace == null || namespace.trim().equals(""))
				parser.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation", xsdFilepath);
			else
				parser.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation", namespace.trim() + " " + xsdFilepath);
			parser.parse(new InputSource(new FileReader(xmlFilepath)));
			return null;
		}
		catch (final Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	public static void main(final String args[]) {
		if ( args.length == 0 ) {
			System.out.println("Usage: NexmlValidator <filename> [schema] [namespace]");
			System.exit(1);
		}
		String xmlFilepath = args[0];
		String schemaPath;
		String namespace;
		if (args.length > 1) {
			schemaPath = args[1];
		} else {
			schemaPath = "http://www.nexml.org/nexml/xsd/nexml.xsd";
		}
		if (args.length > 2) {
			namespace = args[2];
		} else {
			namespace = "http://www.nexml.org/2009";
		}
		final String retval = againstXSD(xmlFilepath, schemaPath, namespace);
		if (retval == null) {
			System.exit(0);
		}
		System.out.println("Validation error: " + retval);
		System.exit(1);
	}
}
