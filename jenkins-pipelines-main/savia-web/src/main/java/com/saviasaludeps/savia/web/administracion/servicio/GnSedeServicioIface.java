/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.saviasaludeps.savia.web.administracion.servicio;

import com.saviasaludeps.savia.web.administracion.bean.GnSedeBean;

/**
 *
 * @author acuartas
 */
public interface GnSedeServicioIface {
    
    void Accion(GnSedeBean bean);
    
    /**
     * Carga inicial de la opción
     * @param bean 
     */
    void cargasInicial(GnSedeBean bean);
}
