/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.saviasaludeps.savia.web.maestro.seleccion.servicio;

import com.saviasaludeps.savia.web.maestro.seleccion.bean.SelTecnologiasBean;

/**
 *
 * @author AlexanderDiaz
 */
public interface TecnologiaServicioIface {
    
    /**
     * Método para cargar inicial del Bean
     * @param bean
     */
    void cargaInicial(SelTecnologiasBean bean);
}
