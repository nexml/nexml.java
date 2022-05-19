/**
 * 
 */
package mesquite.nexml.InterpretNEXML.NexmlReaders;

import java.io.IOException;
import java.lang.String;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import mesquite.lib.MesquiteMessage;
import mesquite.lib.EmployerEmployee;
import mesquite.lib.FileElement;
import mesquite.lib.Listable;
import mesquite.lib.MesquiteFile;
import mesquite.lib.MesquiteProject;
import mesquite.lib.MesquiteTree;
import mesquite.lib.Taxa;
import mesquite.lib.TreeVector;
import mesquite.lib.duties.TreesManager;

import org.nexml.model.Annotatable;
import org.nexml.model.Edge;
import org.nexml.model.Network;
import org.nexml.model.Node;
import org.nexml.model.OTU;
import org.nexml.model.OTUs;
import org.nexml.model.TreeBlock;

/**
 * @author rvosa
 *
 */
public class NexmlTreeBlockReader extends NexmlBlockReader {

    /**
     * @param employerEmployee
     */
    public NexmlTreeBlockReader(EmployerEmployee employerEmployee) {
        super(employerEmployee);
    }

    /* (non-Javadoc)
     * @see mesquite.nexml.InterpretNEXML.NexmlBlockReader#readBlock(mesquite.lib.MesquiteProject, mesquite.lib.MesquiteFile, org.nexml.model.Annotatable, org.nexml.model.OTUs)
     */
    @Override
    protected FileElement readBlock(MesquiteProject mesProject,MesquiteFile mesFile, Annotatable xmlAnnotatable, OTUs xmlOTUs) {
        Taxa mesTaxa = findEquivalentTaxa(xmlOTUs, mesProject);
        TreeBlock xmlTreeBlock = (TreeBlock)xmlAnnotatable;
        TreesManager mesTreeTask = (TreesManager)getEmployerEmployee().findElementManager(TreeVector.class);
        TreeVector mesTreeVector = mesTreeTask.makeNewTreeBlock(mesTaxa, xmlTreeBlock.getLabel(), mesFile);
        for ( Network<?> xmlNetwork : xmlTreeBlock ) {

            // for now we only import trees, not networks
            if ( xmlNetwork instanceof org.nexml.model.Tree ) {

                // instantiate and store named mesquite tree in vector
                MesquiteTree mesTree = new MesquiteTree(mesTaxa);
                mesTreeVector.addElement(mesTree, false);
                mesTree.setName(xmlNetwork.getLabel());

                // populate the tree from the root to the tips, recursively
                Node xmlRoot = ((org.nexml.model.Tree<?>)xmlNetwork).getRoot();
                if ( xmlRoot == null ) {
                    Root: for ( Node xmlNode : xmlNetwork.getNodes() ) {
                       Set<Node> xmlInNodes = xmlNetwork.getInNodes(xmlNode);
                        if ( xmlInNodes.size() == 0 ) {
                            xmlRoot = xmlNode;
                            break Root;
                        }
                    }
                }
                Map<Integer,Integer> otuLookup = new HashMap<Integer,Integer>();
                readTree(xmlNetwork, xmlRoot, mesTree, mesTree.getRoot(), otuLookup);

                // now resolve the taxon references
                for ( int mesNode : otuLookup.keySet() ) {
                    int mesOTU = otuLookup.get(mesNode);
                	if ( mesTree.nodeIsTerminal(mesNode) ) {
                		mesTree.setTaxonNumber(mesNode, mesOTU, false);
                	}
                    else {
                        String mesTaxonName = mesTaxa.getTaxonName(mesOTU);
                        mesTree.setNodeLabel(mesTaxonName, mesNode);
                    }
                }
            }
        }
        return mesTreeVector;
    }
    
    /**
     * 
     * @param xmlTree
     * @param xmlNode
     * @param mesNode
     * @param mesTree
     */
    private void readTree(Network<?> xmlTree, Node xmlNode, MesquiteTree mesTree, int mesNode, Map otuLookup) {

        // store the ID of the OTU, if there is one. We do this
        // in a separate hash from whence we later retrieve only
        // those OTUs that are actually terminal in the topology.
        OTU xmlOTU = xmlNode.getOTU();
        if ( xmlOTU != null ) {
            Taxa mesTaxa = mesTree.getTaxa();
            int mesTaxon = mesTaxa.findByUniqueID(xmlOTU.getId());
            otuLookup.put(mesNode,mesTaxon);
        }

        // copy the node label and the RDFa annotations
        mesTree.setNodeLabel(xmlNode.getLabel(), mesNode);
        readAnnotations(mesTree,xmlNode,mesNode,mesTree);

        // iterate over children
        for ( Node xmlChild : xmlTree.getOutNodes(xmlNode) ) {

            // make child, copy annotations
            int mesChild = mesTree.sproutDaughter(mesNode, false);

            // copy branch length, if any, and branch annotations
            Edge edge = xmlTree.getEdge(xmlNode,xmlChild);
            if ( edge.getLength() != null ) {
                mesTree.setBranchLength(mesChild, edge.getLength().doubleValue(), false);
            }
            readAnnotations(mesTree, edge, mesChild, mesTree);

            // traverse deeper
            readTree(xmlTree,xmlChild,mesTree,mesChild,otuLookup);
        }
    }   
    
    
    /* (non-Javadoc)
     * @see mesquite.nexml.InterpretNEXML.NexmlBlockReader#getThingInMesquiteBlock(mesquite.lib.FileElement, int)
     */
    @Override
    protected Listable getThingInMesquiteBlock(FileElement mesBlock, int index) {
        TreeVector mesTrees = (TreeVector)mesBlock;
        return mesTrees.getTree(index);
    }

}
