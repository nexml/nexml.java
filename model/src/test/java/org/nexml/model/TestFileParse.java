/**
 * 
 */
package org.nexml.model;

import java.io.File;
import java.io.IOException;
import java.net.URL;

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
		Document doc = null;
		try {
			URL nexmlURL = new URL("https://raw.githubusercontent.com/nexml/nexml/master" + "/examples/characters.xml");
			doc = DocumentFactory.parse(nexmlURL.openStream());
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
