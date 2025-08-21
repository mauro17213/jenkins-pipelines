package com.saviasaludeps.savia.ejb.servicios.recobro;

import com.saviasaludeps.savia.dominio.contratacion.CntPrestadorSede;
import com.saviasaludeps.savia.dominio.cuentamedica.conciliacion.CmDetalle;
import com.saviasaludeps.savia.dominio.especial.PePrograma;
import com.saviasaludeps.savia.dominio.generico.ParamConsulta;
import com.saviasaludeps.savia.dominio.recobro.RcoFactura;
import com.saviasaludeps.savia.dominio.recobro.RcoFacturaDetalle;
import com.saviasaludeps.savia.dominio.recobro.RcoGrupo;
import com.saviasaludeps.savia.ejb.entidades.CmDetalles;
import com.saviasaludeps.savia.ejb.entidades.CntPrestadorSedes;
import com.saviasaludeps.savia.ejb.entidades.PeProgramas;
import com.saviasaludeps.savia.ejb.entidades.RcoFacturaDetalles;
import com.saviasaludeps.savia.ejb.entidades.RcoFacturas;
import com.saviasaludeps.savia.ejb.entidades.RcoGrupos;
import com.saviasaludeps.savia.ejb.utilidades.GenericoServicio;
import com.saviasaludeps.savia.negocio.recobro.RcoFacturaDetalleRemoto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.hibernate.Session;

/**
 *
 * @author Stiven Giraldo
 */
@Stateless
@Remote(RcoFacturaDetalleRemoto.class)
public class RcoFacturaDetalleServicio extends GenericoServicio implements RcoFacturaDetalleRemoto {

    @Override
    public int consultarCantidadLista(ParamConsulta paramConsulta) throws Exception {

        int cant = 0;
        try {
            String strQuery = "SELECT COUNT(m) FROM RcoFacturaDetalles m WHERE 1 = 1 ";

            if (paramConsulta.getParametroConsulta1() != null) {
                strQuery += "AND m.rcoFacturasId.id = " + paramConsulta.getParametroConsulta1() + " ";
            }

            if (paramConsulta.getFiltros() != null) {
                for (Map.Entry e : paramConsulta.getFiltros().entrySet()) {
                    switch ((String) e.getKey()) {
                        case "id":
                            strQuery += "AND m.cmDetalleId.id = " + (String) e.getValue() + " ";
                            break;
                        case "aplicaRecobro":
                            strQuery += "AND m.aplicaRecobro = " + Boolean.valueOf(e.getValue().toString()) + " ";
                            break;
                        case "modalidad":
                            strQuery += "AND m.modalidad = " + (String) e.getValue() + " ";
                            break;
                        case "observacionDetalle":
                            strQuery += "AND m.observacionDetalle = " + (String) e.getValue() + " ";
                            break;
                        case "causalRecobro":
                            strQuery += "AND m.causalRecobro = " + Boolean.valueOf(e.getValue().toString()) + " ";
                            break;
                        case "tipoServicio":
                            strQuery += "AND m.tipoServicio = " + e.getValue() + " ";
                            break;
                        case "maeTipoDocumentoCodigo":
                            strQuery += "AND m.maeTipoDocumentoCodigo = " + (String) e.getValue() + " ";
                            break;
                        case "documento":
                            strQuery += "AND m.documento = " + (String) e.getValue() + " ";
                            break;
                        case "nombreCompleto":
                            strQuery += "AND m.nombreCompleto LIKE '%" + (String) e.getValue() + "%' ";
                            break;
                        case "maServicioCodigo":
                            strQuery += "AND m.maServicioCodigo = " + (String) e.getValue() + " ";
                            break;
                        case "maServicioValor":
                            strQuery += "AND m.maServicioValor LIKE '%" + (String) e.getValue() + "%' ";
                            break;
                        case "observacion":
                            strQuery += "AND m.observacion LIKE '%" + (String) e.getValue() + "%' ";
                            break;
                        case "conceptoContable":
                            strQuery += "AND m.conceptoContable LIKE '%" + (String) e.getValue() + "%' ";
                            break;
                        case "maDiagnostico":
                            strQuery += "AND m.maDiagnostico LIKE '%" + (String) e.getValue() + "%' ";
                            break;
                    }
                }
            }
            cant = (int) (long) getEntityManager().createQuery(strQuery)
                    .getSingleResult();
        } catch (NoResultException e) {
            cant = 0;
        } catch (Exception e) {
            Exception(CONSULTAR_TODOS, e);
        } finally {
            cerrarEntityManager();
        }
        return cant;
    }

