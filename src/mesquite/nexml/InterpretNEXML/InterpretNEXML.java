/**
 *
 */
package mesquite.nexml.InterpretNEXML;

import java.awt.*;
import java.io.FileInputStream;
import java.util.List;
import com.osbcp.cssparser.PropertyValue;

import mesquite.lib.*;
import mesquite.lib.duties.FileInterpreterI;
import mesquite.lib.ColorDistribution;
import mesquite.minimal.BasicFileCoordinator.BasicFileCoordinator;
import mesquite.nexml.InterpretNEXML.NexmlReaders.NexmlReader;
import mesquite.nexml.InterpretNEXML.NexmlWriters.NexmlWriter;

import mesquite.trees.BasicTreeDrawCoordinator.BasicTreeDrawCoordinator;
import mesquite.trees.NodeLocsStandard.NodeLocsStandard;
import org.nexml.model.Document;
import org.nexml.model.DocumentFactory;

public class InterpretNEXML extends FileInterpreterI {
    private List<PropertyValue> treeProperties;

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
	    } catch ( Exception e) {
	    	e.printStackTrace();
	    }
        setTreeDefaults(project);
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

    public void setTreeDefaults(MesquiteProject project) {
        CommandChecker cc = new CommandChecker();
        BasicFileCoordinator projectCoordinator = (BasicFileCoordinator) project.doCommand("getCoordinatorModule", "", cc);
        Commandable temp = (Commandable) projectCoordinator.doCommand("getEmployee", "#mesquite.trees.BasicTreeWindowCoord.BasicTreeWindowCoord", cc);
        temp = (Commandable) temp.doCommand("makeTreeWindow","#mesquite.trees.BasicTreeWindowMaker.BasicTreeWindowMaker",cc);
        temp = (Commandable) temp.doCommand("getTreeWindow", Integer.toString(1), cc);

        Commandable treeWindow = (Commandable) temp;
        Dimension dim = (Dimension) treeWindow.doCommand("getTreePaneSize","",cc);

        temp = (Commandable) temp.doCommand("getTreeDrawCoordinator", "#mesquite.trees.BasicTreeDrawCoordinator.BasicTreeDrawCoordinator", cc);
        BasicTreeDrawCoordinator treeDrawCoordinator = (BasicTreeDrawCoordinator) temp;

        temp = (Commandable) treeDrawCoordinator.doCommand("setTreeDrawer", "#mesquite.trees.WeightedSquareTree.WeightedSquareTree", cc);
        NodeLocsStandard nodeLocs = (NodeLocsStandard) temp.doCommand("setNodeLocs","#mesquite.trees.NodeLocsStandard.NodeLocsStandard",cc);

        // these will have to be changed to whatever the original treeWindow size is...
        int width = dim.width;
        int height = dim.height;

        // tell the treeDrawCoordinator to set canvas settings:
//        treeDrawCoordinator.doCommand("setBackground", "Green",cc);
        for (int i=0;i<treeProperties.size();i++) {
            PropertyValue pv = treeProperties.get(i);
            mesquite.lib.MesquiteMessage.notifyProgrammer("setting property "+pv.toString());
            if (pv.getProperty().equalsIgnoreCase("background")) {
                //set the color to the standard color
                mesquite.lib.MesquiteMessage.notifyProgrammer("background will be "+ convertToMesColorName(pv.getValue()));
                treeDrawCoordinator.doCommand("setBackground", convertToMesColorName(pv.getValue()),cc);
            } else if (pv.getProperty().equalsIgnoreCase("width")) {

                width = Integer.parseInt(pv.getValue().replaceAll("\\D",""));
            } else if (pv.getProperty().equalsIgnoreCase("height")) {
                height = Integer.parseInt(pv.getValue().replaceAll("\\D",""));
            } else if (pv.getProperty().equalsIgnoreCase("font-family")) {

            }
        }

        treeWindow.doCommand("setSize",width+" "+height,cc);
//        ((TreeWindowMaker) temp).doCommand("suppressEPCResponse","",cc);
//        temp = ((TreeWindowMaker) temp).doCommand("setTreeSource","#mesquite.trees.StoredTrees.StoredTrees",cc);
//        mesquite.lib.MesquiteMessage.notifyProgrammer("commanding " + temp.toString());
    }

    public static String convertToMesColorNumber ( String val ) {
        String mesColor = null;

        for (int i=0;i<ColorDistribution.standardColorNames.getSize();i++) {
            String thisColor = ColorDistribution.standardColorNames.getValue(i).toLowerCase();
            if(val.equals(thisColor)) {
                mesColor = String.valueOf(i);
            }
        }
        return mesColor;
    }

    public static String convertToMesColorName ( String val ) {
        String mesColor = null;

        for (int i=0;i<ColorDistribution.standardColorNames.getSize();i++) {
            String thisColor = ColorDistribution.standardColorNames.getValue(i).toLowerCase();
            if(val.equals(thisColor)) {
                mesColor = ColorDistribution.standardColorNames.getValue(i);
            }
        }
        return mesColor;
    }


}
