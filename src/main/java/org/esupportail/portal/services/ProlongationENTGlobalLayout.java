package org.esupportail.portal.services;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.LogFactory;

import org.jasig.portal.ChannelRegistryStoreFactory;
import org.jasig.portal.channel.IChannelDefinition;
import org.jasig.portal.layout.dlm.RDBMDistributedLayoutStore;
import org.jasig.portal.layout.UserLayoutStoreFactory;
import org.jasig.portal.portlet.om.IPortletPreference;
import org.jasig.portal.spring.locator.PortletDefinitionRegistryLocator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class ProlongationENTGlobalLayout {

    String wanted_fragment_user = "all-lo";
	
    Map<String, List<String>> layout;
    Map<Long,Map<String,String>> allChannels;
    org.apache.commons.logging.Log log = LogFactory.getLog(ProlongationENTGlobalLayout.class);
    
    public ProlongationENTGlobalLayout() {
	this.layout = getWantedLayout();
	this.allChannels = keepOnlyUsedChannelsAndAddTab(getAllChannels(), layout);
    }
	
    
    Map<Long,Map<String,String>> keepOnlyUsedChannelsAndAddTab(Map<Long,HashMap<String,String>> channels, Map<String, List<String>> layout) {
	Map<String, Long> fname2tab = new HashMap<String,Long>();
	long i = 1;
	for (List<String> fnames : layout.values()) {
	    for (String fname : fnames) {
		fname2tab.put(fname, i);
	    }
	    i++;
	}

	Map<Long,Map<String,String>> rslt = new HashMap<Long,Map<String,String>>();
	for (Map.Entry<Long, HashMap<String,String>> e : channels.entrySet()) {
	    HashMap<String,String> channel = e.getValue();
	    Long tab = fname2tab.get(channel.get("fname"));
	    if (tab != null) {
		channel.put("uportalActiveTab", ""+tab);
		rslt.put(e.getKey(), channel);
	    }
	}
	return rslt;
    }

    Map<String, List<String>> getWantedLayout() {
	Map<String, List<String>> layout = new LinkedHashMap<String, List<String>>();
	try {
	    Map<String, Document> documents = ((RDBMDistributedLayoutStore) UserLayoutStoreFactory.getUserLayoutStoreImpl()).getFragmentLayoutCopies();
	    for (String fragmentName : documents.keySet()) {
		if (!fragmentName.equals(wanted_fragment_user)) continue;

		String expression = "//folder[@type='regular' and @hidden='false' and @name !='Column']";
		for (Element folder : listElement((NodeList) XPathFactory.newInstance().newXPath().evaluate(expression, documents.get(fragmentName), XPathConstants.NODESET))) {
			
		    List<String> channels = new ArrayList<String>();
		    for (Element channel : listElement(folder.getElementsByTagName("channel"))) {
			channels.add(channel.getAttribute("fname"));
		    }
		    layout.put(folder.getAttribute("name"), channels);
		}
	    }
	} catch (Exception e) {
	    log.error(e);
	}
	return layout;
    }

    Map<Long,HashMap<String,String>> getAllChannels() {

	//List<IPortletDefinition> allPortlets = portletDefinitionRegistry.getAllPortletDefinitions();
	List<IChannelDefinition> allPortlets = ChannelRegistryStoreFactory.getChannelRegistryStoreImpl().getChannelDefinitions();

	Map<Long,HashMap<String,String>> rslt = new HashMap<Long,HashMap<String,String>>();
	for (IChannelDefinition pdef : allPortlets) {
	    HashMap<String,String> channel = 
		arrayS("fname", pdef.getFName(),
		       "text", pdef.getName(),
		       "title", pdef.getTitle(),
		       "description", pdef.getDescription());

	    for (IPortletPreference pref : PortletDefinitionRegistryLocator.getPortletDefinitionRegistry().getPortletDefinition(pdef.getId()).getPortletPreferences().getPortletPreferences()) {
		if (pref.getName().equals("url")) {
		    String url = pref.getValues()[0];
		    String service = removePrefixOrNull(url, "/ExternalURLStats?fname=" + pdef.getFName() + "&service=");
		    if (service != null) {
			url = urldecode(service);
			channel.put("useExternalURLStats", "true");
		    }
		    channel.put("url", url);
		}
	    }
	    rslt.put(new Long(pdef.getId()), channel);
	}
	return rslt;
    }

    static List<Element> listElement(final NodeList list) {
	return new AbstractList<Element>() {
	    public int size() {
		return list.getLength();
	    }

	    public Element get(int index) {
		Element item = (Element) list.item(index);
		if (item == null)
		    throw new IndexOutOfBoundsException();
		return item;
	    }
	};
    }

    static String removePrefixOrNull(String s, String prefix) {
	return s.startsWith(prefix) ? s.substring(prefix.length()) : null;
    }

    static String urldecode(String s) {
	try {
	    return java.net.URLDecoder.decode(s, "UTF-8");
	}
	catch (java.io.UnsupportedEncodingException uee) {
	    return s;
	}
    }

    static HashMap<String, String> arrayS(String key1, String val1) {
	HashMap<String, String> r = new HashMap<String, String>();
	r.put(key1, val1);
	return r;
    }
    static HashMap<String, String> arrayS(String key1, String val1, String key2, String val2) {
	HashMap<String, String> r = arrayS(key1, val1);
	r.put(key2, val2);
	return r;
    }
    static HashMap<String, String> arrayS(String key1, String val1, String key2, String val2, String key3, String val3) {
	HashMap<String, String> r = arrayS(key1, val1, key2, val2);
	r.put(key3, val3);
	return r;
    }
    static HashMap<String, String> arrayS(String key1, String val1, String key2, String val2, String key3, String val3, String key4, String val4) {
	HashMap<String, String> r = arrayS(key1, val1, key2, val2, key3, val3);
	r.put(key4, val4);
	return r;
    }        
}