    @Override
    public List<RcoFacturaDetalle> consultarLista(ParamConsulta paramConsulta) throws Exception {
        List<RcoFacturaDetalle> listResult = new ArrayList();
        try {
            String strQuery = "SELECT m FROM RcoFacturaDetalles m WHERE 1 = 1 ";

            if (paramConsulta.getParametroConsulta1() != null) {
                strQuery += "AND m.rcoFacturasId.id = " + paramConsulta.getParametroConsulta1() + " ";
            }

            if (paramConsulta.getFiltros() != null) {
                for (Map.Entry e : paramConsulta.getFiltros().entrySet()) {
                    switch ((String) e.getKey()) {
                        case "cmDetalleId.id":
                            strQuery += "AND m.cmDetalleId.id = " + (String) e.getValue() + " ";
                            break;
                        case "aplicaRecobro":
                            strQuery += "AND m.aplicaRecobro = " + Boolean.valueOf(e.getValue().toString()) + " ";
                            break;
                        case "modalidad":
                            strQuery += "AND m.modalidad = " + (String) e.getValue() + " ";
                            break;
                        case "observacionDetalle":
                            strQuery += "AND m.observacionDetalle = " + (String) e.getValue() + " ";
                            break;
                        case "causalRecobro":
                            strQuery += "AND m.causalRecobro = " + Boolean.valueOf(e.getValue().toString()) + " ";
                            break;
                        case "tipoServicio":
                            strQuery += "AND m.tipoServicio = " + e.getValue() + " ";
                            break;
                        case "maeTipoDocumentoCodigo":
                            strQuery += "AND m.maeTipoDocumentoCodigo = " + (String) e.getValue() + " ";
                            break;
                        case "documento":
                            strQuery += "AND m.documento = " + (String) e.getValue() + " ";
                            break;
                        case "nombreCompleto":
                            strQuery += "AND m.nombreCompleto LIKE '%" + (String) e.getValue() + "%' ";
                            break;
                        case "maServicioCodigo":
                            strQuery += "AND m.maServicioCodigo = " + (String) e.getValue() + " ";
                            break;
                        case "maServicioValor":
                            strQuery += "AND m.maServicioValor LIKE '%" + (String) e.getValue() + "%' ";
                            break;
                        case "observacion":
                            strQuery += "AND m.observacion LIKE '%" + (String) e.getValue() + "%' ";
                            break;
                        case "conceptoContable":
                            strQuery += "AND m.conceptoContable LIKE '%" + (String) e.getValue() + "%' ";
                            break;
                        case "maDiagnostico":
                            strQuery += "AND m.maDiagnostico LIKE '%" + (String) e.getValue() + "%' ";
                            break;
                    }
                }
            }
            strQuery += " ORDER BY ";
            if (paramConsulta.getOrden() != null) {
                strQuery += "m." + paramConsulta.getOrden() + " "
                        + (paramConsulta.isAscendente() ? "ASC" : "DESC");
            } else {
                strQuery += "m.id ASC ";
            }
            List<RcoFacturaDetalles> list = getEntityManager().createQuery(strQuery)
                    .setFirstResult(paramConsulta.getPrimerRegistro())
                    .setMaxResults(paramConsulta.getRegistrosPagina())
                    .getResultList();
            for (RcoFacturaDetalles per : list) {
                listResult.add(castEntidadNegocio(per));
            }
        } catch (NoResultException e) {
            listResult = new ArrayList();
        } catch (Exception e) {
            Exception(CONSULTAR_TODOS, e);
        } finally {
            cerrarEntityManager();
        }
        return listResult;

    }

