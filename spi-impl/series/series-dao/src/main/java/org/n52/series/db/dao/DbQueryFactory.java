/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.series.db.dao;

import org.n52.io.request.IoParameters;

/**
 *
 * @author jansch
 */
public interface DbQueryFactory {
    
    DbQuery createFrom(IoParameters parameters);
}
