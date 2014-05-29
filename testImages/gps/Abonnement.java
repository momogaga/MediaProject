/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modeles.utilisateur;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Bastien
 */
@Entity
public class Abonnement implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    protected String nom;
    protected double prix;

    @Temporal(TemporalType.DATE)
    @Column(name = "DATE_FIN")
    protected Date dateFin;
    @Temporal(TemporalType.DATE)
    @Column(name = "DATE_DEBUT")
    private Date dateDebut;

    public Abonnement() {
    }

    public Abonnement(String nom, double prix) {
        this.nom = nom;
        this.prix = prix;
        this.dateDebut = getCurrentDate();
        this.dateFin = calculeDateDeFin();
    }

    public Date getCurrentDate() {
        Calendar cal = Calendar.getInstance();
        return cal.getTime();
    }

    public Date getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(Date dateDebut) {
        this.dateDebut = dateDebut;
    }

    public final Date calculeDateDeFin() {
        Calendar cal = Calendar.getInstance();
        switch (this.nom) {
            case "Week-end":
                cal.add(Calendar.DAY_OF_MONTH, 2);
                return cal.getTime();
            case "Semaine":
                cal.add(Calendar.DAY_OF_MONTH, 7);
                return cal.getTime();
            case "Mois":
                cal.add(Calendar.DAY_OF_MONTH, 30);
                return cal.getTime();
            case "An":
                cal.add(Calendar.DAY_OF_MONTH, 365);
                return cal.getTime();
            case "Vie":
                return null;
            default:
                return null;
        }
    }

    public String getNom() {
        return nom;
    }

    public double getPrix() {
        return prix;
    }

    public Date getDateFin() {
        return dateFin;
    }
}