    @Override
    public RcoFacturaDetalle consultar(int id) throws Exception {
        RcoFacturaDetalle objRes = null;
        try {
            objRes = castEntidadNegocio((RcoFacturaDetalles) getEntityManager().find(RcoFacturaDetalles.class, id));
        } catch (NoResultException e) {
            objRes = null;
        } catch (Exception e) {
            Exception(CONSULTAR, e);
        } finally {
            cerrarEntityManager();
        }
        return objRes;

    }

    @Override
    public int insertar(RcoFacturaDetalle obj) throws Exception {
        int _id = 0;
        try {
            _id = (int) getEntityManager().merge(castNegocioEntidad(obj)).getId();
            obj.setId(_id);
        } catch (NoResultException e) {
            _id = 0;
        } catch (Exception e) {
            Exception(INSERTAR, e, "La combinación nombre, tipo, valor ya existe.");
        } finally {
            cerrarEntityManager();
        }
        return _id;
    }

    @Override
    public void actualizar(RcoFacturaDetalle obj) throws Exception {
        try {
            Session session = getEntityManager().unwrap(Session.class);
            String strQuery = "UPDATE RcoFacturaDetalles a SET ";
            if (obj.getPeProgramaId().getId() != null) {
                strQuery += "a.peProgramaId.id = :peProgramaId ,";
            }

            strQuery += "a.cntPrestadoresSedesId.id = :cntPrestadoresSedesId, ";
            strQuery += "a.rcoGruposId.id = :rcoGruposId, ";
            strQuery += "a.modalidad = :modalidad, ";
            strQuery += "a.maeEstadoId = :maeEstadoId, ";
            strQuery += "a.maeEstadoCodigo = :maeEstadoCodigo, ";
            strQuery += "a.maeEstadoValor = :maeEstadoValor, ";
            strQuery += "a.observacionDetalle = :observacionDetalle, ";
            strQuery += "a.numeroContrato = :numeroContrato, ";
            strQuery += "a.aplicaRecobro = :aplicaRecobro, ";
            strQuery += "a.causalRecobro = :causalRecobro, ";
            strQuery += "a.maServicioHabilitadoId = :maServicioHabilitadoId, ";
            strQuery += "a.valorTotalRecobro = :valorTotalRecobro, ";
            strQuery += "a.valorRestanteRecobro = :valorRestanteRecobro, ";
            strQuery += "a.usuarioModifica = :usuarioModifica, ";
            strQuery += "a.terminalModifica = :terminalModifica, ";
            strQuery += "a.fechaHoraModifica = :fechaHoraModifica ";
            strQuery += "WHERE a.id = :id";

            Query query = session.createQuery(strQuery);

            if (obj.getPeProgramaId().getId() != null) {
                query.setParameter("peProgramaId", obj.getPeProgramaId().getId());
            }

            query.setParameter("cntPrestadoresSedesId", obj.getCntPrestadoresSedesId().getId());
            query.setParameter("rcoGruposId", obj.getRcoGruposId().getId());
            query.setParameter("modalidad", obj.getModalidad());
            query.setParameter("maeEstadoId", obj.getMaeEstadoId());
            query.setParameter("maeEstadoCodigo", obj.getMaeEstadoCodigo());
            query.setParameter("maeEstadoValor", obj.getMaeEstadoValor());
            query.setParameter("observacionDetalle", obj.getObservacionDetalle());
            query.setParameter("numeroContrato", obj.getNumeroContrato());
            query.setParameter("aplicaRecobro", obj.isAplicaRecobro());
            query.setParameter("causalRecobro", obj.isCausalRecobro());
            query.setParameter("maServicioHabilitadoId", obj.getMaServicioHabilitadoId());
            query.setParameter("valorTotalRecobro", obj.getValorTotalRecobro());
            query.setParameter("valorRestanteRecobro", obj.getValorRestanteRecobro());
            query.setParameter("usuarioModifica", obj.getUsuarioModifica());
            query.setParameter("terminalModifica", obj.getTerminalModifica());
            query.setParameter("fechaHoraModifica", obj.getFechaHoraModifica());
            query.setParameter("id", obj.getId());

            query.executeUpdate();
        } catch (NoResultException e) {
            Exception(ACTUALIZAR, e);
        } catch (Exception e) {
            Exception(ACTUALIZAR, e);
        } finally {
            cerrarEntityManager();
        }
    }

