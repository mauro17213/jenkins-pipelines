/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.saviasaludeps.savia.ejb.entidades;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author stive
 */
@Entity
@Table(name = "int_cargos")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "IntCargos.findAll", query = "SELECT i FROM IntCargos i"),
    @NamedQuery(name = "IntCargos.findById", query = "SELECT i FROM IntCargos i WHERE i.id = :id"),
    @NamedQuery(name = "IntCargos.findByMaeCargo", query = "SELECT i FROM IntCargos i WHERE i.maeCargo = :maeCargo")})
public class IntCargos implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "mae_cargo")
    private int maeCargo;

    public IntCargos() {
    }

    public IntCargos(Integer id) {
        this.id = id;
    }

    public IntCargos(Integer id, int maeCargo) {
        this.id = id;
        this.maeCargo = maeCargo;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getMaeCargo() {
        return maeCargo;
    }

    public void setMaeCargo(int maeCargo) {
        this.maeCargo = maeCargo;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof IntCargos)) {
            return false;
        }
        IntCargos other = (IntCargos) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.saviasaludeps.savia.ejb.entidades.IntCargos[ id=" + id + " ]";
    }
    
}
