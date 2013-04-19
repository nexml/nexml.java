/**
 *
 */
package mesquite.nexml.InterpretNEXML;

import java.awt.*;
import java.io.FileInputStream;
import java.util.List;
import com.osbcp.cssparser.PropertyValue;

import mesquite.lib.*;
import mesquite.lib.duties.DrawTree;
import mesquite.lib.duties.FileInterpreterI;
import mesquite.minimal.BasicFileCoordinator.BasicFileCoordinator;
import mesquite.nexml.InterpretNEXML.NexmlReaders.NexmlReader;
import mesquite.nexml.InterpretNEXML.NexmlWriters.NexmlWriter;

import mesquite.trees.BasicDrawTaxonNames.BasicDrawTaxonNames;
import mesquite.trees.BasicTreeDrawCoordinator.BasicTreeDrawCoordinator;
import mesquite.trees.BasicTreeWindowCoord.BasicTreeWindowCoord;
import mesquite.trees.BasicTreeWindowMaker.BasicTreeWindowMaker;
import mesquite.trees.NodeLocsStandard.NodeLocsStandard;
import org.nexml.model.Document;
import org.nexml.model.DocumentFactory;

public class InterpretNEXML extends FileInterpreterI {
    private List<PropertyValue> treeProperties;
    private List<PropertyValue> canvasProperties;

    /*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
 		return true;  //make this depend on taxa reader being found?)
	}

/*.................................................................................................................*/
	public String preferredDataFileExtension() {
 		return "xml";
	}
/*.................................................................................................................*/
	public boolean canExportEver() {
		 return true;  //
	}
/*.................................................................................................................*/
	public boolean canExportProject(MesquiteProject project) {
		 return true;
	}

/*.................................................................................................................*/
	@SuppressWarnings("rawtypes")
	public boolean canExportData(Class dataClass) {
		return true;
	}
/*.................................................................................................................*/
	public boolean canImport() {
		 return true;
	}

/*.................................................................................................................*/
	public void readFile(MesquiteProject project, MesquiteFile file, String arguments) {
		FileInputStream fs = null;
		Document xmlDocument = null;
		try {
			fs = new FileInputStream(file.getPath());
			xmlDocument = DocumentFactory.parse(fs);
		} catch ( Exception e ) {
			mesquite.lib.MesquiteMessage.notifyProgrammer("Exception: " + e.toString());
			e.printStackTrace();
		}
		NexmlReader nr = new NexmlReader(this);
        try {
			nr.fillProjectFromNexml(xmlDocument,project);
            treeProperties = nr.getTreeProperties();
            canvasProperties = nr.getCanvasProperties();
            sendMesquiteCommands(project);
        } catch ( Exception e) {
	    	e.printStackTrace();
	    }
    }

/* ============================  exporting ============================*/
	/*.................................................................................................................*/
	// XXX make compact/verbose switch
	public boolean getExportOptions(boolean dataSelected, boolean taxaSelected){
		return true;
	}