    @Override
    public void actualizarRecobroSiAplica(RcoFacturaDetalle obj) throws java.lang.Exception {
        try {
            String strQuery = "UPDATE RcoFacturaDetalles a SET ";

            if (obj.getPeProgramaId().getId() != null) {
                strQuery += "a.peProgramaId.id = :peProgramaId ,";
            }
            strQuery += "a.cntPrestadoresSedesId.id = :cntPrestadoresSedesId, ";
            strQuery += "a.rcoGruposId.id = :rcoGruposId, ";
            strQuery += "a.modalidad = :modalidad, ";
            strQuery += "a.maeEstadoId = :maeEstadoId, ";
            strQuery += "a.maeEstadoCodigo = :maeEstadoCodigo, ";
            strQuery += "a.maeEstadoValor = :maeEstadoValor, ";
            strQuery += "a.observacionDetalle = :observacionDetalle, ";
            strQuery += "a.numeroContrato = :numeroContrato, ";
            strQuery += "a.aplicaRecobro = :aplicaRecobro, ";
            strQuery += "a.causalRecobro = :causalRecobro, ";
            strQuery += "a.maServicioHabilitadoId = :maServicioHabilitadoId, ";
            strQuery += "a.valorTotalRecobro = :valorTotalRecobro, ";
            strQuery += "a.valorRestanteRecobro = :valorRestanteRecobro, ";
            strQuery += "a.usuarioModifica = :usuarioModifica, ";
            strQuery += "a.terminalModifica = :terminalModifica, ";
            strQuery += "a.fechaHoraModifica = :fechaHoraModifica ";
            strQuery += "WHERE a.id = :id";

            Query query = getEntityManager().createQuery(strQuery);

            if (obj.getPeProgramaId().getId() != null) {
                query.setParameter("peProgramaId", obj.getPeProgramaId().getId());
            }

            query.setParameter("cntPrestadoresSedesId", obj.getCntPrestadoresSedesId().getId());
            query.setParameter("rcoGruposId", obj.getRcoGruposId().getId());
            query.setParameter("modalidad", obj.getModalidad());
            query.setParameter("maeEstadoId", obj.getMaeEstadoId());
            query.setParameter("maeEstadoCodigo", obj.getMaeEstadoCodigo());
            query.setParameter("maeEstadoValor", obj.getMaeEstadoValor());
            query.setParameter("observacionDetalle", obj.getObservacionDetalle());
            query.setParameter("numeroContrato", obj.getNumeroContrato());
            query.setParameter("aplicaRecobro", obj.isAplicaRecobro());
            query.setParameter("causalRecobro", obj.isCausalRecobro());
            query.setParameter("maServicioHabilitadoId", obj.getMaServicioHabilitadoId());
            query.setParameter("valorTotalRecobro", obj.getValorTotalRecobro());
            query.setParameter("valorRestanteRecobro", obj.getValorRestanteRecobro());
            query.setParameter("usuarioModifica", obj.getUsuarioModifica());
            query.setParameter("terminalModifica", obj.getTerminalModifica());
            query.setParameter("fechaHoraModifica", obj.getFechaHoraModifica());
            query.setParameter("id", obj.getId());

            query.executeUpdate();

        } catch (NoResultException e) {

        } catch (Exception e) {
            Exception(ACTUALIZAR, e);
        } finally {
            cerrarEntityManager();
        }
    }

