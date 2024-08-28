package com.distocraft.dc5000.etl.csexport;

import java.io.FileReader;
import java.rmi.Naming;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.distocraft.dc5000.common.RmiUrlFactory;
import com.distocraft.dc5000.etl.parser.Main;
import com.distocraft.dc5000.etl.parser.MeasurementFile;
import com.distocraft.dc5000.etl.parser.Parser;
import com.distocraft.dc5000.etl.parser.SourceFile;
import com.distocraft.dc5000.etl.parser.utils.FlsUtils;
import com.ericsson.eniq.common.ENIQEntityResolver;
import com.ericsson.eniq.enminterworking.EnmInterCommonUtils;
import com.ericsson.eniq.enminterworking.IEnmInterworkingRMI;

/**
 * 
 * <br>
 * <table border="1" width="100%" cellpadding="3" cellspacing="0">
 * <tr bgcolor="#CCCCFF" class="TableHeasingColor">
 * <td colspan="4"><font size="+2"><b>Parameter Summary</b></font></td>
 * </tr>
 * <tr>
 * <td><b>Name</b></td>
 * <td><b>Key</b></td>
 * <td><b>Description</b></td>
 * <td><b>Default</b></td>
 * </tr>
 * <tr>
 * <td>VendorID from</td>
 * <td>readVendorIDFrom</td>
 * <td>Defines where the vendorID is retrieved from <b>data</b> (moid-tag) or from <b>filename</b>. RegExp is used to
 * further define the actual vendorID. Vendor id is added to the outputdata as objectClass. See. VendorID Mask and
 * objectClass</td>
 * <td>data</td>
 * </tr>
 * <tr>
 * <td>VendorID Mask</td>
 * <td>vendorIDMask</td>
 * <td>Defines the RegExp mask that is used to extract the vendorID from either data or filename. See. VendorID from</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr>
 * <td>&nbsp;</td>
 * <td>singlerow</td>
 * <td>A list of MOs for which a single row is generated even if there is a seq (sequence) tag. The items in the seq tag
 * will be concatinated (separated by semicolon space) into one value for that key.</td>
 * <td>empty string</td>
 * </tr>
 * <tr>
 * <td>&nbsp;</td>
 * <td>extractHeaderRows</td>
 * <td></td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>&nbsp;</td>
 * <td>headerVendorID</td>
 * <td></td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>&nbsp;</td>
 * <td>headerVendorID</td>
 * <td></td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>&nbsp;</td>
 * <td>headerTag</td>
 * <td></td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>&nbsp;</td>
 * <td>numberOfHeaderRows</td>
 * <td></td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * </tr>
 * </table>
 * <br>
 * <br>
 * <table border="1" width="100%" cellpadding="3" cellspacing="0">
 * <tr bgcolor="#CCCCFF" class="TableHeasingColor">
 * <td colspan="2"><font size="+2"><b>Added DataColumns</b></font></td>
 * </tr>
 * <tr>
 * <td><b>Column name</b></td>
 * <td><b>Description</b></td>
 * </tr>
 * <tr>
 * <td>filename</td>
 * <td>contains the filename of the inputdatafile.</td>
 * </tr>
 * <td>objectClass</td>
 * <td>contains the same data as in vendorID (see. readVendorIDFrom)</td>
 * </tr>
 * <tr>
 * <td>FDN</td>
 * <td>contains the FDN -tag.</td>
 * </tr>
 * <tr>
 * <td>DC_SUSPECTFLAG</td>
 * <td>EMPTY</td>
 * </tr>
 * <tr>
 * <td>DIRNAME</td>
 * <td>Conatins full path to the nputdatafile.</td>
 * </tr>
 * <tr>
 * <td>JVM_TIMEZONE</td>
 * <td>contains the JVM timezone (example. +0200)</td>
 * </tr>
 * <tr>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * </tr>
 * </table>
 * <br>
 * <br>
 * 
 * @author savinen <br>
 * <br>
 * 
 */