	/*.................................................................................................................*/
	public boolean exportFile(MesquiteFile file, String arguments) {
		MesquiteProject mesProject = getProject();
		NexmlWriter nw = new NexmlWriter(this);
		Document xmlProject = nw.createDocumentFromProject(mesProject);
		StringBuffer outputBuffer = new StringBuffer();
		String xmlString = null;
		try {
			xmlString = xmlProject.getXmlString();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		outputBuffer.append(xmlString);
		saveExportedFileWithExtension(outputBuffer, arguments, "xml");
		return true;
	}

	/*.................................................................................................................*/
    public String getName() {
		return "NeXML (taxa, matrices, trees and annotations)";
   	 }
	/*.................................................................................................................*/

 	/** returns an explanation of what the module does.*/
 	public String getExplanation() {
 		return "Imports and exports NeXML2009 files (see http://www.nexml.org)" ;
   	 }
	/*.................................................................................................................*/

    public void sendMesquiteCommands(MesquiteProject project) {
        CommandChecker cc = new CommandChecker();
        BasicFileCoordinator projectCoordinator = (BasicFileCoordinator) project.doCommand("getCoordinatorModule", "", cc);
        BasicTreeWindowCoord treeWindowCoord = (BasicTreeWindowCoord) projectCoordinator.doCommand("getEmployee", "#mesquite.trees.BasicTreeWindowCoord.BasicTreeWindowCoord", cc);
        BasicTreeWindowMaker treeWindowMaker = (BasicTreeWindowMaker) treeWindowCoord.doCommand("makeTreeWindow","#mesquite.trees.BasicTreeWindowMaker.BasicTreeWindowMaker",cc);

        // treeWindow is a Commandable because the BasicTreeWindow class isn't public
        Commandable treeWindow = (Commandable) treeWindowMaker.doCommand("getTreeWindow", Integer.toString(1), cc);
        Dimension dim = (Dimension) treeWindow.doCommand("getTreePaneSize","",cc);
        BasicTreeDrawCoordinator treeDrawCoordinator = (BasicTreeDrawCoordinator) treeWindow.doCommand("getTreeDrawCoordinator", "#mesquite.trees.BasicTreeDrawCoordinator.BasicTreeDrawCoordinator", cc);
        BasicDrawTaxonNames taxonNames = (BasicDrawTaxonNames) treeDrawCoordinator.doCommand("getEmployee","#mesquite.trees.BasicDrawTaxonNames.BasicDrawTaxonNames");
        DrawTree treeDrawer = (DrawTree) treeDrawCoordinator.doCommand("setTreeDrawer", "#mesquite.trees.WeightedSquareTree.WeightedSquareTree", cc);
        NodeLocsStandard nodeLocs = (NodeLocsStandard) treeDrawer.doCommand("setNodeLocs","#mesquite.trees.NodeLocsStandard.NodeLocsStandard",cc);

        int width = dim.width;
        int height = dim.height;
        String backgroundColor = "White";
        String fontSize = "10";
        String fontFamily = "Helvetica";

        treeWindowMaker.doCommand("suppressEPCResponse","",cc);
        treeWindowMaker.doCommand("setTreeSource","#mesquite.trees.StoredTrees.StoredTrees",cc);

        // tell the treeDrawCoordinator to set canvas settings:
//        treeDrawCoordinator.doCommand("setBackground", "Green",cc);
        for (int i=0;i<canvasProperties.size();i++) {
            PropertyValue pv = canvasProperties.get(i);
            mesquite.lib.MesquiteMessage.notifyProgrammer("setting canvasProperty "+pv.toString());
            if (pv.getProperty().equalsIgnoreCase("background-color")) {
                //set the color to the standard color
                backgroundColor = pv.getValue();
            } else if (pv.getProperty().equalsIgnoreCase("width")) {
                width = Integer.parseInt(pv.getValue());
            } else if (pv.getProperty().equalsIgnoreCase("height")) {
                height = Integer.parseInt(pv.getValue());
            } else if (pv.getProperty().equalsIgnoreCase("font-family")) {
                fontFamily = pv.getValue();
            } else if (pv.getProperty().equalsIgnoreCase("font-size")) {
                fontSize = pv.getValue();
            }
        }
        treeWindow.doCommand("setSize", width+" "+height, cc);
        treeDrawCoordinator.doCommand("setBackground", backgroundColor, cc);
        taxonNames.doCommand("setFontSize",fontSize,cc);
        taxonNames.doCommand("setFont",fontFamily,cc);


        String borderWidth = "3";
        String borderColor = "0";
        String layout = "rectangular";
        String tipOrientation = "RIGHT";
        boolean scaled = false;

        /*
          border-width: 1px;
          border-color: black;
          border-style: solid;
          layout: rectangular | triangular | radial | polar
          tip-orientation: left | right | top | bottom
          scaled: true | false;
        */
        for (int i=0;i<treeProperties.size();i++) {
            PropertyValue pv = treeProperties.get(i);
            mesquite.lib.MesquiteMessage.notifyProgrammer("setting treeProperty "+pv.toString());
            if (pv.getProperty().equalsIgnoreCase("border-width")) {
                borderWidth = pv.getValue();
            } else if (pv.getProperty().equalsIgnoreCase("border-color")) {
                borderColor = pv.getValue();
            } else if (pv.getProperty().equalsIgnoreCase("tip-orientation")) {
                tipOrientation = pv.getValue();
            } else if (pv.getProperty().equalsIgnoreCase("font-family")) {
                fontFamily = pv.getValue();
            } else if (pv.getProperty().equalsIgnoreCase("font-size")) {
                fontSize = pv.getValue();
            }
        }
        treeDrawer.doCommand("setStemWidth", borderWidth, cc);
        treeDrawer.doCommand("setEdgeWidth", borderWidth, cc);
        treeDrawer.doCommand("setBranchColor", borderColor, cc);

        if (tipOrientation.equalsIgnoreCase("up")) {
            treeDrawer.doCommand("orientUP","",cc);
        } else if (tipOrientation.equalsIgnoreCase("left")) {
            treeDrawer.doCommand("orientLEFT","",cc);
        } else if (tipOrientation.equalsIgnoreCase("right")) {
            treeDrawer.doCommand("orientRIGHT","",cc);
        } else if (tipOrientation.equalsIgnoreCase("down")) {
            treeDrawer.doCommand("orientDOWN","",cc);
        }

        treeWindowMaker.doCommand("desuppressEPCResponse","",cc);
    }
}