    @Override
    public void actualizarRecobroNoAplica(RcoFacturaDetalle obj) throws java.lang.Exception {
        try {

            String strQueryNoRecobro = "UPDATE RcoFacturaDetalles a SET ";
            strQueryNoRecobro += "a.modalidad = :modalidad, ";
            strQueryNoRecobro += "a.maeEstadoId = :maeEstadoId, ";
            strQueryNoRecobro += "a.maeEstadoCodigo = :maeEstadoCodigo, ";
            strQueryNoRecobro += "a.maeEstadoValor = :maeEstadoValor, ";
            strQueryNoRecobro += "a.observacionDetalle = :observacionDetalle, ";
            strQueryNoRecobro += "a.numeroContrato = :numeroContrato, ";
            strQueryNoRecobro += "a.aplicaRecobro = :aplicaRecobro, ";
            strQueryNoRecobro += "a.causalRecobro = :causalRecobro, ";
            strQueryNoRecobro += "a.usuarioModifica = :usuarioModifica, ";
            strQueryNoRecobro += "a.terminalModifica = :terminalModifica, ";
            strQueryNoRecobro += "a.fechaHoraModifica = :fechaHoraModifica ";
            strQueryNoRecobro += "WHERE a.id = :id";

            Query query = getEntityManager().createQuery(strQueryNoRecobro);

            query.setParameter("modalidad", obj.getModalidad());
            query.setParameter("maeEstadoId", obj.getMaeEstadoId());
            query.setParameter("maeEstadoCodigo", obj.getMaeEstadoCodigo());
            query.setParameter("maeEstadoValor", obj.getMaeEstadoValor());
            query.setParameter("observacionDetalle", obj.getObservacionDetalle());
            query.setParameter("numeroContrato", obj.getNumeroContrato());
            query.setParameter("aplicaRecobro", obj.isAplicaRecobro());
            query.setParameter("causalRecobro", obj.isCausalRecobro());
            query.setParameter("usuarioModifica", obj.getUsuarioModifica());
            query.setParameter("terminalModifica", obj.getTerminalModifica());
            query.setParameter("fechaHoraModifica", obj.getFechaHoraModifica());
            query.setParameter("id", obj.getId());

            query.executeUpdate();

        } catch (NoResultException e) {

        } catch (Exception e) {
            Exception(ACTUALIZAR, e);
        } finally {
            cerrarEntityManager();
        }
    }

    @Override
    public RcoFacturaDetalle eliminar(int id) throws java.lang.Exception {
        RcoFacturaDetalle obj = null;
        try {
            RcoFacturaDetalles ent = getEntityManager().find(RcoFacturaDetalles.class, id);
            if (ent != null) {
                obj = castEntidadNegocio(ent);
                getEntityManager().remove(ent);
            }
        } catch (NoResultException e) {
            obj = null;
        } catch (Exception e) {
            Exception(ELIMINAR, e);
        } finally {
            cerrarEntityManager();
        }
        return obj;
    }

    @Override
    public int consultarCantidadListaConciliacion(ParamConsulta paramConsulta) throws Exception {
        int cant = 0;
        try {
            String strTitulo = "SELECT COUNT(u) FROM RcoFacturaDetalles u ";
            StringBuilder strQuery = new StringBuilder("WHERE u.id > 0 ");
            StringBuilder sql = new StringBuilder();
            if (paramConsulta.getFiltros() != null) {
                for (Map.Entry e : paramConsulta.getFiltros().entrySet()) {
                    switch ((String) e.getKey()) {
                        case "id":
                            strQuery.append("AND u.id = ").append(e.getValue()).append(" ");
                            break;
                        case "cntPrestadoresSedesId.id":
                            strTitulo = agregarJoin("INNER JOIN CntPrestadorSedes cntp ON u.cntPrestadoresSedesId = cntp.id ", strTitulo);
                            strQuery.append("AND cntp.id = ").append(e.getValue()).append(" ");
                            break;
                        case "maeEstadoCodigo":
                            strQuery.append("AND u.maeEstadoCodigo = '").append(e.getValue()).append("' ");
                            break;
                    }
                }
            }
            sql.append(strTitulo).append(strQuery);
            cant = (int) (long) getEntityManager().createQuery(sql.toString())
                    .getSingleResult();
        } catch (NoResultException e) {
            cant = 0;
        } catch (Exception e) {
            Exception(CONSULTAR_TODOS, e);
        } finally {
            cerrarEntityManager();
        }
        return cant;
    }