public class CSExportParser extends DefaultHandler implements Parser {

	private Logger log;

	private SourceFile sourceFile;

	private FileReader dtdReader;

	private String objectMask;

	private String readVendorIDFrom;

	private String charValue;

	private String fdn;
	
	private String mimVersion="";

	private List fdnList = null;
	
	private String objectClass;

	private String oldObjectClass;
	
	private String assocNode = "";

	private String key;
	
	private String attributeToPrefix;

	private final Set<String> seqKeys = new HashSet<String>(); // For a given MO, these are the attributes that have a
																// sequence in them

	private String seqKey; // The seq attibute (key) being processed currently.

	private int seqCount = 0;

	private ArrayList seqContainer = null;

	private Map structContainer = null;

	private int highestSeqCount = 1;

	private MeasurementFile measFile = null;
	
	private MeasurementFile assocFile = null;
	
	private MeasurementFile seqMeasFile;

	private Map measData;
	
	private String singleRow = "";

	private Map headerData = null;

	private Map cloneData = null;

	private boolean extractHeader = false;

	private String headerVendorID = null;

	private String headerTag = "";

	private int numberOfHeaderRows = 2;

	private String cloneTypeID = "";

	private int headerCount = 0;

	// ***************** Worker stuff ****************************

	private String techPack;

	private String setType;

	private String setName;

	private int status = 0;

	private Main mainParserObject = null;

	private final String suspectFlag = "";

	private String workerName = "";
	
	private String associateRNC = "";
	
	private boolean associateRNCExist = false;
	
	private String managedElementType  = "";
	
	private boolean managedElementTypeExist = false;
	
	private boolean ne_typeExist = false;

	
	//******************60K
	
	private HashMap<String,String> nodefdn ;
	
	private String ne_type="";

	private boolean isFlsEnabled = false;

	private String serverRole = null;
	
	private IEnmInterworkingRMI multiEs;
	
	private Map<String, String> ossIdToHostNameMap;
	
	// attributes for 5G RadioNode support
	private boolean isSeqDataSeparateMoFlag = false;
	private boolean isSeqDataSeparateAttrFlag = false;
	private boolean isAttributeToPrefix = false;
	private Set<String> attributesToPrefix;
	private static final String SEQUENCE_INDEX = "SEQUENCE_INDEX";
	private static final String ATTRIBUTES_TO_PREFIX = "attributesToPrefix";
	private long parseStartTime;
	private long fileSize = 0L;
	private long totalParseTime = 0L;
	private int fileCount = 0;
	
	@Override
	public void init(final Main main, final String techPack, final String setType, final String setName,
			final String workerName) {
		this.mainParserObject = main;
		this.techPack = techPack;
		this.setType = setType;
		this.setName = setName;
		this.status = 1;
		this.workerName = workerName;

		String logWorkerName = "";
		if (workerName.length() > 0) {
			logWorkerName = "." + workerName;
		}

		log = Logger.getLogger("etl." + techPack + "." + setType + "." + setName + ".parser.XML" + logWorkerName);
		
	}

	@Override
	public int status() {
		return status;
	}

	@Override
	public void run() {

		try {

			this.status = 2;
			SourceFile sf = null;
			parseStartTime = System.currentTimeMillis();

			while ((sf = mainParserObject.nextSourceFile()) != null) {
				fileCount++;
				fileSize += sf.fileSize();

				try {
					mainParserObject.preParse(sf);
					parse(sf, techPack, setType, setName);
					mainParserObject.postParse(sf);
				} catch (final Exception e) {
					mainParserObject.errorParse(e, sf);
				} finally {
					mainParserObject.finallyParse(sf);
				}
			}
			totalParseTime = System.currentTimeMillis() - parseStartTime;
			if (totalParseTime != 0) {
				log.info("Parsing Performance :: " + fileCount
						+ " files parsed in " + totalParseTime / 1000
						+ " sec, filesize is " + fileSize / 1000
						+ " Kb and throughput : " + (fileSize / totalParseTime)
						+ " bytes/ms.");
			}
		} catch (final Exception e) {
			// Exception catched at top level. No good.
			log.log(Level.WARNING, "Worker parser failed to exception", e);
		} finally {
			this.status = 3;
		}
	}

