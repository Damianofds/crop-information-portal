/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://geobatch.geo-solutions.it/
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
package it.geosolutions.geobatch.opensdi.csvingest;

import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.geobatch.annotations.Action;
import it.geosolutions.geobatch.annotations.CheckConfiguration;
import it.geosolutions.geobatch.flow.event.action.ActionException;
import it.geosolutions.geobatch.flow.event.action.BaseAction;
import it.geosolutions.geobatch.opensdi.csvingest.processor.CSVProcessException;
import it.geosolutions.geobatch.opensdi.csvingest.processor.CSVProcessor;
import it.geosolutions.opensdi.persistence.dao.AgrometDAO;
import it.geosolutions.opensdi.persistence.dao.CropDataDAO;
import it.geosolutions.opensdi.persistence.dao.CropDescriptorDAO;
import it.geosolutions.opensdi.persistence.dao.CropStatusDAO;
import it.geosolutions.opensdi.service.UnitOfMeasureService;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import au.com.bytecode.opencsv.CSVReader;


@Action(configurationClass=CSVIngestConfiguration.class)
public class CSVIngestAction extends BaseAction<EventObject> implements InitializingBean {

    protected final static Logger LOGGER = LoggerFactory.getLogger(CSVIngestAction.class);

    @Autowired
    private CropDescriptorDAO cropDescriptorDao;

    @Autowired
    private CropDataDAO cropDataDao;

    @Autowired
    private AgrometDAO agrometDao;

    @Autowired
    private CropStatusDAO cropStatusDao;
    
    @Autowired
    private UnitOfMeasureService unitOfMeasureService;

    @Autowired
    private List<CSVProcessor> processors;
	
	
    
    private static final long AVG_ROW_BYTE_SIZE = 50;

    public CSVIngestAction(final CSVIngestConfiguration configuration) throws IOException {
        super(configuration);
    }

    @Override
    @CheckConfiguration
    public boolean checkConfiguration() {
        return true;
    }

    private void checkInit() {
        if(cropDataDao == null)
            throw new IllegalStateException("cropDataDao is null");
        if(cropDescriptorDao == null)
            throw new IllegalStateException("cropDescriptorDao is null");
        if(agrometDao == null)
            throw new IllegalStateException("agrometDao is null");
        if(cropStatusDao == null)
            throw new IllegalStateException("cropStatusDao is null");
        if(unitOfMeasureService == null)
            throw new IllegalStateException("unitOfMeasureService is null");
    }

    /**
     *
     */
    public Queue<EventObject> execute(Queue<EventObject> events) throws ActionException {

        listenerForwarder.setTask("Check config");

        // @autowired fields are injected *after* the checkConfiguration() is called
        checkInit();

        listenerForwarder.started();

        CSVIngestConfiguration configuration = getConfiguration();
        if (configuration == null) {
            throw new IllegalStateException("ActionConfig is null.");
        }

        while(! events.isEmpty()) {
            EventObject event = events.poll();
            if(event instanceof FileSystemEvent) {
                FileSystemEvent fse = (FileSystemEvent) event;
                File file = fse.getSource();
                processCSVFile(file);
//                    throw new ActionException(this, "Could not process " + event);
            } else {
                throw new ActionException(this, "EventObject not handled " + event);
            }
        }

        return new LinkedList<EventObject>();
    }


    @Transactional(value = "opensdiTransactionManager")
    private void processCSVFile(File file) throws ActionException {
        LOGGER.info("Processing input file " + file);

        String[] headers = null;
        CSVReader reader = null;
        try {
            reader = new CSVReader(new FileReader(file), ',');
            headers = reader.readNext();
        } catch (IOException e) {
        	try{
        		reader.close();
			}catch(Exception ee){
				LOGGER.error("Unable to close the CSV reader",ee);
			}
            throw new ActionException(this, "Error in reading CSV file", e);
        }

        List<String> headersList = sanitizeHeaders(headers);

        CSVProcessor processor = null;
        for (CSVProcessor p : processors) {
            if(p.canProcess(headersList)) {
                processor = p;
                break;
            }
        }

        if(processor == null) {
            LOGGER.warn("No processors found for file " + file.getName() + "; headers: " + headersList);
            try{
        		reader.close();
			}catch(Exception e){
				LOGGER.error("Unable to close the CSV reader",e);
			}
            throw new ActionException(this, "No processors found for file " + file.getName() +  "; headers: " + headersList);
        }
        listenerForwarder.completed();
        listenerForwarder.setTask("CSV Ingestion");
        listenerForwarder.setProgress(0);
        long rowEstimation = file.length()/AVG_ROW_BYTE_SIZE;
        LOGGER.info("Processing CSV " + file.getName() + " with " + processor.getClass().getSimpleName());
        try {
            processor.process(reader,listenerForwarder,rowEstimation);
            String successMsg =   
                  "\n***************************************************" 
                + "\n********** SUCCESS: CSV ingestion resume **********" 
                + "\n***************************************************"
                + "\n* Records inserted: "+ processor.getInsertCount()
                + "\n* Records updated: "+ processor.getUpdateCount()
                + "\n* Records removed: "+ processor.getRemoveCount()
                + "\n* Falied records: "+ processor.getFailCount()
                + "\n***************************************************\n";
            LOGGER.info(successMsg);
            listenerForwarder.progressing(100f, successMsg);
            if(processor.getFailCount()>0){
            	throw new ActionException(this, "Some record failed when processing CSV file" + file.getName());
            }
        } catch (CSVProcessException ex) {
            throw new ActionException(this, "Error processing " + file.getName()+"."+ ex.getLocalizedMessage(), ex);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if(processors== null || processors.isEmpty()){
            throw new IllegalStateException("No CSV Processors have been found... at least one processor is needed in order to use this action...");
        }
        LOGGER.info("List of CSV processor found in the application Context:");
        for(CSVProcessor processor : processors){
            LOGGER.info("--> Processor: '" + processor.getClass().toString() + "' DAO: '" + processor.getDAOClassName() + "'");
        }
    }

    public void setCropDescriptorDao(CropDescriptorDAO cropDescriptorDAO) {
        this.cropDescriptorDao = cropDescriptorDAO;
    }

    public void setCropDataDao(CropDataDAO cropDataDAO) {
        this.cropDataDao = cropDataDAO;
    }

    public void setAgrometDao(AgrometDAO agrometDAO) {
        this.agrometDao = agrometDAO;
    }
    
    public CropStatusDAO getCropStatusDao() {
		return cropStatusDao;
	}

	public void setCropStatusDao(CropStatusDAO cropStatusDao) {
		this.cropStatusDao = cropStatusDao;
	}
	public void setUnitOfMeasureService(UnitOfMeasureService unitOfMeasureService){
		this.unitOfMeasureService = unitOfMeasureService;
	}

    /**
     * Check if null header fields are present
     * 
     * @param headers the CSV header array extracted from a CSV file
     * @return the CSV headers as a List
     * @throws ActionException id a null header is found
     */
    private List<String> sanitizeHeaders(String[] headers) throws ActionException {

        List<String> ret = new ArrayList<String>();

        boolean emptyFound = false;

        for (String h : headers) {
            if(h == null || h.isEmpty()) {
                emptyFound = true;
            } else {
                if(emptyFound) {
                    throw new ActionException(this, "Header value found after blank header");
                }
                ret.add(h);
            }
        }

        return ret;
    }

}

