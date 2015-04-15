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
package it.geosolutions.geobatch.opensdi.csvingest.processor;

import it.geosolutions.geobatch.opensdi.csvingest.utils.CSVPropertyType;
import it.geosolutions.opensdi.model.Fertilizer;
import it.geosolutions.opensdi.persistence.dao.GenericNRLDAO;

import java.util.List;

/**
 * @author DamianoG
 *
 */
public class CSVFertilizerProcessor extends GenericCSVProcessor<Fertilizer, Long> {

    @Override
    public GenericNRLDAO<Fertilizer, Long> getDao() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getHeaders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<CSVPropertyType> getTypes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Integer> getPkProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Fertilizer merge(Fertilizer old, Object[] properties) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void save(Fertilizer entity) {
        
        
    }

    @Override
    public void persist(Fertilizer entity) {
        // TODO Auto-generated method stub
        
    }

}