    @Override
    public List<RcoFacturaDetalle> consultarListaConciliacion(ParamConsulta paramConsulta) throws Exception {
        List<RcoFacturaDetalle> listResult = new ArrayList();
        try {
            String strTitulo = "SELECT u FROM RcoFacturaDetalles u ";
            StringBuilder strQuery = new StringBuilder("WHERE u.id > 0 ");
            StringBuilder sql = new StringBuilder();
            if (paramConsulta.getFiltros() != null) {
                for (Map.Entry e : paramConsulta.getFiltros().entrySet()) {
                    switch ((String) e.getKey()) {
                        case "id":
                            strQuery.append("AND u.id = ").append(e.getValue()).append(" ");
                            break;
                        case "cntPrestadoresSedesId.id":
                            strTitulo = agregarJoin("INNER JOIN CntPrestadorSedes cntp ON u.cntPrestadoresSedesId = cntp.id ", strTitulo);
                            strQuery.append("AND cntp.id = ").append(e.getValue()).append(" ");
                            break;
                        case "maeEstadoCodigo":
                            strQuery.append("AND u.maeEstadoCodigo = '").append(e.getValue()).append("' ");
                            break;
                    }
                }
            }
            sql.append(strTitulo).append(strQuery);
            sql.append("ORDER BY ");
            if (paramConsulta.getOrden() != null) {
                sql.append("u.").append(paramConsulta.getOrden()).append((paramConsulta.isAscendente() ? " ASC" : " DESC"));
            } else {
                sql.append("u.id DESC");
            }
            List<RcoFacturaDetalles> list = getEntityManager().createQuery(sql.toString())
                    .setFirstResult(paramConsulta.getPrimerRegistro())
                    .setMaxResults(paramConsulta.getRegistrosPagina())
                    .getResultList();
            for (RcoFacturaDetalles per : list) {
                listResult.add(castEntidadNegocio(per));
            }
        } catch (NoResultException e) {
            listResult = new ArrayList();
        } catch (Exception e) {
            Exception(CONSULTAR_TODOS, e);
        } finally {
            cerrarEntityManager();
        }
        return listResult;
    }

    @SuppressWarnings("UnusedAssignment")
    private String agregarJoin(String join, String sql) {
        if (sql.contains(join)) {
            return sql;
        } else {
            return sql += join;
        }
    }

    @Override
    public List<RcoFacturaDetalle> consultarFacturaDetallesPorPrestadoryCodigo(int prestadoresId, String codigo) throws Exception {
        List<RcoFacturaDetalle> obj = new ArrayList();
        try {
            String strQuery = "SELECT u "
                    + "FROM RcoFacturaDetalles u "
                    + "WHERE u.cntPrestadoresSedesId.id = " + prestadoresId + " "
                    + "AND u.maeEstadoCodigo = '" + codigo + "'";
            List<RcoFacturaDetalles> list = getEntityManager().createQuery(strQuery).getResultList();
            for (RcoFacturaDetalles detalle : list) {
                obj.add(castEntidadNegocio(detalle));
            }
        } catch (NoResultException e) {
            obj = new ArrayList();
        } catch (Exception e) {
            Exception(CONSULTAR_TODOS, e);
        } finally {
            cerrarEntityManager();
        }
        return obj;
    }