	// ***************** Worker stuff ****************************

	@Override
	public void parse(final SourceFile sf, final String techPack, final String setType, final String setName)
			throws Exception {

		measData = new HashMap();

		this.sourceFile = sf;

		log.finest("Reading configuration...");

		final XMLReader xmlReader = new org.apache.xerces.parsers.SAXParser();
		    String id    = "http://apache.org/xml/properties/input-buffer-size";
		    Object value = new Integer(16384);
		    try {
		    	xmlReader.setProperty(id, value);
		    } 
		    catch (SAXException e) {
		         log.finest("could not set parser property");
		    }
		xmlReader.setContentHandler(this);
		xmlReader.setErrorHandler(this);

		objectMask = sf.getProperty("vendorIDMask", ".+,(.+)=.+");
		readVendorIDFrom = sf.getProperty("readVendorIDFrom", "data");

		singleRow = sf.getProperty("singlerow", "");

		extractHeader = "true".equalsIgnoreCase(sf.getProperty("extractHeaderRows", "true"));
		headerVendorID = sf.getProperty("headerVendorID");
		headerTag = sf.getProperty("headerTag", "mo");
		numberOfHeaderRows = Integer.parseInt(sf.getProperty("numberOfHeaderRows", "2"));
		cloneTypeID = sf.getProperty("cloneTypeID", "");
		String attributesToPrefixString = sf.getProperty(ATTRIBUTES_TO_PREFIX, "");

		attributesToPrefix = new HashSet<>();
		if(!("").equals(attributesToPrefixString))
		{
			String[] attributesToPrefixTokens = attributesToPrefixString.split(",");
			for (int i = 0; i < attributesToPrefixTokens.length; i++) {
				attributesToPrefix.add(attributesToPrefixTokens[i]);
			}
		}
		
		log.fine("Staring to parse...");

		xmlReader.setEntityResolver(new ENIQEntityResolver(log.getName()));

		xmlReader.parse(new InputSource(sf.getFileInputStream()));

		log.fine("Parse finished.");
	}

	/**
	 * Event handlers
	 */
	@Override
	public void startDocument() {
		log.finest("Start document");
		headerCount = 0;
		fdnList = new ArrayList();
		headerData = new HashMap();
		cloneData = new HashMap();
		nodefdn = new HashMap<String,String>();
		oldObjectClass = "";
	}

