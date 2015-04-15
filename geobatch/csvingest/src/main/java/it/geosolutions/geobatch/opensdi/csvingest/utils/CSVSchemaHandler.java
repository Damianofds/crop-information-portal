/*
 *  Copyright (C) 2007-2012 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.geobatch.opensdi.csvingest.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections.ListUtils;
import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

/**
 * @author DamianoG
 *
 * This class holds mapping information between input CSV file and the data entity used in the crop information portal.
 * It loads the information from a properties file called as the entity (lowercase).
 * 
 * The Properties file is loaded in memory in the class constructor execution, it must take into account in order to
 * avoid unwanted caching side-effects of the values.
 *
 */
public class CSVSchemaHandler {

    private static Logger LOGGER = Logger.getLogger(CSVSchemaHandler.class);
    
    private final static String TYPE_LIST = "types_list";
    
    private final static String UNIQUE_LIST = "unique_list";
    
    private final static String LIST_SEPARATOR = ";";
    
    private final List<CSVPropertyType> typesList;
    
    private final List<Integer> uniqueList;
    
    public CSVSchemaHandler(String className){
        typesList = new ArrayList<CSVPropertyType>();
        uniqueList = new ArrayList<Integer>();
        Map<String,String> configMap = loadEntityProperties(className);
        if(!configMap.keySet().contains(TYPE_LIST) || !configMap.keySet().contains(UNIQUE_LIST)){
            throw new IllegalStateException("cannot find TYPE_LIST and UNIQUE_LIST in the properties file...");
        }
        String typeListString = configMap.get(TYPE_LIST);
        String uniqueListString = configMap.get(UNIQUE_LIST);
        
        if(typeListString == null || typeListString.isEmpty()){
            throw new IllegalStateException("TYPE_LIST cannot be null or empty...");
        }
        String[] typeListArray = typeListString.split(LIST_SEPARATOR);
        for(String type : typeListArray){
            try{
                typesList.add(CSVPropertyType.valueOf(type));
            }
            catch(Exception e){
                throw new IllegalStateException("TYPE_LIST contains a not valid value: '" + type + "'");
            }
        }
        if(!StringUtils.trimAllWhitespace(uniqueListString).isEmpty()){
            String[] uniqueListArray = uniqueListString.split(LIST_SEPARATOR);
            for(String index : uniqueListArray){
                try{
                    uniqueList.add(Integer.parseInt(index));
                }
                catch(Exception e){
                    throw new IllegalStateException("UNIQUE_LIST contains a not valid Integer value: '" + index + "'");
                }
            }
        }
    }
    
    public List<CSVPropertyType> getTypeList(){
        return ListUtils.unmodifiableList(typesList);
    }
    
    public List<Integer> getPrimaryKeysIndexes(){
        return ListUtils.unmodifiableList(uniqueList);
    }
    
    /**
     * Search and load for a properties file called as the entity. 
     *  
     * @param entityNames
     */
    public static Map<String, String> loadEntityProperties(String entityName){
        if(entityName == null || entityName.isEmpty()){
            throw new IllegalArgumentException("entityName is null or empty... this should never happen...");
        }
        URL fileURL = searchpropertiesFile(entityName);
        File propFile;
        try {
            propFile = new File(fileURL.toURI());
        } catch (URISyntaxException e1) {
            throw new IllegalArgumentException("Unable to find property file for this CSV Processor...");
        }
        
        InputStream inStream = null;
        Map<String, String> propertiesMap = null;
        try {
            inStream = new FileInputStream(propFile);
            Properties p = new Properties();
            p.load(inStream);
            propertiesMap = new HashMap<String, String>((Map) p);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            LOGGER.error(e.getMessage(), e);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            LOGGER.error(e.getMessage(), e);
        }
        finally{
            if(inStream != null){
                try {
                    inStream.close();
                } catch (IOException e) {
                    inStream = null;
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
        return  propertiesMap;
    }
    
    /**
     * This method search for the properties file where the informations about the keyList and uniqueList are stored.
     * The approach is the following:
     * <ul>
     *    <li>First, search in the classpath root, so <b>WEB-INF/classes</b> if this method run in a webapp. This file is the override file and it could be useful in case we want to change some mapping at runtime, without redeploying nor restarting the server</li>
     *    <li>If the file is not found in the classpath root, search it in the <b>current package</b>. A file must be found since here are the default properties files for this class</li>
     * </ul>
     * 
     * TODO: Instead of search in the classpath root the method should be able to search the file in the Geobatch config directory. Unfortunately the interfaces of CSVGenericProcess doesn't allow to pass the path of that directory. (inject it with spring?) 
     * 
     */
    public static URL searchpropertiesFile(String entityName){
        URL url = CSVSchemaHandler.class.getResource("/"+entityName.toLowerCase()+".properties");
        if(url == null){
            url = CSVSchemaHandler.class.getResource(entityName.toLowerCase()+".properties");
        }
        if(url == null){
            throw new IllegalStateException("the properties file cannot be found in the package... this should never happen...");
        }
        return url;
    }
}
