/**
 * 
 */
package org.nexml.model;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * @author rvosa
 *
 */
public class TestFileParse {
	@Test
	public void parseCharacters() {
		String nexmlRoot = System.getenv("NEXML_ROOT");
		if ( nexmlRoot == null ) {
			nexmlRoot = "/Users/rvosa/Dropbox/documents/projects/current/nexml/src/nexml/trunk/nexml";
		}
		File file = new File(nexmlRoot+"/examples/characters.xml");
		Document doc = null;
		try {
			doc = DocumentFactory.parse(file);
		} catch (SAXException e) {
			Assert.assertTrue(e.getMessage(), false);
			e.printStackTrace();
		} catch (IOException e) {
			Assert.assertTrue(e.getMessage(), false);
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			Assert.assertTrue(e.getMessage(), false);
			e.printStackTrace();
		}
		System.out.println(doc.getXmlString());
	}
	
	@Test
	public void parsePhenoscapeMatrix() throws SAXException, IOException, ParserConfigurationException {
		final File file = new File("test_files/Buckup_1998.xml");
		final Document doc = DocumentFactory.parse(file);
		for (Matrix<?> matrix : doc.getMatrices()) {
			for (Character character : matrix.getCharacters()) {
				for (OTU otu : matrix.getOTUs()) {
					System.out.println(matrix.getRowObject(otu).getCell(character).getValue());
				}
			}
		}
	}

}
