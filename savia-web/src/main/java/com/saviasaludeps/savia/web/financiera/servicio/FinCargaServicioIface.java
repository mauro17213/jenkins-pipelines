/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.saviasaludeps.savia.web.financiera.servicio;

import com.saviasaludeps.savia.web.financiera.bean.FinCargaBean;

/**
 *
 * @author jeperez
 */
public interface FinCargaServicioIface {
    
    void Accion(FinCargaBean bean);
    
    /**
     * Carga inicial de la opción
     * @param bean 
     */
    void cargasInicial(FinCargaBean bean);
    
}