	@Override
	public void endDocument() throws SAXException {
		try {
			String dir = sourceFile.getProperty("inDir");
			String pattern = ".+/(.+)/.+/.+/.+";
			String enmDir = transformFileVariables(dir, pattern);
			String enmAlias = Main.resolveDirVariable(enmDir);
			isFlsEnabled = FlsUtils.isFlsEnabled(enmAlias);
			if (isFlsEnabled) {
				multiEs = (IEnmInterworkingRMI) Naming
						.lookup(RmiUrlFactory.getInstance().getMultiESRmiUrl(EnmInterCommonUtils.getEngineIP()));
				String enmHostName = FlsUtils.getEnmShortHostName(enmAlias);
				try {
					for (Map.Entry<String, String> entry : nodefdn.entrySet()) {
						if ( ( entry.getValue() != null && !entry.getValue().equals("") ) && ( entry.getKey() != null && !entry.getKey().equals("") ) && enmHostName != null){
							log.info("In FLS mode. Adding FDN to the Automatic Node Assignment Blocking Queue "
									+ entry.getValue() + " " + entry.getKey() + " " + enmHostName);
							multiEs.addingToBlockingQueue(entry.getValue(), entry.getKey(), enmHostName);
						}

					}
				} catch (Exception abqE) {
					log.log(Level.WARNING, "Exception occured while adding FDN to Blocking Queue!", abqE);
				}

			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Exception occured while checking for FLS", e);
		}
		log.finest("End document");
		measData.clear();
		cloneData.clear();
		sourceFile = null;

		seqKeys.clear();
		seqContainer = null;
	}


	@Override
	public void startElement(final String uri, final String name, final String qName, final Attributes atts)
			throws SAXException {

		charValue = new String();

		// read object class from data
		for (int i = 0; i < atts.getLength(); i++) {
			if (atts.getLocalName(i).equalsIgnoreCase("fdn")) {
				fdn = atts.getValue(i);
				log.log(Level.FINEST, "fdn: " + fdn);
			}
		}	
		
		if (qName.equals(headerTag) && extractHeader && (numberOfHeaderRows > headerCount)) {

			fdnList.add(fdn);
			headerCount++;
			// first header row

		} else if (qName.equals("mo")) {

				this.mimVersion =  atts.getValue("mimVersion");

			// where to read objectClass (moid)
			if (readVendorIDFrom.equalsIgnoreCase("file")) {

				// read vendor id from file
				objectClass = parseFileName(sourceFile.getName(), objectMask);

			} else if (readVendorIDFrom.equalsIgnoreCase("data")) {

				// read vendor id from file
				objectClass = parseFileName(fdn, objectMask);

			} else {

				// error
				log.warning("readVendorIDFrom property" + readVendorIDFrom + " is not defined");
				throw new SAXException("readVendorIDFrom property" + readVendorIDFrom + " is not defined");
			}

		} else if (qName.equals("attr")) {
			for (int i = 0; i < atts.getLength(); i++) {
				if (atts.getLocalName(i).equalsIgnoreCase("name")) {	
					key = atts.getValue(i);
					if(key.equalsIgnoreCase("associatedRnc")){
						associateRNCExist = true;
						log.log(Level.FINEST,"associatedRnc has been identified   "+associateRNCExist);
					}if(key.equalsIgnoreCase("managedElementType")){
						managedElementTypeExist = true;
						ne_typeExist = true;
						log.log(Level.FINEST,"managedElementType has been identified  "+managedElementTypeExist);
					}if(key.equalsIgnoreCase("reservedBy")){
						structContainer = null;
						log.log(Level.FINEST,"reservedBy has been identified ");
					} if (attributesToPrefix.contains(key)) {
						isSeqDataSeparateAttrFlag = true;
						isSeqDataSeparateMoFlag = true;
						isAttributeToPrefix = true ;
						 attributeToPrefix = key;
					}
					
				}
			}
		}else if (qName.equals("seq")) {
			seqContainer = null ;
			seqKeys.add(key);
			if(key.equalsIgnoreCase("available")){
				seqKeys.remove(key);
				seqKeys.add("reason");
				seqKey = "reason";
				key = "reason";
			}
			else if (key.matches("(?i)associated(.*)Nodes"))
					{
				log.finest("Found associated node : " + key);
				assocNode = key;
					}
			else{
			seqKey = key;	
			}
			for (int i = 0; i < atts.getLength(); i++) {
				if (atts.getLocalName(i).equalsIgnoreCase("count")) {
					seqCount = Integer.parseInt(atts.getValue(i));
				}
			}
			if (seqCount > highestSeqCount) {
				if (!key.matches("(?i)associated(.*)Nodes"))
				{
					log.finest("Not an associated node type sequence : " + key);
					highestSeqCount = seqCount;
				}
			}
			if(key.equalsIgnoreCase("available")){
				seqContainer = null;
			}else{
			seqContainer = new ArrayList();
			}

		} else if (qName.equals("item")) {

		} else if (qName.equals("struct")) {
			if (null != seqContainer) {
				structContainer = new HashMap();
			}else {
				structContainer = new HashMap();
			}
		}

	}

	@Override
	public void endElement(final String uri, final String name, final String qName) throws SAXException {

		if (qName.equals(headerTag) && extractHeader && (numberOfHeaderRows == headerCount)) {
			extractHeader = false;

			try {

				measFile = Main.createMeasurementFile(sourceFile, headerVendorID, techPack, setType, setName,
						this.workerName, this.log);
				measFile.addData("Filename", sourceFile.getName());
				measFile.addData("DC_SUSPECTFLAG", suspectFlag);
				measFile.addData("DIRNAME", sourceFile.getDir());
				final SimpleDateFormat sdf = new SimpleDateFormat("Z");
				measFile.addData("JVM_TIMEZONE", sdf.format(new Date()));
				measFile.addData("objectClass", headerVendorID);

				// add all fdn(s) from header rows.
				for (int i = 0; i < fdnList.size(); i++) {

					final String value = (String) fdnList.get(i);
					String key = "fdn";
					if (i > 0) {
						key += i;
					}
					measFile.addData(key, value);
				}
				// clone the map
				final Iterator iter = measData.keySet().iterator();
				while (iter.hasNext()) {

					final String key = (String) iter.next();
					headerData.put(key, measData.get(key));
				}

				measFile.addData(measData);
				measFile.saveData();
				measData.clear();

			} catch (final Exception e) {
				log.log(Level.FINE, "Error in writing measurement file", e);
			}

		} else if (qName.equals("attr")) {
			if (null == seqContainer) {
				// We're not in a sequence (and we may or may not be under a struct)
				log.log(Level.FINEST, "Key: " + key + " Value: " + charValue);
				if(key.equalsIgnoreCase("managedElementType")){
					managedElementType = charValue;
					log.log(Level.FINEST,"managedElementType value  "+managedElementType);
				}if(key.equalsIgnoreCase("associatedRnc")){
					associateRNC = charValue;
					log.log(Level.FINEST,"associateRNC value  "+associateRNC);
				}
				measData.put(key, charValue);
				
			}else if (seqContainer.size()== 0) {
				//for mobility status

				if (!isSeqDataSeparateAttrFlag) {
					log.log(Level.FINEST, "Key: " + key + " Value: " + charValue + " of attribute " + seqKey
							+ " being put in measData HashMap for sequence index: " + seqContainer.size() + ".");
					measData.put(key, charValue);
					measData.put(seqKey, seqContainer);
				}
				else {
					log.log(Level.FINEST, "Key: " + key + " Value: " + charValue + " of attribute " + seqKey
							+ " being put in struct HashMap for sequence index: " + seqContainer.size() + ".");
					structContainer.put(key, charValue);
					structContainer.put(key + "_" + seqKey, charValue); // This is to provide distinction when there is more
					// than one seq
				}

			}
			else if (seqContainer.size() == seqCount) {
				// We have finished a sequence
				if (!isSeqDataSeparateAttrFlag) {
					measData.put(seqKey, seqContainer);
				} else {
					if(isAttributeToPrefix){
						try {
							log.info("Creating new measurement file for seqMeasFile");
							seqMeasFile = Main.createMeasurementFile(sourceFile, objectClass+"_"+attributeToPrefix, techPack, setType, setName,
									this.workerName, this.log);
						} catch (Exception e) {
							log.log(Level.WARNING,"Not able to create measurement file for "+objectClass+"_"+attributeToPrefix,e);
						}
					}
					else{
						try {
							log.info("Creating new measurement file for seqMeasFile");
							seqMeasFile = Main.createMeasurementFile(sourceFile, objectClass+"_"+key, techPack, setType, setName,
									this.workerName, this.log);
						} catch (Exception e) {
							log.log(Level.WARNING,"Not able to create measurement file for "+objectClass+"_"+attributeToPrefix,e);
						}
					}
					
					try {
						if (seqMeasFile != null) {
							populateSeqData();
						}
					} catch (Exception e) {
						log.log(Level.WARNING, "Error while populating the sequence data :",e);
					}
					try {
						if (seqMeasFile != null) {
							seqMeasFile.close();
						}
					} catch (Exception e) {
						log.log(Level.WARNING, "Error while closing the seqMeasFile :",e);
					} finally {						
						isSeqDataSeparateAttrFlag = false;
					}
				}
				seqContainer = null;
				isAttributeToPrefix = false;
			} else {
				// We're in a sequence
				log.log(Level.FINEST, "Key: " + key + " Value: " + charValue + " of attribute " + seqKey
						+ " being put in struct HashMap for sequence index: " + seqContainer.size() + ".");
				structContainer.put(key, charValue);
				structContainer.put(key + "_" + seqKey, charValue); // This is to provide distinction when there is more
				// than one seq
			}
		
		} else if (qName.equals("item")) {
			if (null == structContainer || structContainer.isEmpty()) {
				// This was an item with no struct under it
				seqContainer.add(charValue);
			} else {
				seqContainer.add(structContainer);
				structContainer = null;
			}

		} else if (qName.equals("seq")) {
			if (key.matches("(?i)associated(.*)Nodes"))
			{
				try{
				assocFile = Main.createMeasurementFile(sourceFile, assocNode, techPack, setType, setName,
						this.workerName, this.log);
			
					log.finest("Writing to  : " + assocNode);
					log.finest("seqCount = "+seqCount);
					for (int i = 0; i < seqCount; i++) {
						// Processing one row (one index of sequence). MO's without sequence are also handled (iterate
						// once).
						log.finest("seqCount(i) = "+i);
						assocFile.addData(headerData);
						assocFile.addData(cloneData);
						assocFile.addData("Filename", sourceFile.getName());
						assocFile.addData("DC_SUSPECTFLAG", suspectFlag);
						assocFile.addData("DIRNAME", sourceFile.getDir());
						assocFile.addData("objectClass", assocNode);
						assocFile.addData("fdn", fdn);
						assocFile.addData("mimVersion", mimVersion);
						assocFile.addData("JVM_TIMEZONE", getJvmTimeZone());
						log.finest("size of seqContainer = "+seqContainer.size());
						if (i < seqContainer.size()) {
								// Check if we have come to the end of items for this seqKey
							if (seqContainer.get(i) instanceof String) {
									// The item could be a string value,
								assocFile.addData("assocNode", (String) seqContainer.get(i));
									log.finest("Sequence key " + seqKey + " has String item");
							} 
							} else if (seqContainer.size() == 0) {
								// For HR26524
								log.finest("Sequence Container size is : " + seqContainer);
								assocFile.addData(seqKey, "");
							}
						log.finest("Saving Data ");
						assocFile.saveData();
				}
					seqContainer = null;
					assocNode = "";
					assocFile.close();
					seqKeys.clear();
					highestSeqCount = 1;
				}
			 catch (final Exception e) {
				log.log(Level.FINE, "Error in writing measurement file", e);
				seqContainer = null;
				assocNode = "";
				seqKeys.clear();
				highestSeqCount = 1;
			}
			}

		} else if (qName.equals("struct")) {

		} else if (qName.equals("mo")) {
			if(associateRNCExist){
				log.log(Level.FINEST,"Adding tempAssociatedRNC to the measFile  "+associateRNC);
				measData.put("tempAssociatedRNC",associateRNC);
			}
			if(managedElementTypeExist){
				log.log(Level.FINEST,"Adding tempManagedElementType to the measFile  "+managedElementType);
				measData.put("tempManagedElementType",managedElementType);
				ne_type=managedElementType;
			}
			if (objectClass != null) {
				try {
					// if this is a clonable type,
					if ((this.cloneTypeID.length() > 0) && this.objectClass.equals(cloneTypeID)) {
						// clear clone data map
						cloneData.clear();

						// inser this tags data to cloneData map, this map is added to every other datatarow from this
						// point forward.
						final Iterator iter = measData.keySet().iterator();
						while (iter.hasNext()) {
							final String key = (String) iter.next();
							cloneData.put(key, measData.get(key));
						}
					}

					if (!objectClass.equals(oldObjectClass)) {
						log.log(Level.FINEST, "New objectClass found: " + objectClass);
						oldObjectClass = objectClass;
						// close old meas file
						if (measFile != null) {
							measFile.close();
						}
						measFile = Main.createMeasurementFile(sourceFile, objectClass, techPack, setType, setName,
								this.workerName, this.log);

					} else {
						log.log(Level.FINEST, "Old objectClass, no need to create new measFile " + oldObjectClass);
					}

					log.fine("Writing " + objectClass);
					log.finest("Seq Count Highest = " + highestSeqCount);
					for (int i = 0; i < highestSeqCount; i++) {
						// Processing one row (one index of sequence). MO's without sequence are also handled (iterate
						// once).
						measFile.addData(headerData);
						measFile.addData(cloneData);
						measFile.addData("Filename", sourceFile.getName());
						measFile.addData("DC_SUSPECTFLAG", suspectFlag);
						measFile.addData("DIRNAME", sourceFile.getDir());
						measFile.addData("objectClass", objectClass);
						measFile.addData("fdn", fdn);
						if(ne_typeExist){
							log.finest("&&&&&&in inside mo "+fdn+"   "+ne_type);
							nodefdn.put(fdn, ne_type);
							ne_typeExist = false;
						}
						measFile.addData("mimVersion", mimVersion);
						measFile.addData("JVM_TIMEZONE", getJvmTimeZone());
						measFile.addData(measData);
						
						if (!isSeqDataSeparateMoFlag) {
							final Iterator<String> seqsKeysIter = seqKeys.iterator();
							while (seqsKeysIter.hasNext()) { // For each attribute with seq tag (for each seqKey),
								// ..get the item for current sequence index.
								seqKey = seqsKeysIter.next();

								//append sequence data
								log.finest(" key is :"+seqKey );
								log.finest(" value is :"+ measData.get(seqKey) );// HV80431 
								if (!seqKey.equalsIgnoreCase("activePlmnList")){
									seqContainer = (ArrayList) measData.get(seqKey);
								}
								if(seqContainer != null){
									if (i < seqContainer.size()) {
										// Check if we have come to the end of items for this seqKey
										if (seqContainer.get(i) instanceof String) {
											// The item could be a string value,
											measFile.addData(seqKey, (String) seqContainer.get(i));
											log.finest("Sequence key " + seqKey + " has String item");
										} else if (seqContainer.get(i) instanceof HashMap) {
											// ..or could be a mapping of keys and values (from a struct tag).
											measFile.addData((HashMap) seqContainer.get(i));
											measFile.addData(seqKey, ""); // Remove the ArrayList from measFile.
											log.finest("Sequence key " + seqKey + " has HashMap (struct) item");
										}
									} else if (seqContainer.size() == 0) {
										// For HR26524
										log.finest("Sequence Container size is : " + seqContainer);
										measFile.addData(seqKey, "");
									}
								}

							}

							if (seqKeys.size() > 0) {// We only need a value for SQUENCE_INDEX when there's a seqKey.
								measFile.addData(SEQUENCE_INDEX, Integer.toString(i + 1));
								log.finest("Writing SEQUENCE_INDEX " + Integer.toString(i + 1));
							} else {
								measFile.addData(SEQUENCE_INDEX, Integer.toString(1));
								log.finest("There is no sequence for this objectClass. So adding the default sequence index of 1");
							}
						}
						log.finest("Saving Data ");
						measFile.saveData();
						if (isSeqDataSeparateMoFlag) {
							break; //save only one row as sequence data will be stored separately
						}
						if (singleRow.contains(objectClass)) {
							break; // Only 1 row to be written for this MO.
						}
					}

					measData.clear();
					seqContainer = null;
					seqKeys.clear();
					highestSeqCount = 1;

				} catch (final Exception e) {
					log.log(Level.INFO, "Error in writing measurement file", e);
					seqContainer = null;
					seqKeys.clear();
					highestSeqCount = 1;
				} finally {
					isSeqDataSeparateMoFlag = false;
				}

			}

		}else if (qName.equals("model")) {
			associateRNCExist = false;
			managedElementTypeExist = false;
			
		}
		
	}
	
	private void populateSeqData() {
		Integer seqIndex = 0;
		HashMap<String,String> itemValueMap = null;
		try {
			for (Object itemValue : seqContainer) {
				seqIndex ++;
				if (itemValue != null){
					seqMeasFile.addData(headerData);
					seqMeasFile.addData(cloneData);
					seqMeasFile.addData("Filename", sourceFile.getName());
					seqMeasFile.addData("DC_SUSPECTFLAG", suspectFlag);
					seqMeasFile.addData("DIRNAME", sourceFile.getDir());
					if (isAttributeToPrefix && itemValue instanceof HashMap){
						seqMeasFile.addData("objectClass", objectClass+"_"+attributeToPrefix);
						itemValueMap = (HashMap) itemValue;
						for ( String itemValueEntryKey : itemValueMap.keySet() )
						{
							if (!itemValueEntryKey.contains(attributeToPrefix))
							{
								seqMeasFile.addData(itemValueMap);
							}
						}
					}
					else{
						seqMeasFile.addData("objectClass", objectClass+"_"+key);
						seqMeasFile.addData(key, itemValue.toString());
					}
					
					seqMeasFile.addData("fdn", fdn);
					seqMeasFile.addData("mimVersion", mimVersion);
					seqMeasFile.addData("JVM_TIMEZONE", getJvmTimeZone());
					seqMeasFile.addData(SEQUENCE_INDEX, seqIndex.toString());
					seqMeasFile.saveData();
				}
			}	
		} catch (Exception e) {
			log.log(Level.WARNING," Error while populating Sequence data :",e);
		}
		finally
		{
			itemValueMap = null;
		}
		
	}
	
	private String getJvmTimeZone() {
		final SimpleDateFormat sdf = new SimpleDateFormat("Z");
		return  sdf.format(new Date());
	}
	

	@Override
	public void characters(final char ch[], final int start, final int length) {
		final StringBuffer charBuffer = new StringBuffer(length);
		for (int i = start; i < (start + length); i++) {
			// If no control char
			if ((ch[i] != '\\') && (ch[i] != '\n') && (ch[i] != '\r') && (ch[i] != '\t')) {
				charBuffer.append(ch[i]);
			}
		}
		charValue += charBuffer;
	}

	/**
	 * Extracts a substring from given string based on given regExp
	 * 
	 */
	public String parseFileName(final String str, final String regExp) {

		final Pattern pattern = Pattern.compile(regExp);
		final Matcher matcher = pattern.matcher(str);

		if (matcher.matches()) {
			final String result = matcher.group(1);
			log.finest(" regExp (" + regExp + ") found from " + str + "  :" + result);
			return result;
		} else {
			log.warning("String " + str + " doesn't match defined regExp " + regExp);
		}

		return "";
	}
	
	/**
	 * Lookups variables from filename
	 * 
	 * @param name
	 *            of the input file
	 * @param pattern
	 *            the pattern to lookup from the filename
	 * @return result returns the group(1) of result, or null
	 */
	private String transformFileVariables(String filename, String pattern) {
		String result = null;
		try {
			Pattern p = Pattern.compile(pattern);
			Matcher m = p.matcher(filename);
			if (m.matches()) {
				result = m.group(1);
			}
		} catch (PatternSyntaxException e) {
			log.log(Level.SEVERE, "Error performing transformFileVariables for ASCIIParser.", e);
		}
		return result;
	}
}