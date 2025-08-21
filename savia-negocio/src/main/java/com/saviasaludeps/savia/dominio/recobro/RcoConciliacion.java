package com.saviasaludeps.savia.dominio.recobro;

import com.saviasaludeps.savia.dominio.contratacion.CntContrato;
import com.saviasaludeps.savia.dominio.contratacion.CntPrestadorSede;
import com.saviasaludeps.savia.dominio.generico.Auditoria;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Stiven Giraldo
 */
public class RcoConciliacion extends Auditoria {
    
    //ESTADOS
    public static final int PENDIENTE_SOPORTE = 0;
    public static final int NOTIFICIACION_PRESTADOR = 1;
    public static final int EN_CONCILIACION = 2;
    public static final int FINALIZADO = 3;
    
    //ESTADO AUDITORIA
    public static final String ESTADO_AUDITORIA_CODIGO_AUDITADO = "2";
    
    //ESTADO ESTADO FACTURA
    public static final String ESTADO_FACTURA_CODIGO_POTENCIAL_RECOBRO = "3";
    
    //Encabaezado Excel Envio Correo
    public static final String[] ENCABEZADOS = {"NUMERO_RADICADO", "NUMERO_FACTURA", "NIT_IPS_RECOBRO", "NOMBRE_IPS_RECOBRO", "NIT_IPS_FACTURA", "NOMBRE_IPS_FACTURA", "CODIGO_SERVICIO", "DESCRIPCION_SERVICIO", "NOMBRE_AFILIADO", "TIPO_DOCUMENTO_AFILIADO", "DOCUMENTO", "VALOR_TOTAL_RECOBRO", "FECHA_PRESTACION", "DIAGNOSTICO", "OBSERVACION_RECOBRO", "FECHA REPORTE"};
    
    private Integer id;
    private String nombre;
    private String descripcion;
    private Integer estado;
    private String numeroContrato;
    private CntContrato cntContratoId;
    private Date fechaInicio;
    private Date fechaFin;
    private String correoEnvio;
    private Integer cantidadItems;
    private Integer cantidadItemsRecobrados;
    private BigDecimal valorTotalConciliado;
    private List<RcoConciliacionGestion> rcoConciliacionGestionList;
    private CntPrestadorSede cntPresadoresSedesId;
    private List<RcoConciliacionAdjunto> rcoConciliacionAdjuntosList;

    public RcoConciliacion() {
    }

    public RcoConciliacion(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getEstado() {
        return estado;
    }

    public void setEstado(Integer estado) {
        this.estado = estado;
    }

    public String getNumeroContrato() {
        return numeroContrato;
    }

    public void setNumeroContrato(String numeroContrato) {
        this.numeroContrato = numeroContrato;
    }

    public CntContrato getCntContratoId() {
        return cntContratoId;
    }

    public void setCntContratoId(CntContrato cntContratoId) {
        this.cntContratoId = cntContratoId;
    }

    public Date getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(Date fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Date getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(Date fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getCorreoEnvio() {
        return correoEnvio;
    }

    public void setCorreoEnvio(String correoEnvio) {
        this.correoEnvio = correoEnvio;
    }

    public Integer getCantidadItems() {
        return cantidadItems;
    }

    public void setCantidadItems(Integer cantidadItems) {
        this.cantidadItems = cantidadItems;
    }

    public Integer getCantidadItemsRecobrados() {
        return cantidadItemsRecobrados;
    }

    public void setCantidadItemsRecobrados(Integer cantidadItemsRecobrados) {
        this.cantidadItemsRecobrados = cantidadItemsRecobrados;
    }

    public BigDecimal getValorTotalConciliado() {
        return valorTotalConciliado;
    }

    public void setValorTotalConciliado(BigDecimal valorTotalConciliado) {
        this.valorTotalConciliado = valorTotalConciliado;
    }

    public List<RcoConciliacionGestion> getRcoConciliacionGestionList() {
        return rcoConciliacionGestionList;
    }

    public void setRcoConciliacionGestionList(List<RcoConciliacionGestion> rcoConciliacionGestionList) {
        this.rcoConciliacionGestionList = rcoConciliacionGestionList;
    }

    public CntPrestadorSede getCntPresadoresSedesId() {
        return cntPresadoresSedesId;
    }

    public void setCntPresadoresSedesId(CntPrestadorSede cntPresadoresSedesId) {
        this.cntPresadoresSedesId = cntPresadoresSedesId;
    }

    public List<RcoConciliacionAdjunto> getRcoConciliacionAdjuntosList() {
        return rcoConciliacionAdjuntosList;
    }

    public void setRcoConciliacionAdjuntosList(List<RcoConciliacionAdjunto> rcoConciliacionAdjuntosList) {
        this.rcoConciliacionAdjuntosList = rcoConciliacionAdjuntosList;
    }
    // metodos
    public String getEstadoStr(){
        String nombreEstado = "";
        if(estado != null){
            switch(estado){
                case PENDIENTE_SOPORTE:
                    nombreEstado = "Pendiente Soporte";
                    break;
                case NOTIFICIACION_PRESTADOR:
                    nombreEstado = "Notificación Prestador";
                    break;
                case EN_CONCILIACION:
                    nombreEstado = "En conciliación";
                    break;
                case FINALIZADO:
                    nombreEstado = "Finalizado";
                    break;
                default:
                    break;
            }   
        }
        return nombreEstado;
    }
    
    @Override
    public String toString() {
        return "RcoConciliacion{" + "id=" + id + ", nombre=" + nombre + ", descripcion=" + descripcion + ", estado=" + estado + ", numeroContrato=" + numeroContrato + ", cntContratoId=" + cntContratoId + ", fechaInicio=" + fechaInicio + ", fechaFin=" + fechaFin + ", correoEnvio=" + correoEnvio + ", cantidadItems=" + cantidadItems + ", cantidadItemsRecobrados=" + cantidadItemsRecobrados + '}';
    }
    
}