    private RcoFacturaDetalle castEntidadNegocio(RcoFacturaDetalles entidad) {
        RcoFacturaDetalle negocio = new RcoFacturaDetalle();
        negocio.setId(entidad.getId());
        if (entidad.getRcoFacturasId() != null) {
            RcoFactura factura = new RcoFactura();
            factura.setId(entidad.getRcoFacturasId().getId());
            factura.setNumeroFacturado(entidad.getRcoFacturasId().getNumeroFacturado());
            negocio.setRcoFacturasId(factura); //Numero de factura y el metodo espera prestador y codigo

        }
        negocio.setCmDetalleId(new CmDetalle(entidad.getCmDetalleId().getId()));
        if (entidad.getPeProgramaId() != null) {
            negocio.setPeProgramaId(new PePrograma(entidad.getPeProgramaId().getId()));
        }
        if (entidad.getCntPrestadoresSedesId() != null) {
            CntPrestadorSede PrestadorSede = new CntPrestadorSede(entidad.getCntPrestadoresSedesId().getId());
            PrestadorSede.setNombreSede(entidad.getCntPrestadoresSedesId().getNombre());
            negocio.setCntPrestadoresSedesId(PrestadorSede);
        }
        if (entidad.getRcoGruposId() != null) {
            negocio.setRcoGruposId(new RcoGrupo(entidad.getRcoGruposId().getId()));
        }
        negocio.setModalidad(entidad.getModalidad());
        negocio.setMaeEstadoId(entidad.getMaeEstadoId());
        negocio.setMaeEstadoCodigo(entidad.getMaeEstadoCodigo());
        negocio.setMaeEstadoValor(entidad.getMaeEstadoValor());
        negocio.setObservacionDetalle(entidad.getObservacionDetalle());
        negocio.setNumeroContrato(entidad.getNumeroContrato());
        negocio.setAplicaRecobro(entidad.getAplicaRecobro());
        negocio.setCausalRecobro(entidad.getCausalRecobro());
        negocio.setCmDetalleEstado(entidad.getCmDetalleEstado());
        negocio.setMaeTipoDocumentoId(entidad.getMaeTipoDocumentoId());
        negocio.setMaeTipoDocumentoCodigo(entidad.getMaeTipoDocumentoCodigo());
        negocio.setMaeTipoDocumentoValor(entidad.getMaeTipoDocumentoValor());
        negocio.setDocumento(entidad.getDocumento());
        negocio.setNombreCompleto(entidad.getNombreCompleto());
        negocio.setConsecutivoItem(entidad.getConsecutivoItem());
        negocio.setMaServicioHabilitadoId(entidad.getMaServicioHabilitadoId());
        negocio.setMaServicioCodigo(entidad.getMaServicioCodigo());
        negocio.setMaServicioValor(entidad.getMaServicioValor());
        negocio.setCantidad(entidad.getCantidad());
        negocio.setValorFacturado(entidad.getValorFacturado());
        negocio.setValorRestanteRecobro(entidad.getValorRestanteRecobro());
        negocio.setValorTotalRecobro(entidad.getValorTotalRecobro());
        negocio.setObservacion(entidad.getObservacion());
        negocio.setTipoServicio(entidad.getTipoServicio());
        negocio.setFechaHoraPrestacion(entidad.getFechaHoraPrestacion());
        negocio.setConceptoContable(entidad.getConceptoContable());
        negocio.setMaDiagnostico(entidad.getMaDiagnostico());
        negocio.setMaDiagnosticoId(entidad.getMaDiagnosticoId());
        negocio.setMaDiagnosticoCodigo(entidad.getMaDiagnosticoCodigo());
        negocio.setMaDiagnosticoValor(entidad.getMaDiagnosticoValor());
        negocio.setUsuarioCrea(entidad.getUsuarioCrea());
        negocio.setTerminalCrea(entidad.getTerminalCrea());
        negocio.setFechaHoraCrea(entidad.getFechaHoraCrea());
        negocio.setUsuarioModifica(entidad.getUsuarioModifica());
        negocio.setTerminalModifica(entidad.getTerminalModifica());
        negocio.setFechaHoraModifica(entidad.getFechaHoraModifica());
        return negocio;
    }

