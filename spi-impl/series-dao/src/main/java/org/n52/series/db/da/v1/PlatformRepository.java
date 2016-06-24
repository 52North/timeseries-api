/*
 * Copyright (C) 2013-2016 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public License
 * version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package org.n52.series.db.da.v1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.io.request.RequestSimpleParameterSet;
import org.n52.io.response.v1.ext.PlatformOutput;
import org.n52.io.response.v1.ext.PlatformType;
import org.n52.sensorweb.spi.SearchResult;
import org.n52.series.db.da.DataAccessException;
import org.n52.series.db.da.beans.DescribableEntity;
import org.n52.series.db.da.beans.FeatureEntity;
import org.n52.series.db.da.beans.ext.PlatformEntity;
import org.n52.series.db.da.dao.v1.FeatureDao;
import org.n52.series.db.da.dao.v1.ext.PlatformDao;
import org.n52.web.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TODO: JavaDoc
 *
 * @author <a href="mailto:h.bredel@52north.org">Henning Bredel</a>
 */
public class PlatformRepository extends ExtendedSessionAwareRepository implements OutputAssembler<PlatformOutput> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformRepository.class);

    @Autowired
    private SeriesRepository seriesRepository;

    @Override
    public boolean exists(String id) throws DataAccessException {
        Session session = getSession();
        try {
            if (PlatformType.isStationaryId(id)) {
                FeatureDao featureDao = new FeatureDao(session);
                return featureDao.hasInstance(parseId(PlatformType.extractId(id)), FeatureEntity.class);
            } else {
                PlatformDao dao = new PlatformDao(session);
                return dao.hasInstance(parseId(PlatformType.extractId(id)), PlatformEntity.class);
            }
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<PlatformOutput> getAllCondensed(DbQuery parameters) throws DataAccessException {
        Session session = getSession();
        try {
            List<PlatformOutput> results = new ArrayList<>();
            for (PlatformEntity entity : getAllInstances(parameters, session)) {
                final PlatformOutput result = createCondensed(entity, parameters);
                results.add(result);
            }
            return results;
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<PlatformOutput> getAllExpanded(DbQuery parameters) throws DataAccessException {
        Session session = getSession();
        try {
            List<PlatformOutput> results = new ArrayList<>();
            for (PlatformEntity entity : getAllInstances(parameters, session)) {
                results.add(createExpanded(entity, parameters, session));
            }
            return results;
        } finally {
            returnSession(session);
        }
    }

    @Override
    public PlatformOutput getInstance(String id, DbQuery parameters) throws DataAccessException {
        Session session = getSession();
        try {
            if (PlatformType.isStationaryId(id)) {
                PlatformEntity platform = getStation(id, parameters, session);
                return createExpanded(platform, parameters, session);
            } else {
                PlatformEntity platform = getPlatform(id, parameters, session);
                return createExpanded(platform, parameters, session);
            }
        } finally {
            returnSession(session);
        }
    }

    @Override
    public Collection<SearchResult> searchFor(IoParameters parameters) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<SearchResult> convertToSearchResults(List<? extends DescribableEntity> found, String locale) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private List<PlatformEntity> getAllInstances(DbQuery parameters, Session session) throws DataAccessException {
        List<PlatformEntity> platforms = new ArrayList<>();
        if (parameters.shallIncludeStationaryTypes()) {
            platforms.addAll(getAllStationary(parameters, session));
        }
        if (parameters.shallIncludeMobilePlatformTypes()) {
            platforms.addAll(getAllMobile(parameters, session));
        }
        return platforms;
    }

    private List<PlatformEntity> getAllStationary(DbQuery parameters, Session session) throws DataAccessException {
        List<PlatformEntity> platforms = new ArrayList<>();
        if (parameters.shallIncludeInsituPlatformTypes()) {
            platforms.addAll(getAllStationaryInsitu(parameters, session));
        }
        if (parameters.shallIncludeRemotePlatformTypes()) {
            platforms.addAll(getAllStationaryRemote(parameters, session));
        }
        return platforms;
    }

    private List<PlatformEntity> getAllStationaryRemote(DbQuery parameters, Session session) throws DataAccessException {
        LOGGER.warn("getAllStationaryRemote() not implemented yet.");
        RequestSimpleParameterSet simpleParameterSet = parameters.getParameters().toSimpleParameterSet();
        if (simpleParameterSet.containsParameter(Parameters.PLATFORMS_INCLUDE_ALL)) {
            simpleParameterSet.removeParameter(Parameters.PLATFORMS_INCLUDE_ALL);
        }
        simpleParameterSet.addParameter(Parameters.PLATFORMS_INCLUDE_STATIONARY, IoParameters.getJsonNodeFrom(true));
        simpleParameterSet.addParameter(Parameters.PLATFORMS_INCLUDE_REMOTE, IoParameters.getJsonNodeFrom(true));
        return Collections.emptyList();
    }

    private PlatformEntity getStation(String id, DbQuery parameters, Session session) throws DataAccessException {
        String featureId = PlatformType.extractId(id);
        FeatureDao featureDao = new FeatureDao(session);
        FeatureEntity feature = featureDao.getInstance(Long.parseLong(featureId), parameters);
        if (feature == null) {
            throw new ResourceNotFoundException("Resource with id '" + id + "' could not be found.");
        }
        return convert(feature);
    }

    private PlatformEntity getPlatform(String id, DbQuery parameters, Session session) throws DataAccessException {
        PlatformDao dao = new PlatformDao(session);
        String platformId = PlatformType.extractId(id);
        PlatformEntity result = dao.getInstance(Long.parseLong(platformId), parameters);
        if (result == null) {
            throw new ResourceNotFoundException("Resource with id '" + id + "' could not be found.");
        }
        return result;
    }

    private List<PlatformEntity> getAllStationaryInsitu(DbQuery parameters, Session session) throws DataAccessException {
        FeatureDao featureDao = new FeatureDao(session);
        return convertAll(featureDao.getAllStations(parameters));
    }

    private List<PlatformEntity> getAllMobile(DbQuery parameters, Session session) throws DataAccessException {
        List<PlatformEntity> platforms = new ArrayList<>();
        if (parameters.shallIncludeInsituPlatformTypes()) {
            platforms.addAll(getAllMobileInsitu(parameters, session));
        }
        if (parameters.shallIncludeRemotePlatformTypes()) {
            platforms.addAll(getAllMobileRemote(parameters, session));
        }
        return platforms;
    }

    private List<PlatformEntity> getAllMobileInsitu(DbQuery parameters, Session session) throws DataAccessException {
        PlatformDao dao = new PlatformDao(session);
        RequestSimpleParameterSet simpleParameterSet = parameters.getParameters().toSimpleParameterSet();
        if (simpleParameterSet.containsParameter(Parameters.PLATFORMS_INCLUDE_ALL)) {
            simpleParameterSet.removeParameter(Parameters.PLATFORMS_INCLUDE_ALL);
        }
        simpleParameterSet.addParameter(Parameters.PLATFORMS_INCLUDE_MOBILE, IoParameters.getJsonNodeFrom(true));
        simpleParameterSet.addParameter(Parameters.PLATFORMS_INCLUDE_INSITU, IoParameters.getJsonNodeFrom(true));
        return dao.getAllInstances(DbQuery.createFrom(IoParameters.createFromQuery(simpleParameterSet)));
    }

    private List<PlatformEntity> getAllMobileRemote(DbQuery parameters, Session session) throws DataAccessException {
        LOGGER.warn("getAllMobileRemote() not implemented yet.");
        RequestSimpleParameterSet simpleParameterSet = parameters.getParameters().toSimpleParameterSet();
        if (simpleParameterSet.containsParameter(Parameters.PLATFORMS_INCLUDE_ALL)) {
            simpleParameterSet.removeParameter(Parameters.PLATFORMS_INCLUDE_ALL);
        }
        simpleParameterSet.addParameter(Parameters.PLATFORMS_INCLUDE_MOBILE, IoParameters.getJsonNodeFrom(true));
        simpleParameterSet.addParameter(Parameters.PLATFORMS_INCLUDE_REMOTE, IoParameters.getJsonNodeFrom(true));
        return Collections.emptyList();
    }

    private PlatformOutput createExpanded(PlatformEntity entity, DbQuery parameters, Session session) throws DataAccessException {
        PlatformOutput result = createCondensed(entity, parameters);
        // TODO better solution for adding a parameter
        RequestSimpleParameterSet simpleParameterSet = parameters.getParameters().toSimpleParameterSet();
        simpleParameterSet.addParameter(Parameters.PLATFORMS, IoParameters.getJsonNodeFrom(entity.getPkid()));
        result.setSeries(seriesRepository.getAllCondensed(DbQuery.createFrom(IoParameters.createFromQuery(simpleParameterSet))));
        return result;
    }

    private PlatformOutput createCondensed(PlatformEntity entity, DbQuery parameters) {
        PlatformOutput result = new PlatformOutput(entity.getPlatformType());
        result.setLabel(getLabelFrom(entity, parameters.getLocale()));
        result.setId(Long.toString(entity.getPkid()));
        result.setDomainId(entity.getDomainId());
        result.setHrefBase(urHelper.getPlatformsHrefBaseUrl(parameters.getHrefBase()));
        return result;
    }

    private List<PlatformEntity> convertAll(List<FeatureEntity> entities) {
        List<PlatformEntity> converted = new ArrayList<>();
        for (FeatureEntity entity : entities) {
            converted.add(convert(entity));
        }
        return converted;
    }

    private PlatformEntity convert(FeatureEntity entity) {
        PlatformEntity result = new PlatformEntity();
        result.setDomainId(entity.getDomainId());
        result.setPkid(entity.getPkid());
        result.setName(entity.getName());
        result.setTranslations(entity.getTranslations());
        result.setDescription(entity.getDescription());
        return result;
    }

}
