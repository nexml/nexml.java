package mesquite.nexml.InterpretNEXML;

import java.net.URI;

public class Constants {
    public static final boolean DEBUGGING = true;
    public static final String MESQUITE_NS_PREFIX = "msq";
	public static final String MESQUITE_NS_BASE = "http://mesquiteproject.org#";
	public static final String NRURIString = "http://mesquiteproject.org/namereference#";
	public static final String BeanURIString = "http://mesquiteproject.org/bean#";
	public static final String TSSURIString = "http://mesquiteproject.org/tss#";
	public static final String BaseURIString = "http://mesquiteproject.org#";

	public static final String BasePrefix = "msq";
	public static final String NRPrefix = "nr";
	public static final String BeanPrefix = "bean";
	public static final String TSSPrefix = "tss";

	public static final URI BaseURI = URI.create(BaseURIString);
	public static final URI BeanURI = URI.create(BeanURIString);
	public static final URI TSSURI = URI.create(TSSURIString);

	public static final String TaxaUID  = BasePrefix + ":taxaUID";
	public static final String TaxonUID = BasePrefix + ":taxonUID";

	public static final String PREDICATES_PROPERTIES = "predicateHandlerMapping.properties";
	public static final String NAMESPACE_PROPERTIES = "namespaceHandlerMapping.properties";

	public static final String NO_RULE = "NO_RULE";
    public static final String NO_VALUE = "NO_VALUE";
    public static final int DEFAULT_CANVAS_WIDTH = 800;
    public static final int DEFAULT_CANVAS_HEIGHT = 800;
    public static final int DEFAULT_FONT_SIZE = 10;
    public static final int DEFAULT_BORDER_WIDTH = 2;
    public static final String DEFAULT_FONT_FAMILY = "Helvetica";
}