    private RcoFacturaDetalles castNegocioEntidad(RcoFacturaDetalle negocio) {
        RcoFacturaDetalles entidad = new RcoFacturaDetalles();
        entidad.setId(negocio.getId());
        entidad.setRcoFacturasId(new RcoFacturas(negocio.getRcoFacturasId().getId()));
        entidad.setCmDetalleId(new CmDetalles(negocio.getCmDetalleId().getId()));
        entidad.setPeProgramaId(new PeProgramas(negocio.getPeProgramaId().getId()));
        entidad.setRcoGruposId(new RcoGrupos(negocio.getRcoGruposId().getId()));
        entidad.setCntPrestadoresSedesId(new CntPrestadorSedes(negocio.getCntPrestadoresSedesId().getId()));
        entidad.setModalidad(negocio.getModalidad());
        entidad.setMaeEstadoId(negocio.getMaeEstadoId());
        entidad.setMaeEstadoCodigo(negocio.getMaeEstadoCodigo());
        entidad.setMaeEstadoValor(negocio.getMaeEstadoValor());
        entidad.setObservacionDetalle(negocio.getObservacionDetalle());
        entidad.setNumeroContrato(negocio.getNumeroContrato());
        entidad.setAplicaRecobro(negocio.isAplicaRecobro());
        entidad.setCausalRecobro(negocio.isCausalRecobro());
        entidad.setCmDetalleEstado(negocio.getCmDetalleEstado());
        entidad.setMaeTipoDocumentoId(negocio.getMaeTipoDocumentoId());
        entidad.setMaeTipoDocumentoCodigo(negocio.getMaeTipoDocumentoCodigo());
        entidad.setMaeTipoDocumentoValor(negocio.getMaeTipoDocumentoValor());
        entidad.setDocumento(negocio.getDocumento());
        entidad.setNombreCompleto(negocio.getNombreCompleto());
        entidad.setConsecutivoItem(negocio.getConsecutivoItem());
        entidad.setMaServicioHabilitadoId(negocio.getMaServicioHabilitadoId());
        entidad.setMaServicioCodigo(negocio.getMaServicioCodigo());
        entidad.setMaServicioValor(negocio.getMaServicioValor());
        entidad.setCantidad(negocio.getCantidad());
        entidad.setValorFacturado(negocio.getValorFacturado());
        entidad.setValorRestanteRecobro(negocio.getValorRestanteRecobro());
        entidad.setValorTotalRecobro(negocio.getValorTotalRecobro());
        entidad.setObservacion(negocio.getObservacion());
        entidad.setTipoServicio(negocio.getTipoServicio());
        entidad.setFechaHoraPrestacion(negocio.getFechaHoraPrestacion());
        entidad.setConceptoContable(negocio.getConceptoContable());
        entidad.setMaDiagnostico(negocio.getMaDiagnostico());
        entidad.setMaDiagnosticoId(negocio.getMaDiagnosticoId());
        entidad.setMaDiagnosticoCodigo(negocio.getMaDiagnosticoCodigo());
        entidad.setMaDiagnosticoValor(negocio.getMaDiagnosticoValor());
        entidad.setUsuarioCrea(negocio.getUsuarioCrea());
        entidad.setTerminalCrea(negocio.getTerminalCrea());
        entidad.setFechaHoraCrea(negocio.getFechaHoraCrea());
        entidad.setUsuarioModifica(negocio.getUsuarioModifica());
        entidad.setTerminalModifica(negocio.getTerminalModifica());
        entidad.setFechaHoraModifica(negocio.getFechaHoraModifica());
        return entidad;
    }

}
